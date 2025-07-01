#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
from __future__ import annotations

from collections import OrderedDict
from copy import deepcopy
from datetime import datetime
from typing import Optional, TYPE_CHECKING, Type, Union
from xml.etree import ElementTree as etree

import numpy as np
import pyvips
from dateutil.parser import isoparse as dateutil_isoparse
from pint import Quantity
from pyvips import Image as VIPSImage
from tifffile import TiffFile, TiffPageSeries, xml2dict

from pims.api.utils.models import ChannelReduction
from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.convertor import AbstractConvertor
from pims.formats.utils.engines.omexml import omexml_type
from pims.formats.utils.engines.tifffile import (
    TifffileChecker, TifffileParser, cached_tifffile,
    remove_tiff_comments
)
from pims.formats.utils.engines.vips import VipsReader
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.formats.utils.structures.planes import PlanesInfo
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils import UNIT_REGISTRY
from pims.utils.color import infer_channel_color
from pims.utils.dict import flatten
from pims.utils.dtypes import dtype_to_bits
from pims.utils.iterables import product
from pims.utils.types import parse_float, parse_int
from pims.utils.vips import bandjoin, bandreduction, fix_rgb_interpretation

if TYPE_CHECKING:
    from pims.files.file import Path


def clean_ome_dict(d: dict) -> dict:
    ignored = ('Settings', 'Ref', 'TiffData', 'BinData')

    def parse_ref(key, ref):
        if key == 'Channel':
            ids = [ref.split(':')[-1]]
        else:
            ids = ref.split(':')[1:]
        return ''.join([f"[{i}]" for i in ids])

    cleaned = dict()
    for k, v in d.items():
        if any(k.endswith(i) for i in ignored):
            continue

        v = deepcopy(v)
        if type(v) is dict:
            if 'ID' in v.keys():
                if k != 'Pixels':
                    id = parse_ref(k, v['ID'])
                    del v['ID']
                    v = {id: v}
                    d[k] = v
                else:
                    del v['ID']
            cleaned[k] = clean_ome_dict(v)
        elif type(v) is list:
            new_v = dict()
            for idx, item in enumerate(v):
                keys = item.keys()
                if 'ID' in keys and all(i not in keys for i in ignored):
                    id = parse_ref(k, item['ID'])
                    del item['ID']
                    new_v[id] = item
                else:
                    new_v[f'[{idx}]'] = item
            if len(new_v) == 0:
                new_v = v
            cleaned[k] = clean_ome_dict(new_v)
        else:
            cleaned[k] = v

    return cleaned


omexml_dimension = {
    'X': 'width',
    'Y': 'height',
    'C': 'n_concrete_channels',
    'Z': 'depth',
    'T': 'duration',
    'S': 'n_samples'
}


def cached_main_tifffile_series(
    format: Union[AbstractFormat, CachedDataPath]
) -> TiffPageSeries:
    tf = cached_tifffile(format)

    def get_baseseries(tf: TiffFile) -> TiffPageSeries:
        idx = np.argmax([np.prod(s.shape) for s in tf.series])
        return tf.series[idx]

    return format.get_cached('_tf_baseseries', get_baseseries, tf)


class OmeTiffChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if super().match(pathlike):
                tf = cls.get_tifffile(pathlike)
                if not tf.is_ome:
                    return False
                
                if len(tf.series) >= 1:
                    baseline = cached_main_tifffile_series(pathlike)
                    if baseline and not baseline.is_pyramidal\
                            and len(baseline.levels) == 1:
                        return True
            return False
        except RuntimeError:
            return False


class OmeTiffParser(TifffileParser):
    @property
    def omexml_description(self):
        tf = cached_tifffile(self.format)
        return tf.pages[0].description

    @cached_property
    def omexml(self):
        omexml = self.omexml_description
        try:
            parsed = etree.fromstring(omexml)
        except etree.ParseError:
            try:
                omexml = omexml.decode(errors='ignore').encode()
                parsed = etree.fromstring(omexml)
            except Exception:  # noqa
                return None
        return parsed

    @cached_property
    def main_series_(self):
        omexml = self.omexml
        max_size = 0
        main = None
        idx = 0
        for element in omexml:
            if element.tag.endswith('BinaryOnly'):
                break
            if not element.tag.endswith('Image'):
                continue

            for im_element in element:
                if not im_element.tag.endswith('Pixels'):
                    continue
                attr = im_element.attrib
                w = int(attr.get('SizeX'))
                h = int(attr.get('SizeY'))
                size = w * h
                if size > max_size:
                    max_size = size
                    main = element
            idx += 1
        return main, idx

    @property
    def main_root(self):
        root, _ = self.main_series_
        return root

    @property
    def main_idx(self):
        _, idx = self.main_series_
        return idx

    def parse_main_metadata(self) -> ImageMetadata:
        imd = ImageMetadata()

        omexml = self.main_root
        if omexml is None:
            raise ValueError('Impossible to find main OME-TIF image.')

        for element in omexml:
            if not element.tag.endswith('Pixels'):
                continue
            attr = element.attrib

            imd.pixel_type = np.dtype(omexml_type[attr.get('Type').lower()])
            imd.significant_bits = dtype_to_bits(imd.pixel_type)

            axes = ''.join(reversed(attr['DimensionOrder']))
            shape = [int(attr['Size' + ax]) for ax in axes]
            spp = 1  # samples per pixel
            cc_idx = 0  # concrete channel idx

            for data in element:
                if not data.tag.endswith('Channel'):
                    continue

                attr = data.attrib
                if cc_idx == 0:
                    spp = int(attr.get('SamplesPerPixel', spp))
                    if spp > 1:
                        # correct channel dimension for spp
                        shape = [
                            shape[i] // spp if ax == 'C' else shape[i]
                            for i, ax in enumerate(axes)
                        ]
                elif int(attr.get('SamplesPerPixel', 1)) != spp:
                    raise ValueError(
                        'OME-TIF: differing SamplesPerPixel not supported'
                    )

                if cc_idx >= shape[axes.index('C')]:
                    # Happens when <Channel> is repeated for spp > 1, while
                    #  information is already extracted.
                    break

                if spp == 3:
                    # If RGB channel, Color attribute is ignored (Icy behavior)
                    colors = [
                        infer_channel_color(None, i, 3) for i in range(spp)
                    ]
                else:
                    colors = [infer_channel_color(
                        parse_int(attr.get('Color')),
                        cc_idx,
                        shape[axes.index('C')]
                    )] * spp

                names = [attr.get('Name')] * spp
                if names[0] is None:
                    if spp == 1:
                        if 2 <= shape[axes.index('C')] <= 3:
                            names = ['RGB'[cc_idx]]
                    elif spp == 3:
                        names = ['R', 'G', 'B']

                emission = parse_float(attr.get('EmissionWavelength'))
                excitation = parse_float(attr.get('ExcitationWavelength'))

                for s in range(spp):
                    imd.set_channel(ImageChannel(
                        index=cc_idx * spp + s,
                        suggested_name=names[s], color=colors[s],
                        emission_wavelength=emission,
                        excitation_wavelength=excitation
                    ))

                cc_idx += 1

            imd.width = shape[axes.index('X')]
            imd.height = shape[axes.index('Y')]
            imd.depth = shape[axes.index('Z')] if 'Z' in axes else 1
            imd.duration = shape[axes.index('T')] if 'T' in axes else 1
            imd.n_concrete_channels = shape[axes.index('C')] if 'C' in axes else 1
            imd.n_samples = spp

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()

        omexml = self.main_root
        if omexml is None:
            raise ValueError('Impossible to find main OME-TIF image.')

        attr = omexml.attrib
        imd.description = attr.get('Name')

        instrument_ref = None
        for element in omexml:
            if element.tag.endswith('AcquisitionDate'):
                imd.acquisition_datetime = self.parse_ome_acquisition_date(
                    element.text
                )
                continue

            if element.tag.endswith('Description'):
                imd.description = element.text
                continue

            if element.tag.endswith('InstrumentRef'):
                attr = element.attrib
                instrument_ref = attr.get('ID')
                continue
            
            if not element.tag.endswith('Pixels'):
                continue
            attr = element.attrib

            imd.physical_size_x = self.parse_ome_physical_size(
                parse_float(attr.get('PhysicalSizeX')), 
                attr.get('PhysicalSizeXUnit')
            )
            imd.physical_size_y = self.parse_ome_physical_size(
                parse_float(attr.get('PhysicalSizeY')),
                attr.get('PhysicalSizeYUnit')
            )
            imd.physical_size_z = self.parse_ome_physical_size(
                parse_float(attr.get('PhysicalSizeZ')),
                attr.get('PhysicalSizeZUnit')
            )
            imd.frame_rate = self.parse_frame_rate(
                parse_float(attr.get('TimeIncrement')),
                attr.get('TimeIncrementUnit')
            )

        if instrument_ref is not None:
            root = self.omexml
            for element in root:
                if element.tag.endswith('BinaryOnly'):
                    break
                if not element.tag.endswith('Instrument'):
                    continue

                attr = element.attrib
                id = attr.get('ID')
                if instrument_ref != id:
                    continue

                for data in element:
                    if data.tag.endswith('Microscope'):
                        attr = data.attrib
                        imd.microscope.model = attr.get('Model')
                        continue

                    if data.tag.endswith('Objective'):
                        attr = data.attrib
                        imd.objective.nominal_magnification = \
                            parse_float(attr.get('NominalMagnification'))
                        imd.objective.calibrated_magnification = \
                            parse_float(attr.get('CalibratedMagnification'))
                        continue

        # Associated
        root = self.omexml
        idx = 0
        for element in root:
            if element.tag.endswith('BinaryOnly'):
                break
            if not element.tag.endswith('Image'):
                continue

            attr = element.attrib
            if idx != self.main_idx:
                name = attr.get('Name')
                if name is None:
                    continue
                if name.lower() in ['thumbnail', 'thumb']:
                    associated = imd.associated_thumb
                elif name.lower() == 'label':
                    associated = imd.associated_label
                elif name.lower() == 'macro':
                    associated = imd.associated_macro
                else:
                    continue

                for im_element in element:
                    if not im_element.tag.endswith('Pixels'):
                        continue
                    attr = im_element.attrib
                    associated.width = attr.get('SizeX')
                    associated.height = attr.get('SizeY')
                    associated.n_channels = attr.get('SizeC', 1)
                    break
            idx += 1

        imd.is_complete = True
        return imd

    @staticmethod
    def parse_frame_rate(
        time_increment: Optional[float], unit: Optional[str]
    ) -> Optional[Quantity]:
        if unit is None:
            unit = 's'
        if time_increment is None or time_increment <= 0:
            return None
        return 1 / (time_increment * UNIT_REGISTRY(unit))

    @staticmethod
    def parse_ome_physical_size(
        physical_size: Optional[float], unit: Optional[str]
    ) -> Optional[Quantity]:
        if unit is None:
            unit = 'µm'
        if physical_size is None or physical_size <= 0 \
                or unit in ['pixel', 'reference frame']:
            return None
        return physical_size * UNIT_REGISTRY(unit)

    @staticmethod
    def parse_ome_acquisition_date(date: Optional[str]) -> Optional[datetime]:
        if date is None:
            return None
        try:
            return dateutil_isoparse(date)
        except ValueError:
            return None

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()
        xml = self.omexml_description
        ome = flatten(clean_ome_dict(xml2dict(xml)))
        for full_key, value in ome.items():
            if value is not None:
                store.set(full_key, value)

        return store

    def parse_pyramid(self) -> Pyramid:
        pyramid = Pyramid()
        width = self.format.main_imd.width
        height = self.format.main_imd.height

        pyramid.insert_tier(width, height, (width, height), subifd=None)
        return pyramid

    def parse_planes(self) -> PlanesInfo:
        imd = self.format.main_imd
        pi = PlanesInfo(
            imd.n_concrete_channels, imd.depth, imd.duration,
            ['page_index'], [np.int64]
        )

        omexml = self.main_root
        if omexml is None:
            raise ValueError('Impossible to find main OME-TIF image.')

        for element in omexml:
            if not element.tag.endswith('Pixels'):
                continue
            attr = element.attrib
            axes = ''.join(reversed(attr['DimensionOrder']))
            positions = [axes.index(ax) for ax in 'CZT']
            shape = [getattr(imd, omexml_dimension[ax]) for ax in axes]
            n_pages = product(shape[:-2])
            ifds = []

            for data in element:
                if not data.tag.endswith('TiffData'):
                    continue

                attr = data.attrib
                ifd = int(attr.get('IFD', 0))
                num = int(attr.get('NumPlanes', 1 if 'IFD' in attr else 0))
                num = int(attr.get('PlaneCount', num))
                idx = [int(attr.get('First' + ax, 0)) for ax in axes[:-2]]
                try:
                    idx = int(np.ravel_multi_index(idx, shape[:-2]))
                except ValueError:
                    # ImageJ produces invalid ome-xml when cropping
                    # print('OME series contains invalid TiffData index')
                    continue

                try:
                    size = num if num else n_pages
                    ifds.extend([None] * (size + idx - len(ifds)))
                    for i in range(size):
                        ifds[idx + i] = ifd + i
                except IndexError:
                    # print('OME series contains index out of range')
                    pass

            if len(ifds) == 0:
                # No TiffData tag. Suppose a <TiffData />.
                ifds = list(range(n_pages))

            for idx, plane_indexes in enumerate(np.ndindex(*shape[:-2])):
                ordered_idxs = [plane_indexes[pos] for pos in positions]
                pi.set(*ordered_idxs, page_index=ifds[idx])

        return pi


class OmeTiffReader(VipsReader):
    def _pages_to_read(self, channels, z, t):
        pages = OrderedDict()

        cc_idxs, s_idxs = self._concrete_channel_indexes(channels)
        page_idxs = self.format.planes_info.get(
            cc_idxs, z, t, 'page_index'
        )

        for page, s in zip(page_idxs, s_idxs):
            if page in pages:
                pages[page].append(s)
            else:
                pages[page] = [s]
        return pages

    def _read(self, c, z, t, vips_func, *args, **kwargs):
        bands = list()
        spp = self.format.main_imd.n_samples
        for page, samples in self._pages_to_read(c, z, t).items():
            im = vips_func(*args, page=page, **kwargs)
            if im.hasalpha():
                im = im.flatten()
            if im.bands != spp:
                # Happen when there is an internal tiff colormap
                # TODO: better handle internal colormaps
                im = bandreduction(im.bandsplit(), ChannelReduction.MAX)
            else:
                im = self._extract_channels(im, samples)
            bands.append(im)
        im = bandjoin(bands)
        if c == [0, 1, 2]:
            im = fix_rgb_interpretation(im)
        return im

    def read_thumb(self, out_width, out_height, precomputed=None, c=None, z=None, t=None):
        # TODO: precomputed ?
        # Thumbnail already uses shrink-on-load feature in default VipsReader
        # (i.e it loads the right pyramid level according the requested dimensions)
        return self._read(c, z, t, self.vips_thumbnail, out_width, out_height)

    def read_window(self, region, out_width, out_height, c=None, z=None, t=None):
        tier = self.format.pyramid.most_appropriate_tier(
            region, (out_width, out_height)
        )
        region = region.scale_to_tier(tier)
        subifd = tier.data.get('subifd')

        def read_func(path, region, page=None, subifd=None):  # noqa
            opts = dict(page=page)
            if subifd is not None:
                opts['subifd'] = subifd
            tiff_page = VIPSImage.tiffload(str(path), **opts)
            im = tiff_page.extract_area(
                region.left, region.top, region.width, region.height
            )
            return im

        return self._read(c, z, t, read_func, self.format.path, region, subifd=subifd)


class OmeTiffConvertor(AbstractConvertor):

    def convert(self, dest_path: Path) -> bool:
        # https://github.com/libvips/pyvips/issues/277#issuecomment-946913363

        n_pages = self.source.main_imd.n_planes
        vips_source = VIPSImage.new_from_file(str(self.source.path), n=n_pages)

        opts = dict()
        if n_pages > 1:
            opts['page_height'] = vips_source.get('page-height')

        result = vips_source.tiffsave(
            str(dest_path), pyramid=True, tile=True,
            tile_width=256, tile_height=256, bigtiff=True,
            properties=False, subifd=True,
            depth=pyvips.enums.ForeignDzDepth.ONETILE,
            compression=pyvips.enums.ForeignTiffCompression.LZW,
            region_shrink=pyvips.enums.RegionShrink.MEAN,
            **opts
        )
        ok = not bool(result)

        # Some cleaning. libvips sets description to all pages, while it is
        #  unnecessary after first page.
        if ok:
            try:
                remove_tiff_comments(dest_path, n_pages, except_pages=[0])
            except Exception:  # noqa
                pass
        return ok

    def conversion_format(self) -> Type[AbstractFormat]:
        return PyrOmeTiffFormat


class OmeTiffFormat(AbstractFormat):
    """
    OME-TIFF format.

    Known limitations:
    * Multi-file OME-TIFF are not supported.
    * Differing samples per pixel (in channels) are not supported.
    * Tiled planar TIFF configuration is not readable.
    * Modulo along axis is not supported.

    References:

    """
    checker_class = OmeTiffChecker
    parser_class = OmeTiffParser
    reader_class = OmeTiffReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = OmeTiffConvertor

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "OME-TIFF"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return self.main_imd.width * self.main_imd.height > 1024 * 1024

    @property
    def media_type(self):
        return "ome/ome-tiff"


# -----------------------------------------------------------------------------
# PYRAMIDAL OME TIF

class PyrOmeTiffChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if not super().match(pathlike):
                return False

            tf = cls.get_tifffile(pathlike)
            if not tf.is_ome:
                return False

            if len(tf.series) >= 1:
                baseline = cached_main_tifffile_series(pathlike)
                if baseline and baseline.is_pyramidal:
                    return baseline.levels[0].keyframe.is_tiled
                    # for level in baseline.levels:
                    #     if level.keyframe.is_tiled is False:
                    #         return False
                    # return True

            return False
        except RuntimeError:
            return False


class PyrOmeTiffParser(OmeTiffParser):
    def parse_pyramid(self) -> Pyramid:
        base_series = cached_main_tifffile_series(self.format)

        pyramid = Pyramid()
        for i, level in enumerate(base_series.levels):
            page = level[0]

            if page.tilewidth != 0:
                tilewidth = page.tilewidth
            else:
                tilewidth = page.imagewidth

            if page.tilelength != 0:
                tilelength = page.tilelength
            else:
                tilelength = page.imagelength

            subifd = i - 1 if i > 0 else None
            pyramid.insert_tier(
                page.imagewidth, page.imagelength,
                (tilewidth, tilelength),
                subifd=subifd
            )

        return pyramid


class PyrOmeTiffFormat(OmeTiffFormat):
    checker_class = PyrOmeTiffChecker
    parser_class = PyrOmeTiffParser
    reader_class = OmeTiffReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = None

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Pyramidal OME-TIFF"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

#  * Copyright (c) 2020-2022. Authors: see NOTICE file.
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

from typing import TYPE_CHECKING, Type

import numpy as np
import pyvips
from pyvips import Image as VIPSImage
from tifffile import TiffPage, TiffPageSeries

from pims.cache import cached_property
from pims.formats.common.ometiff import OmeTiffReader, PyrOmeTiffFormat
from pims.formats.common.tiff import PlanarTiffFormat
from pims.formats.utils.abstract import AbstractFormat, CachedDataPath
from pims.formats.utils.convertor import AbstractConvertor
from pims.formats.utils.engines.omexml import OmeXml
from pims.formats.utils.engines.tifffile import (
    TifffileChecker, TifffileParser, cached_tifffile,
    remove_tiff_comments
)
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.formats.utils.structures.planes import PlanesInfo
from pims.utils.color import infer_channel_color
from pims.utils.dtypes import dtype_to_bits
from pims.utils.types import parse_float

if TYPE_CHECKING:
    from pims.files.file import Path


def cached_tifffile_baseline_series(format: AbstractFormat) -> TiffPageSeries:
    tf = cached_tifffile(format)
    return format.get_cached('_tf_baseline_series', tf.series.__getitem__, 0)


class ImageJTiffChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if not super().match(pathlike):
                return False

            tf = cls.get_tifffile(pathlike)
            return tf.is_imagej and not tf.is_ome
        except RuntimeError:
            return False


class ImageJTiffParser(TifffileParser):
    @cached_property
    def _parsed_imagej_metadata(self) -> dict:
        return cached_tifffile(self.format).imagej_metadata

    @property
    def baseline_series(self) -> TiffPageSeries:
        return cached_tifffile_baseline_series(self.format)

    @property
    def baseline(self) -> TiffPage:
        return self.baseline_series.pages[0]

    def parse_main_metadata(self) -> ImageMetadata:
        baseline = self.baseline_series

        axes = baseline.get_axes(squeeze=False)  # noqa
        shape = baseline.get_shape(squeeze=False)   # noqa

        imd = ImageMetadata()
        imd.width = shape[axes.index('X')]
        imd.height = shape[axes.index('Y')]
        imd.depth = shape[axes.index('Z')]
        imd.duration = shape[axes.index('T')]
        imd.n_concrete_channels = shape[axes.index('C')]
        imd.n_samples = shape[axes.index('S')]
        # In the case we have unknown extra samples or alpha channel:
        if imd.n_samples not in (1, 3):
            if imd.n_samples > 3:
                imd.n_samples = 3
            else:
                imd.n_samples = 1

        imd.pixel_type = baseline.dtype
        imd.significant_bits = dtype_to_bits(imd.pixel_type)

        for cc_idx in range(imd.n_concrete_channels):
            colors = [infer_channel_color(
                None,
                cc_idx,
                imd.n_concrete_channels
            )] * imd.n_samples

            if imd.n_samples == 3 and colors[0] is None:
                colors = [
                    infer_channel_color(None, i, 3)
                    for i in range(imd.n_samples)
                ]

            names = [None] * imd.n_samples
            if imd.n_samples == 1 and 2 <= imd.n_concrete_channels <= 3:
                names = ['RGB'[cc_idx]]
            elif imd.n_samples == 3:
                names = ['R', 'G', 'B']

            for s in range(imd.n_samples):
                imd.set_channel(ImageChannel(
                    index=cc_idx * imd.n_samples + s,
                    suggested_name=names[s], color=colors[s]
                ))

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        imagej_metadata = self._parsed_imagej_metadata

        tags = self.baseline.tags

        unit = imagej_metadata.get('unit', tags.valueof("ResolutionUnit"))
        imd.physical_size_x = self.parse_physical_size(
            tags.valueof("XResolution"), unit
        )
        imd.physical_size_y = self.parse_physical_size(
            tags.valueof("YResolution"), unit
        )
        imd.physical_size_z = self.parse_physical_size(
            parse_float(imagej_metadata.get('spacing')),
            imagej_metadata.get('unit')
        )

        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()

        for key, value in self._parsed_imagej_metadata.items():
            if key == 'LUTs':
                value = str(value)
            store.set(key, value, namespace="IMAGEJ")
        return store

    def parse_planes(self) -> PlanesInfo:
        imd = self.format.main_imd
        pi = PlanesInfo(
            imd.n_concrete_channels, imd.depth, imd.duration,
            ['page_index'], [np.int64]
        )

        # ImageJ's dimension order is TZCYXS
        shape = [imd.duration, imd.depth, imd.n_concrete_channels]
        for idx, tzc in enumerate(np.ndindex(*shape)):
            t, z, c = tzc
            pi.set(c=c, z=z, t=t, page_index=idx)

        return pi


class ImageJTiffConvertor(AbstractConvertor):
    def convert(self, dest_path: Path) -> bool:
        imd = self.source.full_imd
        shape = (
            imd.duration, imd.depth, imd.n_concrete_channels,
            imd.height, imd.width, imd.n_samples
        )
        storedshape = (imd.n_planes, 1, 1, imd.height, imd.width, imd.n_samples)
        axes = 'TZCYXS'

        ome = OmeXml()
        ome.addimage(imd, shape, storedshape, axes)
        omexml = str(ome)

        vips_source = VIPSImage.new_from_file(
            str(self.source.path), n=imd.n_planes
        )
        vips_source = vips_source.copy()
        vips_source.set('image-description', omexml)

        opts = dict()
        if imd.n_planes > 1:
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
                remove_tiff_comments(dest_path, imd.n_planes, except_pages=[0])
            except Exception: # noqa
                pass
        return ok

    def conversion_format(self) -> Type[AbstractFormat]:
        return PyrOmeTiffFormat


class ImageJTiffFormat(PlanarTiffFormat):
    """
    ImageJ TIFF stack format.

    Known limitations:
    * ImageJ LUTs are ignored.
    * Not sure on behavior for truncated stacks.

    """
    checker_class = ImageJTiffChecker
    parser_class = ImageJTiffParser
    reader_class = OmeTiffReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = ImageJTiffConvertor

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "ImageJ TIFF"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        imd = self.main_imd
        return imd.width > 1024 or imd.height > 1024

    @property
    def media_type(self):
        return "image/tiff"

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
import logging
from operator import itemgetter
from typing import Any, List, Optional, Union

import numpy as np
import pyvips.enums
from pyvips import (
    Image as VIPSImage,
    Interpretation as VIPSInterpretation, Size as VIPSSize  # noqa
)
from pyvips.error import Error as VIPSError

from pims.api.exceptions import MetadataParsingProblem
from pims.formats import AbstractFormat
from pims.formats.utils.convertor import AbstractConvertor
from pims.formats.utils.engines.exiftool import ExifToolParser
from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.reader import AbstractReader
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.processing.region import Region, Tile
from pims.utils.dtypes import dtype_to_bits
from pims.utils.vips import (
    vips_format_to_dtype,
    vips_interpretation_to_mode
)

log = logging.getLogger("pims.formats")


def cached_vips_file(format: AbstractFormat) -> VIPSImage:
    """Get cached vips object for the image format."""
    return format.get_cached('_vips', VIPSImage.new_from_file, str(format.path))


def get_vips_field(
    vips_image: VIPSImage, field: str, default: Any = None
) -> Any:
    try:
        return vips_image.get_value(field)
    except VIPSError:
        return default


class VipsParser(ExifToolParser, AbstractParser):
    ALLOWED_MODES = ('L', 'RGB', 'CMYK')

    def parse_main_metadata(self) -> ImageMetadata:
        image = cached_vips_file(self.format)

        imd = ImageMetadata()
        imd.width = image.width
        imd.height = image.height
        imd.depth = 1
        imd.duration = 1
        imd.n_concrete_channels = 1
        imd.n_samples = image.bands

        imd.pixel_type = np.dtype(vips_format_to_dtype[image.format])
        imd.significant_bits = dtype_to_bits(imd.pixel_type)

        mode = vips_interpretation_to_mode.get(image.interpretation)
        if mode in self.ALLOWED_MODES:
            for i, name in enumerate(mode):
                imd.set_channel(ImageChannel(index=i, suggested_name=name))
        else:
            log.error(f"{self.format.path}: Mode {mode} is not supported.")
            raise MetadataParsingProblem(self.format.path)

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()  # Get from ExifToolParser
        return store


class VipsReader(AbstractReader):
    @staticmethod
    def vips_filename_with_options(filename: str, **options) -> str:
        """Create filename with options as expected by vips."""
        if len(options) > 0:
            opt_string = ','.join(f"{k}={v}" for (k, v) in options.items())
            opt_string = '[' + opt_string + ']'
            return filename + opt_string
        return filename

    @staticmethod
    def _extract_channels(im: VIPSImage, c: Optional[Union[int, List[int]]]) -> VIPSImage:
        if c is None or im.bands == len(c):
            return im
        elif type(c) is int or len(c) == 1:
            if len(c) == 1:
                c = c[0]
            return im.extract_band(c)
        else:
            channels = list(itemgetter(*c)(im))
            im = channels[0].bandjoin(channels[1:])
            return im

    def vips_thumbnail(
        self, width: int, height: int, **loader_options
    ) -> VIPSImage:
        """Get VIPS thumbnail using vips shrink-on-load features."""

        filename = self.vips_filename_with_options(
            str(self.format.path),
            **loader_options
        )

        image = cached_vips_file(self.format)
        if image.interpretation in ("grey16", "rgb16", "cmyk"):
            # Related to https://github.com/libvips/libvips/issues/1941 ?
            return VIPSImage.thumbnail(
                filename, width, height=height,
                size=VIPSSize.FORCE, linear=True
            ).colourspace(image.interpretation)

        return VIPSImage.thumbnail(
            filename, width, height=height, size=VIPSSize.FORCE
        )

    def read_thumb(
        self, out_width: int, out_height: int, precomputed: bool = False,
        c: Optional[Union[int, List[int]]] = None, **other
    ) -> VIPSImage:
        im = self.vips_thumbnail(out_width, out_height)
        if im.hasalpha():
            im = im.flatten()
        return self._extract_channels(im, c)

    def read_window(
        self, region: Region, out_width: int, out_height: int,
        c: Optional[Union[int, List[int]]] = None, **other
    ) -> VIPSImage:
        image = cached_vips_file(self.format)
        region = region.scale_to_tier(self.format.pyramid.base)
        im = image.crop(region.left, region.top, region.width, region.height)
        if im.hasalpha():
            im = im.flatten()
        return self._extract_channels(im, c)

    def read_tile(
        self, tile: Tile, c: Optional[Union[int, List[int]]] = None, **other
    ) -> VIPSImage:
        return self.read_window(
            tile, int(tile.width), int(tile.height), c, **other
        )


# [HISTOGRAM REFACTORING] Not used in practice ? to delete ?
# class VipsHistogramReader(NullHistogramReader):
#     def is_complete(self) -> bool:
#         image = cached_vips_file(self.format)
#         return image.width * image.height <= get_settings().max_pixels_complete_histogram
#
#     def vips_hist_image(self) -> VIPSImage:
#         if self.is_complete():
#             return cached_vips_file(self.format)
#
#         def _thumb(format):
#             length = get_settings().max_length_complete_histogram
#             image = cached_vips_file(format)
#             if image.interpretation in ("grey16", "rgb16"):
#                 return VIPSImage.thumbnail(str(format.path), length, linear=True)\
#                     .colourspace(image.interpretation)
#             return VIPSImage.thumbnail(str(format.path), length)
#
#         return self.format.get_cached('_vips_hist_image', _thumb, self.format)
#
#     def type(self) -> HistogramType:
#         if self.is_complete():
#             return HistogramType.COMPLETE
#         else:
#             return HistogramType.FAST
#
#     def image_bounds(self) -> Tuple[int, int]:
#         image = self.vips_hist_image()
#         vips_stats = image.stats()
#         np_stats = vips_to_numpy(vips_stats).astype(np.int)
#         return tuple(np_stats[0, 0:2, 0])
#
#     def image_histogram(self, squeeze=True):
#         image = self.vips_hist_image()
#         return np.sum(vips_to_numpy(image.hist_find()), axis=2).squeeze()
#
#     def channels_bounds(self) -> List[Tuple[int, int]]:
#         image = self.vips_hist_image()
#         vips_stats = image.stats()
#         np_stats = vips_to_numpy(vips_stats).astype(np.int)
#         return list(map(tuple, np_stats[1:, :2, 0]))
#
#     def channel_bounds(self, c: int) -> Tuple[int, int]:
#         image = self.vips_hist_image()
#         vips_stats = image.stats()
#         np_stats = vips_to_numpy(vips_stats).astype(np.int)
#         return tuple(np_stats[c + 1, :2, 0])
#
#     def channel_histogram(self, c, squeeze=True):
#         image = self.vips_hist_image()
#         return vips_to_numpy(image.hist_find(band=c))[0, :, 0]
#
#     def planes_bounds(self):
#         return self.channels_bounds()
#
#     def plane_bounds(self, c, z, t):
#         return self.channel_bounds(c)
#
#     def plane_histogram(self, c, z, t, squeeze=True):
#         return self.channel_histogram(c)


class VipsSpatialConvertor(AbstractConvertor):
    def vips_source(self):
        return cached_vips_file(self.source)

    def conversion_format(self):
        from pims.formats.common.tiff import PyrTiffFormat
        return PyrTiffFormat

    def convert(self, dest_path):
        source = self.vips_source()
        if source.interpretation == VIPSInterpretation.CMYK:
            source = source.colourspace(VIPSInterpretation.SRGB)

        result = source.tiffsave(
            str(dest_path), pyramid=True, tile=True,
            tile_width=256, tile_height=256, bigtiff=True,
            properties=False, strip=True,
            depth=pyvips.enums.ForeignDzDepth.ONETILE,
            compression=pyvips.enums.ForeignTiffCompression.LZW,
            region_shrink=pyvips.enums.RegionShrink.MEAN
        )
        return not bool(result)

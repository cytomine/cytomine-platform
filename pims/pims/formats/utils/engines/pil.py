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
from typing import Optional

import numpy as np
from PIL import Image as PILImage

from pims.api.exceptions import MetadataParsingProblem
from pims.formats import AbstractFormat
from pims.formats.utils.engines.exiftool import ExifToolParser
from pims.formats.utils.engines.vips import VipsSpatialConvertor
from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.reader import AbstractReader
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.processing.adapters import pil_to_vips
from pims.processing.region import Region

log = logging.getLogger("pims.formats")


def cached_pillow_file(
    format: AbstractFormat, pil_format_slug: Optional[str]
) -> PILImage:
    slugs = [pil_format_slug] if pil_format_slug else None
    return format.get_cached('_pil', PILImage.open, format.path, formats=slugs)


def cached_palette_converted_pillow_file(
    format: AbstractFormat, pil_format_slug: Optional[str]
) -> PILImage:
    """Palette converted pillow image"""
    def _open_palette_converted(_format, _pil_format_slug):
        image = cached_pillow_file(format, pil_format_slug)
        palette = getattr(image, "palette", None)
        if palette:
            image = image.convert(palette.mode)
        return image

    return format.get_cached(
        '_pil_palette_converted', _open_palette_converted,
        format.path, pil_format_slug
    )


class PillowParser(ExifToolParser, AbstractParser):
    FORMAT_SLUG = None

    def parse_main_metadata(self) -> ImageMetadata:
        image = cached_palette_converted_pillow_file(self.format, self.FORMAT_SLUG)

        imd = ImageMetadata()
        imd.width = image.width
        imd.height = image.height
        imd.depth = 1
        imd.duration = getattr(image, "n_frames", 1)

        mode = image.mode  # Possible values: 1, L, RGB
        imd.pixel_type = np.dtype("uint8")
        imd.significant_bits = 8 if mode != "1" else 1

        imd.n_concrete_channels = 1
        channel_mode = "L" if mode == "1" else mode
        if channel_mode in ("L", "RGB"):
            imd.n_samples = len(channel_mode)
            for i, name in enumerate(channel_mode):
                imd.set_channel(ImageChannel(index=i, suggested_name=name))
        else:
            # https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#bmp
            log.error(f"{self.format.path}: Mode {mode} is not supported.")
            raise MetadataParsingProblem(self.format.path)

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()  # Get from ExifToolParser
        return store


class SimplePillowReader(AbstractReader):
    FORMAT_SLUG = None

    def read_thumb(
        self, out_width, out_height, precomputed=None,
        c=None, z=None, t=None
    ):
        image = cached_pillow_file(self.format, self.FORMAT_SLUG)

        # We do not use Pillow resize() method as resize will be better handled
        # by vips in response generation.
        return self.read_window(
            Region(0, 0, image.width, image.height),
            out_width, out_height, c, z, t
        )

    def read_window(self, region, out_width, out_height, c=None, z=None, t=None):
        image = cached_palette_converted_pillow_file(self.format, self.FORMAT_SLUG)
        region = region.scale_to_tier(self.format.pyramid.base)
        return image.crop(
            (region.left, region.top, region.right, region.bottom)
        )

    def read_tile(self, tile, c=None, z=None, t=None):
        return self.read_window(tile, int(tile.width), int(tile.height), c, z, t)


# [HISTOGRAM REFACTORING] Not used in practice ? to delete ?
# class PillowHistogramReader(NullHistogramReader):
#     FORMAT_SLUG = None
#
#     def is_complete(self):
#         image = cached_pillow_file(self.format, self.FORMAT_SLUG)
#         return image.width <= 1024 and image.height <= 1024
#
#     def pillow_hist_image(self):
#         if self.is_complete():
#             return cached_pillow_file(self.format, self.FORMAT_SLUG)
#
#         def _thumb(format, fslug):
#             image = cached_pillow_file(format, fslug)
#             if image.width > image.height:
#                 w, h = get_rationed_resizing(1024, image.width, image.height)
#             else:
#                 h, w = get_rationed_resizing(1024, image.height, image.width)
#             return image.resize((w, h))
#
#         return self.format.get_cached(
#             '_pillow_hist_image', _thumb, self.format, self.FORMAT_SLUG
#         )
#
#     @property
#     def use_pillow(self):
#         return not self.zhf
#
#     def type(self):
#         if self.use_pillow:
#             if self.is_complete():
#                 return HistogramType.COMPLETE
#             else:
#                 return HistogramType.FAST
#         return super().type()
#
#     def image_bounds(self):
#         if not self.use_pillow:
#             return super().image_bounds()
#         image = self.pillow_hist_image()
#         extrema = np.asarray(image.getextrema())
#         return extrema[:, 0].min(), extrema[:, 1].max()
#
#     def image_histogram(self, squeeze=True):
#         if not self.use_pillow:
#             return super().image_histogram()
#         image = self.pillow_hist_image()
#         n_values = 256
#         histogram = pil_to_numpy(image.histogram())
#         histogram = histogram.reshape(-1, n_values)
#         return np.sum(histogram, axis=0)
#
#     def channels_bounds(self):
#         if not self.use_pillow:
#             return super().channels_bounds()
#         image = self.pillow_hist_image()
#         return image.getextrema()
#
#     def channel_bounds(self, c):
#         if not self.use_pillow:
#             return super().channel_bounds(c)
#         image = self.pillow_hist_image()
#         return image.getextrema()[c]
#
#     def channel_histogram(self, c, squeeze=True):
#         if not self.use_pillow:
#             return super().channel_histogram(c)
#         image = self.pillow_hist_image()
#         n_values = 256
#         histogram = pil_to_numpy(image.histogram())
#         histogram = histogram.reshape(-1, n_values)
#         return histogram[c]
#
#     def planes_bounds(self):
#         if not self.use_pillow:
#             return super().planes_bounds()
#         return self.channels_bounds()
#
#     def plane_bounds(self, c, z, t):
#         if not self.use_pillow:
#             return super().plane_bounds(c, z, t)
#         return self.channel_bounds(c)
#
#     def plane_histogram(self, c, z, t, squeeze=True):
#         if not self.use_pillow:
#             return super().plane_histogram(c, z, t)
#         return self.channel_histogram(c)


class PillowSpatialConvertor(VipsSpatialConvertor):
    def vips_source(self):
        return pil_to_vips(cached_palette_converted_pillow_file(self.source, None))

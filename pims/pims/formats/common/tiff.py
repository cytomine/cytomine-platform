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
from typing import List, Optional, Union

from pyvips import Image as VIPSImage

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.engines.tifffile import TIFF_FLAGS, TifffileChecker, TifffileParser
from pims.formats.utils.engines.vips import VipsReader, VipsSpatialConvertor
# -----------------------------------------------------------------------------
# PYRAMIDAL TIFF
from pims.formats.utils.histogram import DefaultHistogramReader


class PyrTiffChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if not super().match(pathlike):
                return False

            tf = cls.get_tifffile(pathlike)
            for name in TIFF_FLAGS:
                if getattr(tf, 'is_' + name, False):
                    return False

            if len(tf.series) == 1:
                baseline = tf.series[0]
                if baseline and baseline.is_pyramidal:
                    for level in baseline.levels:
                        if level.keyframe.is_tiled is False:
                            return False
                    return True

            return False
        except RuntimeError:
            return False


class PyrTiffVipsReader(VipsReader):
    # Thumbnail already uses shrink-on-load feature in default VipsReader
    # (i.e it loads the right pyramid level according the requested dimensions)

    def read_window(
        self, region, out_width, out_height,
        c: Optional[Union[int, List[int]]] = None, **other
    ):
        tier = self.format.pyramid.most_appropriate_tier(
            region, (out_width, out_height)
        )
        region = region.scale_to_tier(tier)

        page = tier.data.get('page_index')
        tiff_page = VIPSImage.tiffload(str(self.format.path), page=page)
        im = tiff_page.extract_area(
            region.left, region.top, region.width, region.height
        )
        return self._extract_channels(im, c)

    def read_tile(
        self, tile, c: Optional[Union[int, List[int]]] = None, **other
    ):
        tier = tile.tier
        page = tier.data.get('page_index')
        tiff_page = VIPSImage.tiffload(str(self.format.path), page=page)

        # There is no direct access to underlying tiles in vips
        # But the following computation match vips implementation so that only the tile
        # that has to be read is read.
        # https://github.com/jcupitt/tilesrv/blob/master/tilesrv.c#L461
        # TODO: is direct tile access significantly faster ?
        im = tiff_page.extract_area(
            tile.left, tile.top, tile.width, tile.height
        )
        return self._extract_channels(im, c)


class PyrTiffFormat(AbstractFormat):
    checker_class = PyrTiffChecker
    parser_class = TifffileParser
    reader_class = PyrTiffVipsReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Pyramidal TIFF"

    @classmethod
    def is_spatial(cls):
        return True

    @classmethod
    def is_writable(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

    @property
    def media_type(self):
        return "image/pyrtiff"


# -----------------------------------------------------------------------------
# PLANAR TIFF


class PlanarTiffChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if not super().match(pathlike):
                return False

            tf = cls.get_tifffile(pathlike)
            for name in TIFF_FLAGS:
                if getattr(tf, 'is_' + name, False):
                    return False

            if len(tf.series) >= 1:
                baseline = tf.series[0]
                if baseline and not baseline.is_pyramidal\
                        and len(baseline.levels) == 1:
                    return True

            return False
        except RuntimeError:
            return False


class PlanarTiffFormat(AbstractFormat):
    checker_class = PlanarTiffChecker
    parser_class = TifffileParser
    reader_class = VipsReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = VipsSpatialConvertor

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Planar TIFF"

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

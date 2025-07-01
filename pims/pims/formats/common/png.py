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

from pint import Quantity

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.engines.vips import (
    VipsParser, VipsReader,
    VipsSpatialConvertor
)
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageMetadata
from pims.utils import UNIT_REGISTRY
from pims.utils.types import parse_datetime, parse_float

log = logging.getLogger("pims.formats")


class PNGChecker(SignatureChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        buf = cls.get_signature(pathlike)
        return (len(buf) > 3 and
                buf[0] == 0x89 and
                buf[1] == 0x50 and
                buf[2] == 0x4E and
                buf[3] == 0x47)


class PNGParser(VipsParser):
    def parse_main_metadata(self) -> ImageMetadata:
        imd = super().parse_main_metadata()

        # Do not count alpha channel if any
        if imd.n_samples in (2, 4):
            imd.n_samples -= 1

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        raw = self.format.raw_metadata

        desc_fields = ("PNG.Comment", "EXIF.ImageDescription", "EXIF.UserComment")
        imd.description = raw.get_first_value(desc_fields)

        date_fields = ("PNG.CreationTime", "PNG.ModifyDate", "EXIF.CreationDate",
                       "EXIF.DateTimeOriginal", "EXIF.ModifyDate")
        imd.acquisition_datetime = parse_datetime(raw.get_first_value(date_fields))

        imd.physical_size_x = self.parse_physical_size(
            raw.get_value("PNG.PixelsPerUnitX"),
            raw.get_value("PNG.PixelUnits"), True
        )
        imd.physical_size_y = self.parse_physical_size(
            raw.get_value("PNG.PixelsPerUnitY"),
            raw.get_value("PNG.PixelUnits"), True
        )
        if imd.physical_size_x is None and imd.physical_size_y is None:
            imd.physical_size_x = self.parse_physical_size(
                raw.get_value("EXIF.XResolution"),
                raw.get_value("EXIF.ResolutionUnit"), False
            )
            imd.physical_size_y = self.parse_physical_size(
                raw.get_value("EXIF.YResolution"),
                raw.get_value("EXIF.ResolutionUnit"), False
            )
        imd.is_complete = True
        return imd

    @staticmethod
    def parse_physical_size(
        physical_size: Optional[str], unit: Optional[str], inverse: bool
    ) -> Optional[Quantity]:
        supported_units = {1: "meter", 2: "inch"}
        if type(unit) == str:
            supported_units = {"meters": "meter", "inches": "inch"}
        if physical_size is not None and unit in supported_units.keys():
            physical_size = parse_float(physical_size)
            if physical_size is None or physical_size <= 0:
                return None
            if inverse:
                physical_size = 1 / physical_size
            return physical_size * UNIT_REGISTRY(supported_units[unit])
        return None


class PNGFormat(AbstractFormat):
    """PNG Formats. Do not support (yet) APNG sequences.

    References
        https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#png
        https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#apng-sequences
        https://exiftool.org/TagNames/PNG.html
        http://www.libpng.org/pub/png/spec/
        https://github.com/ome/bioformats/blob/master/components/formats-bsd/src/loci/formats/in/APNGReader.java
    """

    checker_class = PNGChecker
    parser_class = PNGParser
    reader_class = VipsReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = VipsSpatialConvertor

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        imd = self.main_imd
        return imd.width > 1024 or imd.height > 1024

    @property
    def media_type(self):
        return "image/png"

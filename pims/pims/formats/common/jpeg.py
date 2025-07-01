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


class JPEGChecker(SignatureChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        buf = cls.get_signature(pathlike)
        return (len(buf) > 2 and
                buf[0] == 0xFF and
                buf[1] == 0xD8 and
                buf[2] == 0xFF)


class JPEGParser(VipsParser):
    @staticmethod
    def parse_physical_size(
        physical_size: Optional[int], unit: Optional[str]
    ) -> Optional[Quantity]:
        supported_units = ("meters", "inches", "cm")
        if physical_size is not None and unit in supported_units:
            physical_size = parse_float(physical_size)
            if physical_size is not None and physical_size > 0:
                return physical_size * UNIT_REGISTRY(unit)
        return None

    def parse_known_metadata(self) -> ImageMetadata:
        # Tags reference: https://exiftool.org/TagNames/JPEG.html
        imd = super().parse_known_metadata()
        raw = self.format.raw_metadata

        desc_fields = ("File.Comment", "EXIF.ImageDescription", "EXIF.UserComment")
        imd.description = raw.get_first_value(desc_fields)

        date_fields = ("EXIF.CreationDate", "EXIF.DateTimeOriginal", "EXIF.ModifyDate")
        imd.acquisition_datetime = parse_datetime(raw.get_first_value(date_fields))

        imd.physical_size_x = self.parse_physical_size(
            raw.get_value("EXIF.XResolution"),
            raw.get_value("EXIF.ResolutionUnit")
        )
        imd.physical_size_y = self.parse_physical_size(
            raw.get_value("EXIF.YResolution"),
            raw.get_value("EXIF.ResolutionUnit")
        )
        if imd.physical_size_x is None and imd.physical_size_y is None:
            imd.physical_size_x = self.parse_physical_size(
                raw.get_value("JFIF.XResolution"),
                raw.get_value("JFIF.ResolutionUnit")
            )
            imd.physical_size_y = self.parse_physical_size(
                raw.get_value("JFIF.YResolution"),
                raw.get_value("JFIF.ResolutionUnit")
            )
        imd.is_complete = True
        return imd


class JPEGFormat(AbstractFormat):
    """JPEG Format.

    References
        https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#jpeg
        https://exiftool.org/TagNames/JPEG.html
        https://www.w3.org/Graphics/JPEG/

    """
    checker_class = JPEGChecker
    parser_class = JPEGParser
    reader_class = VipsReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = VipsSpatialConvertor

    def __init__(self, *args, **kwargs):
        super(JPEGFormat, self).__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        imd = self.main_imd
        # JPEG can only have 1 (L), 3 (RGB) or 4 (CMYK) channels.
        # We do not support CMYK as is (subtractive colorspace) so it has to be converted
        return imd.width > 1024 or imd.height > 1024 or imd.n_channels == 4

    @property
    def media_type(self):
        return "image/jpeg"

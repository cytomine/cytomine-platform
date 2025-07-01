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
from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.engines.pil import (
    PillowParser, PillowSpatialConvertor, SimplePillowReader
)
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageMetadata
from pims.utils import UNIT_REGISTRY
from pims.utils.types import parse_float

log = logging.getLogger("pims.formats")


class BMPChecker(SignatureChecker):

    @classmethod
    def match(cls, pathlike):
        buf = cls.get_signature(pathlike)
        return (len(buf) > 1 and
                buf[0] == 0x42 and
                buf[1] == 0x4D)


class BMPParser(PillowParser):
    FORMAT_SLUG = 'BMP'

    def parse_known_metadata(self) -> ImageMetadata:
        # Tags reference: https://exiftool.org/TagNames/BMP.html
        imd = super().parse_known_metadata()
        raw = self.format.raw_metadata

        imd.description = raw.get_value("File.Comment")
        imd.acquisition_datetime = self.format.path.creation_datetime
        imd.physical_size_x = self.parse_physical_size(raw.get_value("File.PixelsPerMeterX"))
        imd.physical_size_y = self.parse_physical_size(raw.get_value("File.PixelsPerMeterY"))
        imd.is_complete = True
        return imd

    @staticmethod
    def parse_physical_size(physical_size: Optional[str]) -> Optional[Quantity]:
        if physical_size is not None:
            physical_size = parse_float(physical_size)
            if physical_size is not None and physical_size > 0:
                return 1 / physical_size * UNIT_REGISTRY("meters")
        return None


class BMPReader(SimplePillowReader):
    FORMAT_SLUG = 'BMP'


class BMPFormat(AbstractFormat):
    """BMP Format.

    References
        https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#bmp
        https://exiftool.org/TagNames/BMP.html
    """

    checker_class = BMPChecker
    parser_class = BMPParser
    reader_class = BMPReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = PillowSpatialConvertor

    @classmethod
    def init(cls):
        # https://github.com/python-pillow/Pillow/issues/5036
        from PIL import BmpImagePlugin
        assert BmpImagePlugin

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        imd = self.main_imd
        return imd.width > 1400 or imd.height > 1400

    @property
    def media_type(self):
        return "image/bmp"

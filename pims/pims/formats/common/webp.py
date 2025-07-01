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


class WebPChecker(SignatureChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        buf = cls.get_signature(pathlike)
        return (len(buf) > 13 and
                buf[0] == 0x52 and
                buf[1] == 0x49 and
                buf[2] == 0x46 and
                buf[3] == 0x46 and
                buf[8] == 0x57 and
                buf[9] == 0x45 and
                buf[10] == 0x42 and
                buf[11] == 0x50 and
                buf[12] == 0x56 and
                buf[13] == 0x50)


class WebPParser(VipsParser):
    def parse_main_metadata(self) -> ImageMetadata:
        imd = super().parse_main_metadata()
        # Do not count alpha channel if any
        if imd.n_samples in (2, 4):
            imd.n_samples -= 1
        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        raw = self.format.raw_metadata
        # Tags reference: https://exiftool.org/TagNames/RIFF.html

        desc_fields = ("RIFF.Comment", "EXIF.ImageDescription", "EXIF.UserComment")
        imd.description = raw.get_first_value(desc_fields)

        date_fields = (
            "RIFF.DateTimeOriginal", "EXIF.CreationDate", "EXIF.DateTimeOriginal",
            "EXIF.ModifyDate"
        )
        imd.acquisition_datetime = parse_datetime(raw.get_first_value(date_fields))

        imd.physical_size_x = self.parse_physical_size(
            raw.get_value("EXIF.XResolution"),
            raw.get_value("EXIF.ResolutionUnit")
        )
        imd.physical_size_y = self.parse_physical_size(
            raw.get_value("EXIF.YResolution"),
            raw.get_value("EXIF.ResolutionUnit")
        )

        if imd.duration > 1:
            total_time = raw.get_value(
                "RIFF.Duration"
            )  # String such as "0.84 s" -> all sequence duration
            if total_time:
                frame_rate = imd.duration / UNIT_REGISTRY(total_time)
                imd.frame_rate = frame_rate.to("Hz")

        imd.is_complete = True
        return imd

    @staticmethod
    def parse_physical_size(
        physical_size: Optional[str], unit: Optional[str]
    ) -> Optional[Quantity]:
        supported_units = ("meters", "inches", "cm")
        if physical_size is not None and unit in supported_units:
            physical_size = parse_float(physical_size)
            if physical_size is not None and physical_size > 0:
                return physical_size * UNIT_REGISTRY(unit)
        return None


class WebPFormat(AbstractFormat):
    """WebP Format. Do not support (yet) WebP sequences.

    References
        https://libvips.github.io/libvips/API/current/VipsForeignSave.html#vips-webpload
        https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#webp
        https://exiftool.org/TagNames/RIFF.html
    """

    checker_class = WebPChecker
    parser_class = WebPParser
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
        return "image/webp"

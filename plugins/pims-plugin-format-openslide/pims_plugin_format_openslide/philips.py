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
from datetime import datetime
from typing import Optional

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.engines.tifffile import TifffileChecker
from pims.formats.utils.engines.vips import cached_vips_file, get_vips_field
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageMetadata
from pims_plugin_format_openslide.utils.engine import OpenslideVipsParser, OpenslideVipsReader


class PhilipsChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if super().match(pathlike):
                tf = cls.get_tifffile(pathlike)
                return tf.is_philips
            return False
        except RuntimeError:
            return False


class PhilipsParser(OpenslideVipsParser):
    # TODO: parse ourselves the philips tiff comment tag
    def parse_known_metadata(self) -> ImageMetadata:
        image = cached_vips_file(self.format)

        imd = super().parse_known_metadata()

        acquisition_date = self.parse_acquisition_date(
            get_vips_field(image, 'philips.DICOM_ACQUISITION_DATETIME')
        )
        if acquisition_date:
            imd.acquisition_datetime = acquisition_date

        imd.is_complete = True
        return imd

    @staticmethod
    def parse_acquisition_date(date: str) -> Optional[datetime]:
        # Have seen: 20181019105847.000000
        try:
            return datetime.strptime(date, "%Y%m%d%H%M%S.%f")
        except (ValueError, TypeError):
            return None


class PhilipsFormat(AbstractFormat):
    """
    Philips TIFF format.

    References
    ----------
    * https://openslide.org/formats/philips/
    """

    checker_class = PhilipsChecker
    parser_class = PhilipsParser
    reader_class = OpenslideVipsReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Philips TIFF"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

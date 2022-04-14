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
from pims.utils.types import parse_datetime
from pims_plugin_format_openslide.utils.engine import OpenslideVipsParser, OpenslideVipsReader


class BifChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if super().match(pathlike):
                tf = cls.get_tifffile(pathlike)
                return tf.is_bif
            return False
        except RuntimeError:
            return False


class BifParser(OpenslideVipsParser):
    # TODO: parse ourselves ventana xml
    def parse_known_metadata(self) -> ImageMetadata:
        image = cached_vips_file(self.format)

        imd = super().parse_known_metadata()

        imd.acquisition_datetime = self.parse_acquisition_date(
            get_vips_field(image, 'ventana.ScanDate')
        )

        imd.microscope.model = get_vips_field(image, 'ventana.ScannerModel')
        imd.is_complete = True
        return imd

    @staticmethod
    def parse_acquisition_date(date: str) -> Optional[datetime]:
        # Have seen: 8/18/2014 09:44:30 | 8/30/2017 12:04:52 PM
        return parse_datetime(
            date, ["%m/%d/%Y %H:%M:%S", "%m/%d/%Y %H:%M:%S %p"]
        )


class BifFormat(AbstractFormat):
    """
    Ventana BIF (TIFF) format.

    References
    ----------
    * https://openslide.org/formats/ventana/
    * https://github.com/openslide/openslide/blob/main/src/openslide-vendor-ventana.c
    * https://github.com/ome/bioformats/blob/develop/components/formats-gpl/src/loci/formats/in/VentanaReader.java
    """

    checker_class = BifChecker
    parser_class = BifParser
    reader_class = OpenslideVipsReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Ventana BIF"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

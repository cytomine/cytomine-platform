# * Copyright (c) 2020. Authors: see NOTICE file.
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *      http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
from functools import cached_property

from pims.formats import AbstractFormat
from pims.formats.utils.engines.tifffile import TifffileChecker
from pims.formats.utils.engines.vips import VipsHistogramReader, cached_vips_file, get_vips_field
from pims.formats.utils.metadata import parse_datetime

from pims_plugin_format_openslide.utils.engine import OpenslideVipsParser, OpenslideVipsReader


class BifChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike):
        if super().match(pathlike):
            tf = cls.get_tifffile(pathlike)
            xmp = tf.pages[0].tags.get('XMP')
            return xmp and b'<iScan' in xmp.value
        return False


class BifParser(OpenslideVipsParser):
    # TODO: parse ourselves ventana xml
    def parse_known_metadata(self):
        image = cached_vips_file(self.format)

        imd = super().parse_known_metadata()

        acquisition_date = self.parse_acquisition_date(
            get_vips_field(image, 'ventana.ScanDate')
        )
        if acquisition_date:
            imd.acquisition_datetime = acquisition_date

        imd.is_complete = True
        return imd

    @staticmethod
    def parse_acquisition_date(date):
        # Have seen: 8/18/2014 09:44:30 | 8/30/2017 12:04:52 PM
        return parse_datetime(date, ["%m/%d/%Y %H:%M:%S", "%m/%d/%Y %H:%M:%S %p"])


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
    histogram_reader_class = VipsHistogramReader

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

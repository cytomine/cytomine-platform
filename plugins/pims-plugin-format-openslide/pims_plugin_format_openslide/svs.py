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

from pint import Quantity
from tifffile import astype

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.engines.tifffile import TifffileChecker, TifffileParser, cached_tifffile
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageMetadata, MetadataStore
from pims.utils import UNIT_REGISTRY
from pims.utils.types import parse_float
from pims_plugin_format_openslide.utils.engine import OpenslideVipsReader


def _find_named_series(tf, name):
    return next((s for s in tf.series if s.name.lower() == name), None)


class SVSChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if super().match(pathlike):
                tf = cls.get_tifffile(pathlike)
                return tf.is_svs
            return False
        except RuntimeError:
            return False


class SVSParser(TifffileParser):
    @cached_property
    def _parsed_svs_description(self) -> dict:
        """
        Return metadata from Aperio image description as dict.
        The Aperio image description format is unspecified.
        Expect failures.
        """
        description = self.baseline.description
        if not description.startswith('Aperio '):
            raise ValueError('invalid Aperio image description')

        result = {}
        items = description.split('|')
        headers = items[0].split('\n', 1)
        key, value = headers[0].strip().rsplit(None, 1)  # 'Aperio Image Library'
        result[key.strip()] = value.strip()
        if len(headers) == 1:
            return result
        result['Description'] = headers[1].strip()  # TODO: parse this?

        if len(items) == 1:
            return result
        for item in items[1:]:
            key, value = item.split(' = ')
            result[key.strip()] = astype(value.strip())
        return result

    @staticmethod
    def parse_physical_size(
        physical_size: Optional[str], unit: Optional[str] = None
    ) -> Optional[Quantity]:
        if physical_size is not None and parse_float(physical_size) is not None:
            return parse_float(physical_size) * UNIT_REGISTRY("micrometers")
        return None

    @staticmethod
    def parse_acquisition_date(
        date: Optional[str], time: Optional[str] = None
    ) -> Optional[datetime]:
        """
        Date examples: 11/25/13 , 2013-12-05T12:49:03.69Z
        Time examples: 15:10:34
        """
        try:
            if date and time:
                return datetime.strptime(f"{date} {time}", "%m/%d/%y %H:%M:%S")
            elif date:
                return datetime.strptime(date, "%Y-%m-%dT%H:%M:%SZ")
            else:
                return None
        except (ValueError, TypeError):
            return None

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        svs_metadata = self._parsed_svs_description

        imd.description = self.baseline.description
        imd.acquisition_datetime = self.parse_acquisition_date(
            svs_metadata.get("Date"), svs_metadata.get("Time"))

        imd.physical_size_x = self.parse_physical_size(svs_metadata.get("MPP"))
        imd.physical_size_y = imd.physical_size_x
        imd.objective.nominal_magnification = parse_float(
            svs_metadata.get("AppMag")
        )

        for series in cached_tifffile(self.format).series:
            name = series.name.lower()
            if name == "thumbnail":
                associated = imd.associated_thumb
            elif name == "label":
                associated = imd.associated_label
            elif name == "macro":
                associated = imd.associated_macro
            else:
                continue
            page = series[0]
            associated.width = page.imagewidth
            associated.height = page.imagelength
            associated.n_channels = page.samplesperpixel

        imd.is_complete = True
        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()

        for key, value in self._parsed_svs_description.items():
            store.set(key, value, namespace="APERIO")
        return store


class SVSFormat(AbstractFormat):
    """
    Aperio SVS format.

    Known limitations:
    * Do not work with 16-bit SVS images
    * No support for z-Stack (does it really exist ?)

    References:
        https://openslide.org/formats/aperio/
        https://docs.openmicroscopy.org/bio-formats/6.5.1/formats/aperio-svs-tiff.html
        https://github.com/ome/bioformats/blob/master/components/formats-gpl/src/loci/formats/in/SVSReader.java
        https://www.leicabiosystems.com/digital-pathology/manage/aperio-imagescope/
        https://github.com/openslide/openslide/blob/master/src/openslide-vendor-aperio.c
    """
    checker_class = SVSChecker
    parser_class = SVSParser
    reader_class = OpenslideVipsReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Leica Aperio SVS"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

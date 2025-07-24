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
from pyvips import Image as VipsImage
from tifffile import astype

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.engines.tifffile import TifffileChecker, TifffileParser, cached_tifffile
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.metadata import ImageMetadata, MetadataStore
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils.types import parse_float, parse_int
from pims_plugin_format_openslide.utils.engine import OpenslideVipsReader


class NDPIChecker(TifffileChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            if super().match(pathlike):
                tf = cls.get_tifffile(pathlike)
                return tf.is_ndpi
            return False
        except RuntimeError:
            return False


class NDPIParser(TifffileParser):
    @cached_property
    def _parsed_ndpi_tags(self) -> dict:
        tags = self.baseline.ndpi_tags

        comments = tags.get("Comments", None)
        if comments:
            # Comments tag (65449): ASCII key=value pairs (not always present)
            lines = comments.split('\n')
            for line in lines:
                key, value = line.split('=')
                tags[key.strip()] = astype(value.strip())
            del tags["Comments"]
        return tags

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        ndpi_metadata = self._parsed_ndpi_tags

        # Magnification extracted by OpenSlide: nominal_magnification
        # Magnification extracted by BioFormats: calibrated_magnification
        imd.objective.nominal_magnification = parse_float(
            ndpi_metadata.get("Magnification")
        )
        imd.objective.calibrated_magnification = parse_float(
            ndpi_metadata.get("Objective.Lens.Magnificant")  # noqa
        )  # Not a typo!

        imd.microscope.model = ndpi_metadata.get("Model")

        # NDPI series: Baseline, Macro, Map
        for series in cached_tifffile(self.format).series:
            name = series.name.lower()
            if name == "macro":
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

        skipped_tags = ('McuStarts', '65439')
        for key, value in self._parsed_ndpi_tags.items():
            if key not in skipped_tags:
                store.set(key, value, namespace="HAMAMATSU")
        return store

    def parse_pyramid(self) -> Pyramid:
        # Tifffile is inconsistent with Openslide
        # https://github.com/cgohlke/tifffile/issues/41
        openslide = VipsImage.openslideload(str(self.format.path))

        pyramid = Pyramid()
        for level in range(parse_int(openslide.get('openslide.level-count'))):
            prefix = f'openslide.level[{level}].'
            pyramid.insert_tier(
                parse_int(openslide.get(prefix + 'width')),
                parse_int(openslide.get(prefix + 'height')),
                (parse_int(openslide.get(prefix + 'tile-width')),
                 parse_int(openslide.get(prefix + 'tile-height')))
            )

        return pyramid


class NDPIFormat(AbstractFormat):
    """
    Hamamatsu NDPI.

    References
        https://openslide.org/formats/hamamatsu/
        https://docs.openmicroscopy.org/bio-formats/6.5.1/formats/hamamatsu-ndpi.html

    """
    checker_class = NDPIChecker
    parser_class = NDPIParser
    reader_class = OpenslideVipsReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "Hamamatsu NDPI"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

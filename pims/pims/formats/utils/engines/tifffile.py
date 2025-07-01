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
from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import List, Optional, TYPE_CHECKING, Tuple, Union

import numpy as np
from pint import Quantity
from tifffile import TIFF, TiffFile, TiffPage, tifffile

from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils import UNIT_REGISTRY
from pims.utils.types import parse_datetime

if TYPE_CHECKING:
    from pims.files.file import Path

TIFF_FLAGS = (
    'geotiff',
    'philips',
    # 'shaped',
    'lsm',
    'ome',
    'imagej',
    'fluoview',
    'stk',
    'sis',
    'svs',
    'scn',
    'qpi',
    'ndpi',
    'scanimage',
    'mdgel',
    'bif'
)


def read_tifffile(path, silent_fail=True):
    try:
        tf = tifffile.TiffFile(path)
    except tifffile.TiffFileError as error:
        if not silent_fail:
            raise error
        tf = None
    return tf


def cached_tifffile(
    format: Union[AbstractFormat, CachedDataPath]
) -> tifffile.TiffFile:
    return format.get_cached(
        '_tf', read_tifffile, format.path.resolve(), silent_fail=True
    )


def cached_tifffile_baseline(format: AbstractFormat) -> TiffPage:
    tf = cached_tifffile(format)
    return format.get_cached('_tf_baseline', tf.pages.__getitem__, 0)


class TifffileChecker(SignatureChecker):
    @classmethod
    def get_tifffile(cls, pathlike: CachedDataPath):
        return cached_tifffile(pathlike)

    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        buf = cls.get_signature(pathlike)
        if not (len(buf) > 2 and (
                buf[0] == buf[1] == 0x49 or
                buf[0] == buf[1] == 0x4D)):
            return False

        return cls.get_tifffile(pathlike) is not None


class TifffileParser(AbstractParser):
    @property
    def baseline(self) -> TiffPage:
        return cached_tifffile_baseline(self.format)

    def parse_main_metadata(self) -> ImageMetadata:
        baseline = self.baseline

        imd = ImageMetadata()
        imd.width = baseline.imagewidth
        imd.height = baseline.imagelength
        imd.depth = baseline.imagedepth
        imd.duration = 1
        imd.n_concrete_channels = 1

        imd.pixel_type = baseline.dtype
        imd.significant_bits = baseline.bitspersample

        imd.n_samples = baseline.samplesperpixel
        if TIFF.EXTRASAMPLE.UNASSALPHA in baseline.extrasamples:
            imd.n_samples -= 1

        # In the case we have unknown extra samples:
        if imd.n_samples not in (1, 3) and len(baseline.extrasamples) == 0:
            if imd.n_samples > 3:
                imd.n_samples = 3
            else:
                imd.n_samples = 1

        if imd.n_channels == 3:
            imd.set_channel(ImageChannel(index=0, suggested_name='R'))
            imd.set_channel(ImageChannel(index=1, suggested_name='G'))
            imd.set_channel(ImageChannel(index=2, suggested_name='B'))
        else:
            imd.set_channel(ImageChannel(index=0, suggested_name='L'))

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()
        baseline = self.baseline
        tags = baseline.tags

        imd.description = baseline.description
        imd.acquisition_datetime = self.parse_acquisition_date(tags.valueof(306))

        imd.physical_size_x = self.parse_physical_size(
            tags.valueof("XResolution"), tags.valueof("ResolutionUnit")
        )
        imd.physical_size_y = self.parse_physical_size(
            tags.valueof("YResolution"), tags.valueof("ResolutionUnit")
        )
        return imd

    @staticmethod
    def parse_acquisition_date(
        date: Union[datetime, str]
    ) -> Union[datetime, None]:
        """
        Parse a date(time) from a TiffTag to datetime.

        Parameters
        ----------
        date: str, datetime

        Returns
        -------
        datetime: datetime, None
        """

        if isinstance(date, datetime):
            return date
        elif not isinstance(date, str) or (len(date) != 19 or date[16] != ':'):
            return None
        else:
            return parse_datetime(date, raise_exc=False)

    @staticmethod
    def parse_physical_size(
        physical_size: Union[Tuple, float],
        unit: Optional[Union[tifffile.TIFF.RESUNIT, str]] = None
    ) -> Union[Quantity, None]:
        """
        Parse a physical size and its unit from a TiffTag to a Quantity.
        """
        if not unit or physical_size is None:
            return None
        if type(physical_size) == tuple and len(physical_size) == 1:
            rational = (physical_size[0], 1)
        elif type(physical_size) != tuple:
            rational = (physical_size, 1)
        else:
            rational = physical_size
        if rational[0] <= 0 or rational[1] <= 0:
            return None
        if type(unit) is not str:
            unit = unit.name.lower()
        return rational[1] / rational[0] * UNIT_REGISTRY(unit)

    def parse_raw_metadata(self) -> MetadataStore:
        baseline = cached_tifffile_baseline(self.format)
        store = super().parse_raw_metadata()

        # Tags known to be not parsable, unnecessary or hazardous.
        skipped_tags = (273, 279, 278, 288, 289, 320, 324, 325,
                        347, 437, 519, 520, 521, 559, 20624,
                        20625, 34675, 50839) + tuple(range(65420, 65459))

        for tag in baseline.tags:
            if tag.code not in skipped_tags and \
                    type(tag.value) not in (bytes, np.ndarray):
                if isinstance(tag.value, Enum):
                    value = tag.value.name
                else:
                    value = tag.value
                store.set(tag.name, value, namespace="TIFF")
        return store

    def parse_pyramid(self) -> Pyramid:
        image = cached_tifffile(self.format)
        base_series = image.series[0]

        pyramid = Pyramid()
        for level in base_series.levels:
            page = level[0]

            if page.tilewidth != 0:
                tilewidth = page.tilewidth
            else:
                tilewidth = page.imagewidth

            if page.tilelength != 0:
                tilelength = page.tilelength
            else:
                tilelength = page.imagelength

            pyramid.insert_tier(
                page.imagewidth, page.imagelength,
                (tilewidth, tilelength),
                page_index=page.index
            )

        return pyramid


def remove_tiff_comments(
    filepath: Path, n_pages: Optional[int],
    except_pages: Optional[List[int]] = None
):
    if except_pages is None:
        except_pages = []

    with TiffFile(str(filepath), mode='r+b') as tif:
        if n_pages is None:
            n_pages = len(tif.pages)
        for index in range(n_pages):
            if index in except_pages:
                continue
            tag = tif.pages[index].tags.get(270, None)
            if tag is not None:
                tag.overwrite("")

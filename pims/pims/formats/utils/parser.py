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

from abc import ABC, abstractmethod
from typing import List, TYPE_CHECKING

from pims.formats.utils.structures.annotations import ParsedMetadataAnnotation
from pims.formats.utils.structures.metadata import ImageMetadata, MetadataStore
from pims.formats.utils.structures.planes import PlanesInfo
from pims.formats.utils.structures.pyramid import Pyramid

if TYPE_CHECKING:
    from pims.formats import AbstractFormat


class AbstractParser(ABC):
    """
    Base parser. All format parsers must extend this class.
    """

    def __init__(self, format: AbstractFormat):
        self.format = format

    @abstractmethod
    def parse_main_metadata(self) -> ImageMetadata:
        """
        Parse minimal set of required metadata for any PIMS request.
        This method must be as fast as possible.

        Main metadata that must be parsed by this method are:
        * width
        * height
        * depth
        * duration
        * n_concrete_channels
        * n_samples
        * n_distinct_channels
        * pixel_type
        * significant_bits
        * for every channel:
            * index
            * color (can be None)
            * suggested_name (can be None, used to infer color)

        It is allowed to parse more metadata in this method if it does not
        introduce overhead.
        """
        pass

    @abstractmethod
    def parse_known_metadata(self) -> ImageMetadata:
        """
        Parse all known standardised metadata. In practice, this method
        completes the image metadata object partially filled by
        `parse_main_metadata`.

        This method should set `imd.is_complete` to True before returning `imd`.
        """
        return self.format.main_imd

    @abstractmethod
    def parse_raw_metadata(self) -> MetadataStore:
        """
        Parse all raw metadata in a generic store. Raw metadata are not
        standardised and highly depend on underlying parsed format.

        Raw metadata MUST NOT be used by PIMS for processing.
        This method is expected to be SLOW.
        """
        return MetadataStore()

    def parse_pyramid(self) -> Pyramid:
        """
        Parse pyramid (and tiers) from format metadata. In all cases, the
        pyramid must have at least one tier (i.e. the image at full resolution).

        Arbitrary information useful for readers can be stored for each tier
        (e.g.: a TIFF page index).

        This method must be as fast as possible.
        """
        imd = self.format.main_imd
        p = Pyramid()
        p.insert_tier(imd.width, imd.height, (imd.width, imd.height))
        return p

    def parse_planes(self) -> PlanesInfo:
        """
        Parse plane information from format metadata. In all cases, there is
        at least one plane (0, 0, 0).

        Arbitrary information useful for readers can be stored for each plane
        (e.g.: a TIFF page index).

        This method must be as fast as possible.
        """
        imd = self.format.main_imd
        pi = PlanesInfo(imd.n_channels, imd.depth, imd.duration)
        return pi

    def parse_annotations(self) -> List[ParsedMetadataAnnotation]:
        """
        Parse annotations stored in image format metadata, together with
        optional terms and properties.
        """
        return []

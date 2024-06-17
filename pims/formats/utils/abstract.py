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

import logging
import re
from abc import ABC
from copy import deepcopy
from typing import Any, Dict, List, Optional, TYPE_CHECKING, Type

from pims.cache import SimpleDataCache, cached_property
from pims.formats.utils.checker import AbstractChecker
from pims.formats.utils.convertor import AbstractConvertor
from pims.formats.utils.histogram import AbstractHistogramReader
from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.reader import AbstractReader
from pims.formats.utils.structures.annotations import ParsedMetadataAnnotation
from pims.formats.utils.structures.metadata import ImageMetadata, MetadataStore
from pims.formats.utils.structures.planes import PlanesInfo
from pims.formats.utils.structures.pyramid import Pyramid, normalized_pyramid

if TYPE_CHECKING:
    from pims.files.file import Path

log = logging.getLogger("pims.formats")

_CAMEL_TO_SPACE_PATTERN = re.compile(r'((?<=[a-z])[A-Z]|(?<!\A)[A-Z](?=[a-z]))')


class CachedDataPath(SimpleDataCache):
    """
    A cache associated to a path.

    Technical details: It would be more meaningful to have `CachedDataPath` inheriting
    from `SimpleDataCache` and `Path` as Python allows multiple inheritance. Other
    meaningful implementation could be to have `CachedDataPath` that extends `Path` and
    have an attribute `cache`. However, both solutions are impossible because they
    cause circular imports.
    """
    def __init__(self, path: Path):
        super().__init__()
        self.path = path


class AbstractFormat(ABC, SimpleDataCache):
    """
    Base format. All image formats must extend this class.
    """
    checker_class: Type[AbstractChecker] = None
    parser_class: Type[AbstractParser] = None
    reader_class: Type[AbstractReader] = None
    convertor_class: Type[AbstractConvertor] = None

    histogram_reader_class: Type[AbstractHistogramReader] = None

    def __init__(self, path: Path, existing_cache: Dict[str, Any] = None):
        """
        Initialize an image in this format. It does nothing until some
        parsing or reading methods are called.

        Parameters
        ----------
        path
            The image path
        existing_cache
            A cache of data related to the image that have been previously
            computed and that could be used again in the future.
            In practice, it is used to collect data computed during matching
            (format identification) that can be used again in parser or reader.
        """
        self._path = path
        super(AbstractFormat, self).__init__(existing_cache)

        self._enabled = False

        self.parser = self.parser_class(self)
        self.reader = self.reader_class(self) if self.reader_class else None
        self.convertor = self.convertor_class(self) if self.convertor_class else None

        self.histogram_reader = self.histogram_reader_class(self)

    @classmethod
    def init(cls):
        """
        Initialize the format, such that all third-party libs are ready.
        """
        pass

    @classmethod
    def _get_identifier(cls):
        """
        Get the format identifier. It must be unique across all formats.
        """
        return cls.__name__.replace('Format', '')

    @classmethod
    def get_identifier(cls, uppercase: bool = True) -> str:
        """
        Get the format identifier. It must be unique across all formats.

        Parameters
        ----------
        uppercase: bool
            If the format must be returned in uppercase.
            In practice, comparisons are always done using the uppercase identifier

        Returns
        -------
        identifier: str
            The format identifier
        """
        identifier = cls._get_identifier()
        if uppercase:
            return identifier.upper()
        return identifier

    @classmethod
    def get_name(cls) -> str:
        """Get the format name in a human-readable way."""
        return re.sub(_CAMEL_TO_SPACE_PATTERN, r' \1', cls.get_identifier(False))

    @classmethod
    def get_remarks(cls) -> str:
        """Get format remarks in a human-readable way."""
        return str()

    @classmethod
    def get_plugin_name(cls) -> str:
        """Get PIMS format plugin name adding this format."""
        return '.'.join(cls.__module__.split('.')[:-1])

    @classmethod
    def is_readable(cls) -> bool:
        """Whether PIMS can read images in this format."""
        return cls.reader_class is not None

    @classmethod
    def is_writable(cls):  # TODO
        return False

    @classmethod
    def is_convertible(cls) -> bool:
        """Whether PIMS can convert images in this format into another one."""
        return cls.convertor_class is not None

    @classmethod
    def is_importable(cls) -> bool:
        """Whether PIMS allows to import images in this format."""
        return True

    @classmethod
    def is_spatial(cls) -> bool:
        """Whether this format is adapted for spatial data requests."""
        return False

    @classmethod
    def is_spectral(cls) -> bool:
        """Whether this format is adapted for spectral data requests."""
        return False

    @classmethod
    def match(cls, cached_path: CachedDataPath) -> bool:
        """
        Identify if it is this format or not.

        Parameters
        ----------
        cached_path : CachedDataPath
            The path, proxied with some useful results across formats.

        Returns
        -------
        match: boolean
            Whether it is this format
        """
        if cls.checker_class:
            return cls.checker_class.match(cached_path)
        return False

    @classmethod
    def from_proxy(cls, cached_path: CachedDataPath) -> AbstractFormat:
        return cls(path=cached_path.path, existing_cache=cached_path.cache)

    @classmethod
    def from_path(cls, path: Path) -> AbstractFormat:
        return cls(path=path)

    @property
    def enabled(self):
        return self._enabled

    @enabled.setter
    def enabled(self, value):
        self._enabled = value

    @property
    def path(self) -> Path:
        return self._path

    @property
    def media_type(self) -> str:
        return "application/octet-stream"

    # Conversion

    @cached_property
    def need_conversion(self) -> bool:
        """
        Whether the image in this format needs to be converted to another one.
        Decision can be made based on the format metadata.
        """
        return True

    def conversion_format(self) -> Optional[Type[AbstractFormat]]:
        """
        Get the format to which the image in this format will be converted,
        if needed.
        """
        if self.convertor:
            return self.convertor.conversion_format()
        else:
            return None

    def convert(self, dest_path: Path) -> bool:
        """
        Convert the image in this format to another one at a given destination
        path.

        Returns
        -------
        result
            Whether the conversion succeeded or not
        """
        if self.convertor:
            return self.convertor.convert(dest_path)
        else:
            raise NotImplementedError()

    # Metadata parsing

    @cached_property
    def main_imd(self) -> ImageMetadata:
        """
        Get main image metadata, that is, required metadata to process
        any request.

        It is possible that other non-required metadata have been populated.
        """
        return self.parser.parse_main_metadata()

    @cached_property
    def full_imd(self) -> ImageMetadata:
        """
        Get full image metadata, that is, all known and standardised metadata.
        `self.full_imd.is_complete` should be true.
        """
        return self.parser.parse_known_metadata()

    @cached_property
    def raw_metadata(self) -> MetadataStore:
        """
        Get all raw metadata in a generic store. Raw metadata are not
        standardised and highly depend on underlying parsed format.

        Raw metadata MUST NOT be used by PIMS for processing.
        """
        return self.parser.parse_raw_metadata()

    @cached_property
    def pyramid(self) -> Pyramid:
        """
        Get image format pyramid. There is always at least one tier (the
        pyramid basis).
        """
        return self.parser.parse_pyramid()

    @cached_property
    def normalized_pyramid(self) -> Pyramid:
        return normalized_pyramid(self.main_imd.width, self.main_imd.height)

    @cached_property
    def is_pyramid_normalized(self) -> bool:
        return self.pyramid == self.normalized_pyramid

    @cached_property
    def planes_info(self) -> PlanesInfo:
        """
        Information about each plane.
        """
        return self.parser.parse_planes()

    @cached_property
    def annotations(self) -> List[ParsedMetadataAnnotation]:
        """
        Get annotations stored in image format metadata.
        """
        return self.parser.parse_annotations()

    @cached_property
    def histogram(self):
        return self.histogram_reader

    @cached_property
    def main_path(self):
        return self.path

    def serialize(self):
        _ = self.main_imd
        _ = self.full_imd
        _ = self.pyramid
        _ = self.normalized_pyramid
        _ = self.is_pyramid_normalized
        _ = self.planes_info
        return deepcopy(self)
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

from datetime import date, datetime, time
from enum import Enum
from typing import AbstractSet, Any, List, Optional, Sequence, Tuple, ValuesView

import numpy as np
from pint import Quantity

from pims.utils.color import Color, infer_channel_color
from pims.utils.dict import flatten


class MetadataType(Enum):
    """
    Types for metadata.
    MetadataType names come from API specification.
    """

    def __init__(self, python_type=str):
        self.python_type = python_type

    BOOLEAN = bool
    INTEGER = int
    DECIMAL = float
    JSON = dict
    LIST = list
    DATE = date
    TIME = time
    DATETIME = datetime
    STRING = str
    UNKNOWN = type(None)


class Metadata:
    """
    A metadata from a file (e.g. an image).
    """

    def __init__(self, key: str, value: Any, namespace: str = ""):
        """
        Initialize a metadata.

        Parameters
        ----------
        key: str
            The name of the metadata
        value: any
            The value of the metadata
        namespace: str
            The namespace of the key-value pair.

        All attributes are read-only.
        """
        self._key = key
        self._value = value
        self._namespace = namespace.upper()
        self._metadata_type = self.infer_metadata_type()

    @property
    def value(self) -> Any:
        return self._value

    @property
    def key(self) -> str:
        return self._key

    @property
    def namespace(self) -> str:
        return self._namespace

    @property
    def namespaced_key(self) -> str:
        return f"{self.namespace}.{self.key}" if self.namespace else self.key

    @property
    def metadata_type(self) -> MetadataType:
        return self._metadata_type

    def infer_metadata_type(self) -> MetadataType:
        """
        Try to infer the metadata type from the metadata value.
        """
        for mt in MetadataType:
            if type(self._value) == mt.python_type:
                return mt
        return MetadataType.UNKNOWN

    def __eq__(self, o: object) -> bool:
        return isinstance(o, Metadata) and self.key == o.key \
               and self.value == o.value \
               and self.namespace == o.namespace

    def __str__(self) -> str:
        return f"{self.namespaced_key}={str(self.value)} ({self.metadata_type.name})"

    def __repr__(self) -> str:
        return f"{self.namespaced_key}={str(self.value)} ({self.metadata_type.name})"


class MetadataStore:
    """
    A set of metadata stores, extracted from a file (e.g. an image).
    Nested dict like interface.
    1st level dict represents namespaced stores.
    2nd level dicts are metadata dictionaries for each namespace.
    """

    def __init__(self):
        self._namedstores = dict()

    @staticmethod
    def _split_namespaced_key(namespaced_key: str) -> Tuple[str, str]:
        """Split namespace and the rest from a key"""
        split = namespaced_key.split('.', 1)
        return ("", namespaced_key) if len(split) < 2 else split

    def set(self, namespaced_key: str, value: Any, namespace: Optional[str] = None) -> None:
        """
        Set a metadata in the store.

        Parameters
        ----------
        namespaced_key: str
            The name of the metadata, starting with its namespace. Namespace and key are
            dot-separated.
        value: any
            The value of the metadata
        namespace: str, optional
            If given, prepend the namespaced_key with this namespace
        """
        if namespace:
            namespaced_key = f"{namespace}.{namespaced_key}"
        namespace, key = self._split_namespaced_key(namespaced_key)
        metadata = Metadata(key, value, namespace)
        store = self._namedstores.get(metadata.namespace, dict())
        store[key] = metadata
        self._namedstores[metadata.namespace] = store

    def get_namedstore(self, namespace: str, default=None) -> dict:
        """Get store for given namespace"""
        return self._namedstores.get(namespace.upper(), default)

    def get(self, namespaced_key: str, default: Any = None) -> Any:
        """Get metadata for a given (namespaced) key"""
        namespace, key = self._split_namespaced_key(namespaced_key)
        store = self.get_namedstore(namespace)
        if store:
            return store.get(key, default)
        return default

    def get_value(self, namespaced_key: str, default: Any = None) -> Any:
        """Get metadata value for a given (namespaced) key"""
        metadata = self.get(namespaced_key, None)
        if metadata:
            return metadata.value
        return default

    def get_first_value(self, namespaced_keys: Sequence[str], default: Any = None) -> Any:
        """Get the first non-null metadata value in the list of metadata keys"""
        for namespaced_key in namespaced_keys:
            metadata = self.get(namespaced_key, None)
            if metadata is not None:
                return metadata.value
        return default

    def get_metadata_type(self, namespaced_key: str, default: Any = None) -> Any:
        """Get metadata type for a given (namespaced) key"""
        metadata = self.get(namespaced_key, None)
        if metadata:
            return metadata.metadata_type
        return default

    @staticmethod
    def _flatten(d, parent_key='', sep='.'):
        return flatten(d, parent_key, sep)

    def flatten(self):
        return self._flatten(self._namedstores)

    def items(self) -> AbstractSet[Tuple[str, Metadata]]:
        return self.flatten().items()

    def keys(self) -> AbstractSet[str]:
        return self.flatten().keys()

    def values(self) -> ValuesView[Metadata]:
        return self.flatten().values()

    def __contains__(self, o: object) -> bool:
        if type(o) == Metadata:
            return self.get(o.namespaced_key) is not None
        return o in self._namedstores

    def __len__(self) -> int:
        return len(self._namedstores)

    def __iter__(self):
        return iter(self._namedstores)

    def __str__(self) -> str:
        return str(self._namedstores)

    def __repr__(self) -> str:
        return repr(self._namedstores)


class _MetadataStorable:
    """An interface to convert a class to a metadata store"""
    def metadata_namespace(self) -> str:
        return ""

    def to_metadata_store(self, store: MetadataStore) -> MetadataStore:
        """
        Insert this object content into a metadata store.
        Object variables starting with `_` are ignored.
        """
        keys = ()
        if hasattr(self, '__slots__'):
            keys += self.__slots__
        if hasattr(self, '__dict__'):
            keys += tuple(self.__dict__.keys())

        for key in keys:
            if not key.startswith("_"):
                value = getattr(self, key)
                if isinstance(value, list):
                    for item in value:
                        if issubclass(type(item), _MetadataStorable):
                            item.to_metadata_store(store)
                elif issubclass(type(value), _MetadataStorable):
                    value.to_metadata_store(store)
                elif value is not None:
                    store.set(
                        key, value,
                        namespace=self.metadata_namespace()
                    )
        return store


class ImageChannel(_MetadataStorable):
    emission_wavelength: Optional[Quantity]
    excitation_wavelength: Optional[Quantity]
    index: int
    suggested_name: Optional[str]

    __slots__ = (
        'emission_wavelength', 'excitation_wavelength', 'index',
        'suggested_name', '_color'
    )

    def __init__(
        self, index: int, emission_wavelength: Optional[float] = None,
        excitation_wavelength: Optional[float] = None, suggested_name: Optional[str] = None,
        color: Optional[Color] = None
    ):
        self.emission_wavelength = emission_wavelength
        self.excitation_wavelength = excitation_wavelength
        self.index = index
        self.suggested_name = suggested_name
        self._color = color

    def _infer_color(self) -> Optional[Color]:
        """Try to infer channel color. If found, store color in `_color`."""
        c = infer_channel_color(self.suggested_name, self.index, channel_color_list=[])
        if c:
            self._color = c
        return c

    @property
    def color(self) -> Optional[Color]:
        if self._color:
            return self._color
        return self._infer_color()

    @color.setter
    def color(self, value: Color):
        self._color = value

    @property
    def hex_color(self) -> str:
        c = self.color
        return c.as_hex() if c else None

    def metadata_namespace(self) -> str:
        return f"channel[{self.index}]"


class ImageObjective(_MetadataStorable):
    nominal_magnification: Optional[float]
    calibrated_magnification: Optional[float]

    __slots__ = ('nominal_magnification', 'calibrated_magnification')

    def __init__(self):
        self.nominal_magnification = None
        self.calibrated_magnification = None

    def metadata_namespace(self) -> str:
        return "objective"


class ImageMicroscope(_MetadataStorable):
    model: Optional[str]

    __slots__ = ('model',)

    def __init__(self):
        self.model = None

    def metadata_namespace(self) -> str:
        return "microscope"


class ImageAssociated(_MetadataStorable):
    n_channels: Optional[int]
    height: Optional[int]
    width: Optional[int]

    __slots__ = ('n_channels', 'height', 'width', '_kind')

    def __init__(self, kind: str):
        self.width = None
        self.height = None
        self.n_channels = None
        self._kind = kind

    @property
    def exists(self) -> bool:
        return self.width is not None and \
               self.height is not None and \
               self.n_channels is not None

    def metadata_namespace(self) -> str:
        return f"associated.{self._kind}"


class ImageMetadata(_MetadataStorable):
    """Wrapper on parsed image metadata"""
    width: int
    height: int
    depth: int
    duration: int
    n_samples: int
    n_concrete_channels: int
    n_distinct_channels: int
    pixel_type: np.dtype
    significant_bits: int
    physical_size_x: Optional[Quantity]
    physical_size_y: Optional[Quantity]
    physical_size_z: Optional[Quantity]
    frame_rate: Optional[Quantity]
    acquisition_datetime: Optional[datetime]
    description: Optional[str]
    channels: List[ImageChannel]
    objective: ImageObjective
    microscope: ImageMicroscope
    associated_thumb: ImageAssociated
    associated_label: ImageAssociated
    associated_macro: ImageAssociated

    __slots__ = (
        'width', 'height', 'duration', 'n_concrete_channels',
        'n_samples', 'n_distinct_channels', 'pixel_type',
        'significant_bits', 'physical_size_x', 'physical_size_y',
        'physical_size_z', 'frame_rate', 'acquisition_datetime',
        'description', 'channels', 'objective', 'microscope',
        'associated_thumb', 'associated_label', 'associated_macro'
    )

    def __init__(self):
        self._is_complete = False

        self.width = 1
        self.height = 1
        self.depth = 1
        self.duration = 1

        self.n_samples = 1
        self.n_distinct_channels = 1

        self.pixel_type = np.dtype('uint8')
        self.significant_bits = 8

        self.physical_size_x = None
        self.physical_size_y = None
        self.physical_size_z = None
        self.frame_rate = None

        self.acquisition_datetime = None
        self.description = None

        self.channels = list()
        self.objective = ImageObjective()
        self.microscope = ImageMicroscope()
        self.associated_thumb = ImageAssociated('thumb')
        self.associated_label = ImageAssociated('label')
        self.associated_macro = ImageAssociated('macro')

    def set_channel(self, channel):
        self.channels.insert(channel.index, channel)

    def metadata_namespace(self) -> str:
        return "image"

    @property
    def is_complete(self) -> bool:
        """Whether the parser tried to fill all metadata or not"""
        return self._is_complete

    @is_complete.setter
    def is_complete(self, value: bool):
        self._is_complete = value

    @property
    def n_channels(self) -> int:
        return self.n_samples * self.n_concrete_channels

    @property
    def n_planes(self) -> int:
        return self.n_concrete_channels * self.depth * self.duration

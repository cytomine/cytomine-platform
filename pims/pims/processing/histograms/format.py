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

from abc import ABC
from typing import List, Tuple, Union, TYPE_CHECKING

import numpy as np
import zarr as zarr
from zarr.errors import _BaseZarrError as ZarrError  # noqa

from pims.api.utils.models import HistogramType
from pims.cache import cached_property
from pims.formats.utils.histogram import HistogramReaderInterface, PlaneIndex

if TYPE_CHECKING:
    from pims.files.file import Path

ZHF_PER_PLANE = "plane"
ZHF_PER_CHANNEL = "channel"
ZHF_PER_IMAGE = "image"
ZHF_HIST = "hist"
ZHF_BOUNDS = "bounds"
ZHF_ATTR_TYPE = "histogram_type"
ZHF_ATTR_FORMAT = "histogram_format"


class HistogramFormat(HistogramReaderInterface, ABC):
    def __init__(self, path: Path, **kwargs):
        self.path = path

    @classmethod
    def match(cls, path: Path, *args, **kwargs) -> Union[bool, HistogramFormat]:
        return False


class ZarrHistogramFormat(HistogramFormat):
    def __init__(self, path: Path, **kwargs):
        super().__init__(path)
        self.__dict__.update(kwargs)

        if not hasattr(self, 'zhf'):
            self.zhf = zarr.open(str(self.path), mode='r')

    @classmethod
    def match(cls, path: Path, *args, **kwargs) -> Union[bool, HistogramFormat]:
        try:
            zhf = zarr.open(str(path), mode='r')
            if ZHF_ATTR_FORMAT in zhf.attrs:
                return cls(path, zhf=zhf)
        except ZarrError:
            return False

    @cached_property
    def per_planes(self) -> bool:
        return ZHF_PER_PLANE in self.zhf

    @cached_property
    def per_channels(self) -> bool:
        return ZHF_PER_CHANNEL in self.zhf

    @cached_property
    def per_image(self) -> bool:
        return ZHF_PER_IMAGE in self.zhf

    def type(self) -> HistogramType:
        return self.zhf.attrs[ZHF_ATTR_TYPE]

    def image_bounds(self) -> Tuple[int, int]:
        return tuple(self.zhf[f"{ZHF_PER_IMAGE}/{ZHF_BOUNDS}"])

    def image_histogram(self, squeeze: bool = True) -> np.ndarray:
        hist = self.zhf[f"{ZHF_PER_IMAGE}/{ZHF_HIST}"][:]
        if not squeeze:
            hist = hist[np.newaxis, :]
        return hist

    def channels_bounds(self) -> List[Tuple[int, int]]:
        if not self.per_channels:
            return [self.image_bounds()]
        return list(map(tuple, self.zhf[f"{ZHF_PER_CHANNEL}/{ZHF_BOUNDS}"]))

    def channel_bounds(self, c: int) -> Tuple[int, int]:
        if not self.per_channels:
            return self.image_bounds()
        return tuple(self.zhf[f"{ZHF_PER_CHANNEL}/{ZHF_BOUNDS}"][c])

    def channel_histogram(
        self, c: PlaneIndex, squeeze: bool = True
    ) -> np.ndarray:
        if not self.per_channels:
            return self.image_histogram()

        if type(c) is list:
            hist = self.zhf[f"{ZHF_PER_CHANNEL}/{ZHF_HIST}"]\
                .get_orthogonal_selection((c,))
            return np.squeeze(hist) if squeeze else hist

        hist = self.zhf[f"{ZHF_PER_CHANNEL}/{ZHF_HIST}"][c]
        return hist[np.newaxis, :] if not squeeze else hist

    def planes_bounds(self) -> List[Tuple[int, int]]:
        if not self.per_planes:
            return self.channels_bounds()
        return list(map(
            tuple, self.zhf[f"{ZHF_PER_PLANE}/{ZHF_BOUNDS}"].reshape((-1, 2))
        ))

    def plane_bounds(self, c: int, z: int, t: int) -> Tuple[int, int]:
        if not self.per_planes:
            return self.channel_bounds(c)
        return tuple(self.zhf[f"{ZHF_PER_PLANE}/{ZHF_BOUNDS}"][t, z, c])

    def plane_histogram(
        self, c: PlaneIndex, z: PlaneIndex, t: PlaneIndex, squeeze: bool = True
    ) -> np.ndarray:
        if not self.per_planes:
            return self.channel_histogram(c)

        if type(c) is list or type(z) is list or type(t) is list:
            c = c if type(c) is list else [c]
            z = z if type(z) is list else [z]
            t = t if type(t) is list else [t]
            hist = self.zhf[f"{ZHF_PER_PLANE}/{ZHF_HIST}"]\
                .get_orthogonal_selection((t, z, c))
            return np.squeeze(hist) if squeeze else hist

        hist = self.zhf[f"{ZHF_PER_PLANE}/{ZHF_HIST}"][t, z, c]
        return hist[np.newaxis, np.newaxis, np.newaxis, :] if not squeeze else hist

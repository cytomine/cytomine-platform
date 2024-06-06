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
from abc import ABC, abstractmethod
from typing import List, TYPE_CHECKING, Tuple, Union

import numpy as np

from pims.api.exceptions import BadRequestException
from pims.api.utils.models import HistogramType

log = logging.getLogger("pims.formats")

if TYPE_CHECKING:
    from pims.formats import AbstractFormat

PlaneIndex = Union[int, List[int]]


class HistogramReaderInterface(ABC):

    @abstractmethod
    def type(self) -> HistogramType:
        pass

    @abstractmethod
    def image_bounds(self) -> Tuple[int, int]:
        """
        Intensity bounds on the whole image (all planes merged).

        Returns
        -------
        mini : int
            The lowest intensity in all image planes
        maxi : int
            The greatest intensity in all image planes
        """
        pass

    @abstractmethod
    def image_histogram(self, squeeze: bool = True) -> np.ndarray:
        """
        Intensity histogram on the whole image (all planes merged)

        Parameters
        ----------
        squeeze

        Returns
        -------
        histogram : np.ndarray of shape:
         * If `squeeze=True`: `(2**image.bitdepth,)`
         * Otherwise: `(1, 2**image.bitdepth)`
        """
        pass

    @abstractmethod
    def channels_bounds(self) -> List[Tuple[int, int]]:
        """
        Intensity bounds for every channels

        Returns
        -------
        channels_bounds : list of tuple (int, int)
        """
        pass

    @abstractmethod
    def channel_bounds(self, c: int) -> Tuple[int, int]:
        """
        Intensity bounds for a channel.

        Parameters
        ----------
        c : int
            The image channel index. Index is expected to be valid.

        Returns
        -------
        mini : int
            The lowest intensity for that channel in all image (Z, T) planes
        maxi : int
            The greatest intensity for that channel in all image (Z, T) planes
        """
        pass

    @abstractmethod
    def channel_histogram(self, c: PlaneIndex, squeeze: bool = True) -> np.ndarray:
        """
        Intensity histogram(s) for one of several channel(s)

        Parameters
        ----------
        squeeze
        c
            The image channel index(es)

        Returns
        -------
        histogram : np.ndarray of shape:
         * If `squeeze=True` and `type(c) is int`: `(2**image.bitdepth,)`
         * Otherwise: `(len(c), 2**image.bitdepth)`
        """
        pass

    @abstractmethod
    def planes_bounds(self) -> List[Tuple[int, int]]:
        """
        Intensity bounds for every planes

        Returns
        -------
        planes_bounds : list of tuple (int, int)
        """
        pass

    @abstractmethod
    def plane_bounds(self, c: int, z: int, t: int) -> Tuple[int, int]:
        """
        Intensity bounds for a plane

        Parameters
        ----------
        c : int
            The image channel index
        z : int
            The focal plane index
        t : int
            The timepoint index

        Returns
        -------
        mini : int
            The lowest intensity for that plane
        maxi : int
            The greatest intensity for that plane
        """
        pass

    @abstractmethod
    def plane_histogram(
        self, c: PlaneIndex, z: PlaneIndex, t: PlaneIndex, squeeze=True
    ) -> np.ndarray:
        """
        Intensity histogram(s) for one or several plane(s).

        Returns
        -------
        Shape: (len(t), len(z), len(c), 2**image.bitdepth). If `squeeze` is
        True, plane dimensions of length 1 are removed.
        """
        pass


class AbstractHistogramReader(HistogramReaderInterface, ABC):
    """
    Base histogram reader. All histogram readers must extend this class.
    """
    def __init__(self, format: AbstractFormat):
        self.format = format


class DefaultHistogramReader(AbstractHistogramReader):
    def type(self) -> HistogramType:
        return HistogramType.FAST

    def image_bounds(self) -> Tuple[int, int]:
        log.warning(
            f"Impossible {self.format.path} to compute "
            f"image histogram bounds. Default values used."
        )
        return 0, 2 ** self.format.main_imd.significant_bits

    def image_histogram(self, squeeze=True):
        raise BadRequestException(
            detail=f"No histogram found for {self.format.path}"
        )

    def channels_bounds(self) -> List[Tuple[int, int]]:
        log.warning(
            f"Impossible {self.format.path} to compute "
            f"channels histogram bounds. Default values used."
        )
        return [(0, 2 ** self.format.main_imd.significant_bits)] \
               * self.format.main_imd.n_channels

    def channel_bounds(self, c: int) -> Tuple[int, int]:
        log.warning(
            f"Impossible {self.format.path} to compute "
            f"channel histogram bounds. Default values used."
        )
        return 0, 2 ** self.format.main_imd.significant_bits

    def channel_histogram(self, c, squeeze=True):
        raise BadRequestException(
            detail=f"No histogram found for {self.format.path}"
        )

    def planes_bounds(self) -> List[Tuple[int, int]]:
        log.warning(
            f"Impossible {self.format.path} to compute "
            f"plane histogram bounds. Default values used."
        )
        return [(0, 2 ** self.format.main_imd.significant_bits)] \
               * self.format.main_imd.n_planes

    def plane_bounds(self, c: int, z: int, t: int) -> Tuple[int, int]:
        log.warning(
            f"Impossible {self.format.path} to compute "
            f"plane histogram bounds. Default values used."
        )
        return 0, 2 ** self.format.main_imd.significant_bits

    def plane_histogram(self, c, z, t, squeeze=True):
        raise BadRequestException(
            detail=f"No histogram found for {self.format.path}"
        )

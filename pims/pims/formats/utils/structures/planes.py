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
from typing import Any, List, Optional

import numpy as np


class PlanesInfo:
    """
    Efficient wrapper to store arbitrary information about planes.
    """

    def __init__(
        self, n_channels: int, depth: int, duration: int,
        keys: Optional[List[str]] = None,
        value_datatypes: Optional[List] = None
    ):
        """
        Initializer.

        Parameters
        ----------
        n_channels
            Number of channels
        depth
            Number of z-slices
        duration
            Number of timepoints
        keys
            Key names of values to store for every plane.
            Must have same length as `value_formats`
        value_datatypes
            Numpy datatype of values to store for every plane.
            Must have same length as `keys`.
        """
        self.n_channels = n_channels
        self.depth = depth
        self.duration = duration

        keys = keys if keys else []
        value_datatypes = value_datatypes if value_datatypes else []
        self._keys = keys
        self._data = self._init_data(keys, value_datatypes)

    def _init_data(self, keys: List[str], formats: List[np.dtype]):
        return np.zeros(
            (self.n_channels, self.depth, self.duration),
            dtype={'names': keys, 'formats': formats}
        )

    @property
    def n_planes(self) -> int:
        return self.n_channels * self.depth * self.duration

    def set(self, c, z, t, **infos):
        """
        Set information for a given plane.

        Parameters
        ----------
        c
            The channel index
        z
            The z-slice index
        t
            The timepoint index
        infos
            A dictionary of information to store.
            Keys must have been defined in initializer, otherwise they are
            ignored. Values must coerce the defined corresponding datatype.
        """
        plane_info = self._data[c, z, t]
        for k, v in infos.items():
            if k in self._keys:
                plane_info[k] = v

    def get(self, c, z, t, key: str, default: Any = None) -> Any:
        if key not in self._keys:
            return default
        return self._data[c, z, t][key]

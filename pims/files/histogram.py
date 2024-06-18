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
from typing import List, Tuple, Type

import numpy as np

from pims.api.exceptions import NoMatchingFormatProblem
from pims.api.utils.models import HistogramType
from pims.files.file import Path
from pims.formats.utils.histogram import HistogramReaderInterface, PlaneIndex
from pims.processing.histograms import HISTOGRAM_FORMATS
from pims.processing.histograms.format import HistogramFormat


class Histogram(Path, HistogramReaderInterface):
    def __init__(self, *pathsegments, format: Type[HistogramFormat] = None):
        super().__init__(*pathsegments)

        _format = None
        if format:
            _format = format(Path(self))
        else:
            for possible_format in HISTOGRAM_FORMATS:
                _format = possible_format.match(Path(self))
                if _format is not None:
                    break

        if _format is None:
            raise NoMatchingFormatProblem(Path(self))
        else:
            self._format = _format

    def type(self) -> HistogramType:
        return self._format.type()

    def image_bounds(self) -> Tuple[int, int]:
        """Intensity bounds on the whole image (all planes merged)."""
        return self._format.image_bounds()

    def image_histogram(self, squeeze: bool = True) -> np.ndarray:
        """Intensity histogram on the whole image (all planes merged)."""
        return self._format.image_histogram()

    def channels_bounds(self) -> List[Tuple[int, int]]:
        """Intensity bounds for every channels."""
        return self._format.channels_bounds()

    def channel_bounds(self, c: int) -> Tuple[int, int]:
        """Intensity bounds for a channel."""
        return self._format.channel_bounds(c)

    def channel_histogram(self, c: PlaneIndex, squeeze: bool = True) -> np.ndarray:
        """Intensity histogram(s) for one of several channel(s)"""
        return self._format.channel_histogram(c)

    def planes_bounds(self) -> List[Tuple[int, int]]:
        """Intensity bounds for every planes."""
        return self._format.planes_bounds()

    def plane_bounds(self, c: int, z: int, t: int) -> Tuple[int, int]:
        """Intensity bounds for a plane."""
        return self._format.plane_bounds(c, z, t)

    def plane_histogram(
        self, c: PlaneIndex, z: PlaneIndex, t: PlaneIndex, squeeze: bool = True
    ) -> np.ndarray:
        """Intensity histogram(s) for one or several plane(s)."""
        return self._format.plane_histogram(c, z, t)

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

import pytest

from pims.api.exceptions import ColormapNotFoundProblem
from pims.api.utils.models import ColormapEnum, IntensitySelectionEnum
from pims.api.utils.processing_parameter import parse_colormap_id, parse_intensity_bounds
from pims.processing.colormaps import ALL_COLORMAPS
from pims.utils.color import Color


def test_parse_intensity_bounds():
    class FakeImage:
        def __init__(self, significant_bits, n_channels):
            self.n_channels = n_channels
            self.significant_bits = significant_bits

        def channel_bounds(self, channel):
            return [channel, channel + 10]

    assert parse_intensity_bounds(FakeImage(8, 1), [0], [0], [0], [], []) == ([0], [255])
    assert parse_intensity_bounds(
        FakeImage(8, 1), [0], [0], [0], [IntensitySelectionEnum.AUTO_IMAGE],
        [IntensitySelectionEnum.AUTO_IMAGE]
    ) == ([0], [255])
    assert parse_intensity_bounds(
        FakeImage(8, 1), [0], [0], [0], [IntensitySelectionEnum.STRETCH_IMAGE],
        [IntensitySelectionEnum.STRETCH_IMAGE]
    ) == ([0], [10])
    assert parse_intensity_bounds(FakeImage(8, 1), [0], [0], [0], [10], [100]) == ([10], [100])
    assert parse_intensity_bounds(FakeImage(8, 1), [0], [0], [0], [10], [1000]) == ([10], [255])

    assert parse_intensity_bounds(FakeImage(16, 1), [0], [0], [0], [], []) == ([0], [65535])
    assert parse_intensity_bounds(
        FakeImage(16, 1), [0], [0], [0], [IntensitySelectionEnum.AUTO_IMAGE],
        [IntensitySelectionEnum.AUTO_IMAGE]
    ) == ([0], [10])
    assert parse_intensity_bounds(
        FakeImage(16, 1), [0], [0], [0], [IntensitySelectionEnum.STRETCH_IMAGE],
        [IntensitySelectionEnum.STRETCH_IMAGE]
    ) == ([0], [10])
    assert parse_intensity_bounds(FakeImage(16, 1), [0], [0], [0], [10], [100]) == ([10], [100])
    assert parse_intensity_bounds(FakeImage(16, 1), [0], [0], [0], [10], [1000]) == ([10], [1000])
    assert parse_intensity_bounds(
        FakeImage(16, 1), [0], [0], [0], [10], [100000]
    ) == ([10], [65535])

    assert parse_intensity_bounds(
        FakeImage(8, 2), [0, 1], [0], [0], [IntensitySelectionEnum.AUTO_IMAGE],
        [IntensitySelectionEnum.AUTO_IMAGE]
    ) == ([0, 0], [255, 255])
    assert parse_intensity_bounds(
        FakeImage(8, 2), [0, 1], [0], [0], [IntensitySelectionEnum.STRETCH_IMAGE],
        [IntensitySelectionEnum.STRETCH_IMAGE]
    ) == ([0, 1], [10, 11])
    assert parse_intensity_bounds(
        FakeImage(8, 2), [0, 1], [0], [0], [10], [100]
    ) == ([10, 10], [100, 100])
    assert parse_intensity_bounds(
        FakeImage(8, 2), [0, 1], [0], [0], [10], [1000, 20]
    ) == ([10, 10], [255, 20])

    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [IntensitySelectionEnum.AUTO_IMAGE],
        [IntensitySelectionEnum.AUTO_IMAGE]
    ) == ([0, 1], [10, 11])
    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [IntensitySelectionEnum.STRETCH_IMAGE],
        [IntensitySelectionEnum.STRETCH_IMAGE]
    ) == ([0, 1], [10, 11])
    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [10], [100]
    ) == ([10, 10], [100, 100])
    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [10], [1000, 20]
    ) == ([10, 10], [1000, 20])
    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [10, 5], [100000, 20]
    ) == ([10, 5], [65535, 20])
    assert parse_intensity_bounds(
        FakeImage(16, 2), [0, 1], [0], [0], [10, IntensitySelectionEnum.AUTO_IMAGE],
        [100000, 20]
    ) == ([10, 1], [65535, 20])


def test_parse_colormap_id():
    red = Color("red")
    assert parse_colormap_id(ColormapEnum.NONE, ALL_COLORMAPS, red) is None
    assert parse_colormap_id(
        ColormapEnum.DEFAULT, ALL_COLORMAPS, red
    ).identifier == 'RED'
    assert parse_colormap_id(
        ColormapEnum.DEFAULT_INVERTED, ALL_COLORMAPS, red
    ).identifier == '!RED'

    assert parse_colormap_id('JET', ALL_COLORMAPS, red).identifier == 'JET'
    assert parse_colormap_id('!JET', ALL_COLORMAPS, red).identifier == '!JET'

    assert parse_colormap_id('blue', ALL_COLORMAPS, red).identifier == 'BLUE'
    assert parse_colormap_id('!blue', ALL_COLORMAPS, red).identifier == '!BLUE'

    assert parse_colormap_id('!0x0f0', ALL_COLORMAPS, red).identifier == '!LIME'

    assert '#ABCDEF' not in ALL_COLORMAPS
    assert parse_colormap_id('#abcdef', ALL_COLORMAPS, red).identifier == '#ABCDEF'
    assert '#ABCDEF' in ALL_COLORMAPS

    with pytest.raises(ColormapNotFoundProblem):
        parse_colormap_id('brol', ALL_COLORMAPS, red)

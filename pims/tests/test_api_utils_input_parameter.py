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

from pims.api.exceptions import BadRequestException
from pims.api.utils.input_parameter import check_reduction_validity, parse_planes, parse_region
from pims.api.utils.models import TierIndexType
from pims.processing.region import Region
from tests.conftest import not_raises
from tests.test_api_utils_output_parameter import FakeImagePyramid


def test_parse_planes():
    assert parse_planes([], 10) == [0]
    assert parse_planes([1, 2], 10) == [1, 2]
    assert parse_planes([1, 2, 200], 10) == [1, 2]
    assert parse_planes([2, '5:'], 8) == [2, 5, 6, 7]
    assert parse_planes([':'], 3) == [0, 1, 2]
    assert parse_planes([], 10, default=[1, 2]) == [1, 2]

    with pytest.raises(BadRequestException):
        parse_planes([2, '5:', 'foo'], 10)


def test_check_reduction_validity():
    with not_raises(BadRequestException):
        check_reduction_validity([0], None)
        check_reduction_validity([], None)
        check_reduction_validity([1, 2], "SUM")

    with pytest.raises(BadRequestException):
        check_reduction_validity([1, 2], None)


def test_parse_region():
    img = FakeImagePyramid(1000, 2000, 3)

    region = {'top': 100, 'left': 50, 'width': 128, 'height': 128}
    assert parse_region(
        img, **region, tier_idx=0, tier_type=TierIndexType.LEVEL
    ) == Region(100, 50, 128, 128)
    assert parse_region(
        img, **region, tier_idx=1, tier_type=TierIndexType.LEVEL
    ) == Region(100, 50, 128, 128, downsample=2)

    region = {'top': 0.1, 'left': 0.15, 'width': 0.02, 'height': 0.2}
    assert parse_region(
        img, **region, tier_idx=0, tier_type=TierIndexType.LEVEL
    ) == Region(200, 150, 20, 400)
    assert parse_region(
        img, **region, tier_idx=1, tier_type=TierIndexType.LEVEL
    ) == Region(100, 75, 10, 200, downsample=2)

    with pytest.raises(BadRequestException):
        region = {'top': 100, 'left': 900, 'width': 1280, 'height': 1280}
        parse_region(
            img, **region, tier_idx=0, tier_type=TierIndexType.LEVEL,
            silent_oob=False
        )

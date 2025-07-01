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

from pims.api.exceptions import BadRequestException, TooLargeOutputProblem
from pims.api.utils.header import SafeMode
from pims.api.utils.models import TierIndexType
from pims.api.utils.output_parameter import (
    check_level_validity, check_tilecoord_validity, check_tileindex_validity, check_zoom_validity,
    get_thumb_output_dimensions,
    safeguard_output_dimensions
)
from pims.formats.utils.structures.pyramid import Pyramid
from tests.conftest import not_raises


def test_get_output_dimensions():
    class FakeImage:
        def __init__(self, width, height):
            self.width = width
            self.height = height

    assert get_thumb_output_dimensions(FakeImage(1000, 2000), height=200) == (100, 200)
    assert get_thumb_output_dimensions(FakeImage(1000, 2000), width=100) == (100, 200)
    assert get_thumb_output_dimensions(FakeImage(1000, 2000), length=200) == (100, 200)
    assert get_thumb_output_dimensions(FakeImage(2000, 1000), length=200) == (200, 100)
    assert get_thumb_output_dimensions(
        FakeImage(1000, 2000), width=20, length=3, height=500
    ) == (250, 500)

    with pytest.raises(BadRequestException):
        get_thumb_output_dimensions(FakeImage(1000, 2000))


def test_safeguard_output_dimensions():
    assert safeguard_output_dimensions(SafeMode.UNSAFE, 100, 10000, 10000) == (10000, 10000)
    assert safeguard_output_dimensions(SafeMode.SAFE_RESIZE, 100, 10, 99) == (10, 99)
    assert safeguard_output_dimensions(SafeMode.SAFE_RESIZE, 100, 1000, 2000) == (50, 100)
    assert safeguard_output_dimensions(SafeMode.SAFE_RESIZE, 100, 2000, 1000) == (100, 50)

    with pytest.raises(TooLargeOutputProblem):
        assert safeguard_output_dimensions(SafeMode.SAFE_REJECT, 100, 1000, 2000)

    with not_raises(TooLargeOutputProblem):
        assert safeguard_output_dimensions(SafeMode.SAFE_REJECT, 100, 10, 99)


class FakeImagePyramid:
    def __init__(self, width, height, n_tiers):
        self.pyramid = Pyramid()
        for i in range(n_tiers):
            self.pyramid.insert_tier(width / (2 ** i), height / (2 ** i), 256)


def test_check_level_validity():
    with not_raises(BadRequestException):
        check_level_validity(FakeImagePyramid(100, 100, 1).pyramid, 0)
        check_level_validity(FakeImagePyramid(100, 100, 20).pyramid, 10)
        check_level_validity(FakeImagePyramid(100, 100, 20).pyramid, None)

    with pytest.raises(BadRequestException):
        check_level_validity(FakeImagePyramid(100, 100, 1).pyramid, 1)

    with pytest.raises(BadRequestException):
        check_level_validity(FakeImagePyramid(100, 100, 20).pyramid, 25)


def test_check_zoom_validity():
    with not_raises(BadRequestException):
        check_zoom_validity(FakeImagePyramid(100, 100, 1).pyramid, 0)
        check_zoom_validity(FakeImagePyramid(100, 100, 20).pyramid, 10)
        check_zoom_validity(FakeImagePyramid(100, 100, 20).pyramid, None)

    with pytest.raises(BadRequestException):
        check_zoom_validity(FakeImagePyramid(100, 100, 1).pyramid, 1)

    with pytest.raises(BadRequestException):
        check_zoom_validity(FakeImagePyramid(100, 100, 20).pyramid, 25)


def test_check_tileindex_validity():
    img = FakeImagePyramid(1000, 2000, 1)

    with not_raises(BadRequestException):
        check_tileindex_validity(img.pyramid, 0, 0, TierIndexType.ZOOM)
        check_tileindex_validity(img.pyramid, 0, 0, TierIndexType.LEVEL)
        check_tileindex_validity(img.pyramid, 31, 0, TierIndexType.ZOOM)
        check_tileindex_validity(img.pyramid, 31, 0, TierIndexType.LEVEL)

    with pytest.raises(BadRequestException):
        check_tileindex_validity(img.pyramid, 32, 0, TierIndexType.ZOOM)

    with pytest.raises(BadRequestException):
        check_tileindex_validity(img.pyramid, -1, 0, TierIndexType.ZOOM)


def test_check_tilecoord_validity():
    img = FakeImagePyramid(1000, 2000, 1)

    with not_raises(BadRequestException):
        check_tilecoord_validity(img.pyramid, 0, 0, 0, TierIndexType.ZOOM)
        check_tilecoord_validity(img.pyramid, 0, 0, 0, TierIndexType.LEVEL)
        check_tilecoord_validity(img.pyramid, 3, 7, 0, TierIndexType.ZOOM)
        check_tilecoord_validity(img.pyramid, 3, 7, 0, TierIndexType.LEVEL)

    with pytest.raises(BadRequestException):
        check_tilecoord_validity(img.pyramid, 3, 8, 0, TierIndexType.ZOOM)

    with pytest.raises(BadRequestException):
        check_tilecoord_validity(img.pyramid, -1, 0, 0, TierIndexType.ZOOM)

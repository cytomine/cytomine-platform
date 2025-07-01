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
from pims.utils.iterables import check_array_size, ensure_list
from tests.conftest import not_raises


def test_check_array_size():
    with not_raises(BadRequestException):
        check_array_size([1], [1, 2], True)
        check_array_size(None, [1, 2], True)

    with pytest.raises(BadRequestException):
        check_array_size([1], [2], True)

    with pytest.raises(BadRequestException):
        check_array_size(None, [1], False)

    with pytest.raises(BadRequestException):
        check_array_size([1], [], True)


def test_ensure_list():
    assert ensure_list(3) == [3]
    assert ensure_list((2, 4)) == [(2, 4)]
    assert ensure_list("a") == ['a']
    assert ensure_list([2]) == [2]
    assert ensure_list(None) == []

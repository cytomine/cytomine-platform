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

from pims.api.utils.range_parameter import is_range, parse_range


def test_is_range():
    assert is_range("10:20") is True
    assert is_range("20:10") is True
    assert is_range(":10") is True
    assert is_range("10:") is True
    assert is_range(":") is True
    assert is_range("a") is False
    assert is_range("2:3:4") is False


def test_parse_range():
    assert parse_range("10:20", 0, 100) == range(10, 20)
    assert parse_range("20:10", 0, 100) == range(10, 20)
    assert parse_range(":10", 0, 100) == range(0, 10)
    assert parse_range("10:", 0, 100) == range(10, 100)
    assert parse_range(":", 0, 100) == range(0, 100)

    with pytest.raises(ValueError):
        parse_range("a", 0, 100)

    with pytest.raises(ValueError):
        parse_range("2:3:4", 0, 100)

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

from pims.utils.types import parse_boolean, parse_float, parse_json


def test_boolean_parser():
    assert parse_boolean(True) is True
    assert parse_boolean(False) is False
    assert parse_boolean("truE") is True
    assert parse_boolean("YeS") is True
    assert parse_boolean("nO") is False

    assert parse_boolean("test", False) is None
    with pytest.raises(ValueError):
        parse_boolean("test", True)

    assert parse_boolean(1, False) is None
    with pytest.raises(ValueError):
        parse_boolean(1, True)


def test_json_parser():
    assert parse_json("{}") == dict()
    assert parse_json('{ "age":100}') == dict(age=100)
    assert parse_json("{\"age\":100 }") == dict(age=100)
    assert parse_json('{"age":100 }') == dict(age=100)
    assert parse_json('{"foo":[5,6.8],"foo":"bar"}') == dict(foo="bar")

    assert parse_json("{asdf}", False) is None
    with pytest.raises(ValueError):
        parse_json("{asdf}", True)

    assert parse_json("{'age':100 }", False) is None
    with pytest.raises(ValueError):
        parse_json("{'age':100 }", True)


def test_float_parser():
    assert parse_float(2.3) == 2.3
    assert parse_float("2.3") == 2.3
    assert parse_float("2,3") == 2.3

    assert parse_float("foo", False) is None
    with pytest.raises(ValueError):
        parse_float("foo", True)

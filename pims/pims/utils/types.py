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

import json
from datetime import datetime
from typing import Any, List, Union


def parse_json(value: Any, raise_exc: bool = False) -> Union[dict, None]:
    try:
        return json.loads(value)
    except:  # noqa
        if raise_exc:
            raise
        return None


def parse_boolean(value: Any, raise_exc: bool = False) -> Union[bool, None]:
    _true_set = {'yes', 'true', 't', 'y', '1'}
    _false_set = {'no', 'false', 'f', 'n', '0'}

    if value is True or value is False:
        return value
    elif isinstance(value, str):
        value = value.lower()
        if value in _true_set:
            return True
        if value in _false_set:
            return False

    if raise_exc:
        raise ValueError('Expected "%s"' % '", "'.join(_true_set | _false_set))
    return None


def parse_float(value: Any, raise_exc: bool = False) -> Union[float, None]:
    if type(value) == str:
        value = value.replace(",", ".")
    try:
        return float(value)
    except:  # noqa
        if raise_exc:
            raise
        return None


def parse_int(value: Any, raise_exc: bool = False) -> Union[int, None]:
    try:
        return int(value)
    except:  # noqa
        if raise_exc:
            raise
        return None


def parse_datetime(
    value: Any, formats: List[str] = None, raise_exc: bool = False
) -> Union[datetime, None]:
    if formats is None:
        formats = [
            "%Y:%m:%d %H:%M:%S",
            "%m/%d/%y %H:%M:%S"
        ]

    for format in formats:
        try:
            return datetime.strptime(value, format)
        except (ValueError, TypeError):
            continue
    if raise_exc:
        raise ValueError
    return None


def parse_bytes(
    value: Any, encoding: str = None, errors: str = 'strict',
    raise_exc: bool = False
) -> Union[str, None]:
    """Return Unicode string from encoded bytes."""
    try:
        if encoding is not None:
            return value.decode(encoding, errors)
        try:
            return value.decode('utf-8', errors)
        except UnicodeDecodeError:
            return value.decode('cp1252', errors)
    except:  # noqa
        if raise_exc:
            raise ValueError
        return None


def is_int(value: Any) -> bool:
    try:
        int(value)
        return True
    except ValueError:
        return False

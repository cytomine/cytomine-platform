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

import collections.abc
from typing import Any, List, Union


def get_first(d: dict, keys: List[Any], default: Any = None) -> Any:
    """
    Get first non-null value for the list of keys.
    If all values are null, `default` is returned.
    """
    for k in keys:
        v = d.get(k)
        if v is not None:
            return v
    return default


def invert(d: dict) -> dict:
    """Invert keys and values in a dictionary"""
    return {
        v: k for k, v in d.items()
    }


def flatten(
    d: Union[dict, collections.abc.MutableMapping], parent_key='', sep='.'
) -> dict:
    """
    Deeply flatten a dictionary.
    Nested dictionary keys are renamed as <parent_key><sep><nested_key>
    """
    items = []
    for k, v in d.items():
        if parent_key:
            if k.startswith('['):
                new_key = parent_key + k
            else:
                new_key = parent_key + sep + k
        else:
            new_key = k
        if isinstance(v, collections.abc.MutableMapping):
            items.extend(flatten(v, new_key, sep=sep).items())
        else:
            items.append((new_key, v))
    return dict(items)

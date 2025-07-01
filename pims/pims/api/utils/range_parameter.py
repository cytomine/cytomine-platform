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
from typing import Any

from pims.utils.types import is_int


def is_range(value: Any) -> bool:
    """
    Whether a value is a PIMS range or not.
    Valid range examples: ":", "2:", ":2", "2:4"

    Parameters
    ----------
    value
        Value expected to be formatted as a range.

    Returns
    -------
    bool
        Whether it is a range.
    """
    if not isinstance(value, str):
        return False
    split = [v.strip() for v in value.split(':')]
    return len(split) == 2 and all([bound == '' or is_int(bound) for bound in split])


def parse_range(pims_range: Any, mini: int, maxi: int) -> range:
    """
    Cast PIMS range to a Python range. Implicit low and high bounds
    are replace by `mini` and `maxi` respectively if necessary.

    Parameters
    ----------
    pims_range
        PIMS range to convert.
    mini
        Value replacing implicit low bound.
    maxi
        Value replacing implicit high bound.

    Returns
    -------
    range
        Python range, always in ascending order.

    Raises
    ------
    ValueError
        If `pims_range` is not a PIMS range.
    """
    if not is_range(pims_range):
        raise ValueError(f'Invalid literal for Range(): {pims_range}')

    low, high = [v.strip() for v in pims_range.split(':')]
    low = mini if low == '' else int(low)
    high = maxi if high == '' else int(high)

    low, high = min(low, high), max(low, high)
    return range(low, high)

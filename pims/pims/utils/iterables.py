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
from typing import Any, Dict, Iterable, List, Optional, Sized, TypeVar, Union

from pims.api.exceptions import BadRequestException

T = TypeVar('T')


def split_tuple(tuple_: Any, index: int) -> Any:
    if type(tuple_) == tuple:
        return tuple_[index]
    else:
        return tuple_


def find_first_available_int(values, mini=0, maxi=100) -> int:
    """
    Find first available integer between bounds which is not in a list.

    Parameters
    ----------
    values : list of int, array-like
        A list of unavailable integers.
    mini : int (optional)
        Minimum possible integer (inclusive).
    maxi : int (optional)
        Maximum possible integer (exclusive).

    Returns
    -------
    available : int

    Raises
    ------
    ValueError
        If there is no available integer.
    """
    for i in range(mini, maxi):
        if i not in values:
            return i

    raise ValueError("There is no available integer.")


def ensure_list(value: Union[List[T], T]) -> List[T]:
    """
    Ensure it is a list.

    Parameters
    ----------
    value
        Value to convert as a list

    Returns
    -------
    transformed
        The value converted as a list if it is not already the case.
    """
    if value is not None:
        return value if type(value) is list else [value]
    return []


def check_array_size(
    iterable: Optional[Sized], allowed: List[int], nullable: bool = True,
    name: Optional[str] = None
):
    """
    Verify an iterable has an allowed size or, optionally, is empty.

    Parameters
    ----------
    iterable
        Iterable which the size has to be verified.
    allowed
        Allowed iterable sizes
    nullable
        Whether no iterable at all is accepted or not.
    name
        Iterable name for exception messages.

    Raises
    ------
    BadRequestException
        If the iterable doesn't have one of the allowed sizes
        or is None if `nullable` is false.

    """
    if iterable is None:
        if not nullable:
            name = 'A parameter' if not name else name
            raise BadRequestException(detail=f"{name} is unset while it is not allowed.")
        return

    if not len(iterable) in allowed:
        name = 'A parameter' if not name else name
        allowed_str = ', '.join([str(i) for i in set(allowed)])
        raise BadRequestException(
            f'{name} has a size of {len(iterable)} '
            f'while only these sizes are allowed: {allowed_str}'
        )


def check_array_size_parameters(
    parameter_names: Iterable[str], parameters: Dict, allowed: List[int],
    nullable: bool = True
):
    for name in parameter_names:
        value = parameters.get(name)
        check_array_size(
            value, allowed=allowed, nullable=nullable, name=name
        )


def flatten(t):
    return [item for sublist in t for item in sublist]


def product(iterable):
    prod = 1
    for i in iterable:
        prod *= i
    return prod

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
from typing import Tuple, Union


def get_rationed_resizing(
    resized: Union[int, float], length: int, other_length: int
) -> Tuple[int, int]:
    """
    Get resized lengths for `length` and `other_length` according to
    the ratio between `resized` and `length`.

    Parameters
    ----------
    resized : int or float
        Already resized length. If float, it is the ratio.
    length : int
        Non-resized length related to `resized`.
    other_length : int
        Other non-resized length to resize according the ratio.

    Returns
    -------
    resized : int
        First resized length according ratio.
    other_resized : int
        Other resized length according ratio.
    """
    ratio = resized if type(resized) == float else resized / length
    resized = resized if type(resized) == int else round(ratio * length)
    other_resized = round(ratio * other_length)
    return resized, other_resized


def max_intensity(bitdepth: int, count: bool = False):
    """
    Get maximum intensity for a given bitdepth.
    To get number of possible intensities, set `count` to True.
    """
    mi = 2 ** bitdepth
    if not count:
        mi -= 1
    return mi

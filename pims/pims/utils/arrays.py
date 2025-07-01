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

import numpy as np
from skimage import dtype_limits
from skimage.exposure.exposure import _offset_array  # noqa


def to_unsigned_int(arr: np.ndarray) -> np.ndarray:
    """
    Offset the array to get the lowest value at 0 if there is any negative.
    """
    if arr.dtype is not np.uint8 or arr.dtype is not np.uint16:
        arr_min, arr_max = dtype_limits(arr, clip_negative=False)
        arr = _offset_array(arr, arr_min, arr_max)
    return arr

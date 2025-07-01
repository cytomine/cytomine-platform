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


def dtype_to_bits(dtype) -> int:
    """Get number of bits for a dtype-like (Numpy or string datatype)."""
    if type(dtype) is str:
        dtype = np.dtype(dtype)
    return dtype.type(0).nbytes * 8


def bits_to_str_dtype(bits: int) -> str:
    """
    Get the required (string) datatype for data encoded on given bits.

    Parameters
    ----------
    bits
        Number of bits used to encode data

    Returns
    -------
    str_dtype
        Datatype (in string format) for given `bits`
    """
    if bits > 16:
        return 'uint32'
    elif bits > 8:
        return 'uint16'
    else:
        return 'uint8'


def np_dtype(bits: int) -> np.dtype:
    """
    Get Numpy datatype for data encoded on given bits.

    Parameters
    ----------
    bits
        Number of bits used to encode data

    Returns
    -------
    dtype
        Numpy datatype for given `bits`
    """
    return np.dtype(bits_to_str_dtype(bits))

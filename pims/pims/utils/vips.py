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
from typing import List

import numpy as np
from pyvips import Image as VIPSImage, Interpretation as VIPSInterpretation, Operation  # noqa

from pims.api.utils.models import ChannelReduction
from pims.utils.dtypes import bits_to_str_dtype

vips_format_to_dtype = {
    'uchar': np.uint8,
    'char': np.int8,
    'ushort': np.uint16,
    'short': np.int16,
    'uint': np.uint32,
    'int': np.int32,
    'float': np.float32,
    'double': np.float64,
    'complex': np.complex64,
    'dpcomplex': np.complex128,
}

dtype_to_vips_format = {
    'uint8': 'uchar',
    'int8': 'char',
    'uint16': 'ushort',
    'int16': 'short',
    'uint32': 'uint',
    'int32': 'int',
    'float32': 'float',
    'float64': 'double',
    'complex64': 'complex',
    'complex128': 'dpcomplex',
}

vips_interpretation_to_mode = {
    'b-w': 'L',
    'rgb': 'RGB',
    'srgb': 'RGB',
    'cmyk': 'CMYK',
    'rgb16': 'RGB',
    'grey16': 'L'
}

format_to_vips_suffix = {
    'JPEG': '.jpg',
    'JPG': '.jpg',
    'PNG': '.png',
    'WEBP': '.webp'
}


def vips_dtype(bits: int) -> str:
    """VIPS format for a given number of bits."""
    return dtype_to_vips_format[bits_to_str_dtype(bits)]


def bandjoin(bands: List[VIPSImage]) -> VIPSImage:
    if len(bands) == 1:
        return bands[0]

    return Operation.call('bandjoin', bands)


def bandjoin_rgb(bands: List[VIPSImage]) -> VIPSImage:
    return fix_rgb_interpretation(bandjoin(bands))


def fix_rgb_interpretation(im: VIPSImage) -> VIPSImage:
    if im.interpretation == VIPSInterpretation.GREY16:
        im = im.copy(interpretation=VIPSInterpretation.RGB16)
    elif im.interpretation == VIPSInterpretation.B_W:
        im = im.copy(interpretation=VIPSInterpretation.SRGB)
    return im


def bandreduction(bands: List[VIPSImage], reduction: ChannelReduction) -> VIPSImage:
    if reduction == ChannelReduction.ADD:
        return VIPSImage.sum(bands).cast(bands[0].format)
    elif reduction == ChannelReduction.MAX:
        return Operation.call('bandrank', bands, index=len(bands) - 1)
    elif reduction == ChannelReduction.MIN:
        return Operation.call('bandrank', bands, index=0)
    elif reduction == ChannelReduction.MED:
        return Operation.call('bandrank', bands, index=-1)
    else:
        raise ValueError(f"{reduction} not implemented")

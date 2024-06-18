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

from abc import ABC

import numpy as np
from scipy import linalg  # noqa
from skimage.color import (
    bex_from_rgb, combine_stains, hdx_from_rgb, hed_from_rgb, rgb_from_bex,
    rgb_from_hdx, rgb_from_hed, separate_stains
)
from skimage.util.dtype import _convert

from pims.api.utils.models import Colorspace, FilterType
from pims.filters import AbstractFilter


def color_deconvolution(img, sep_matrix, comb_matrix, component_idx):
    separated = separate_stains(img, sep_matrix)

    stains = np.zeros_like(separated)
    stains[:, :, component_idx] = separated[:, :, component_idx]

    combined = combine_stains(stains, comb_matrix)

    return _convert(combined, img.dtype)


class AbstractColorDeconvolutionFilter(AbstractFilter, ABC):
    @classmethod
    def get_type(cls):
        return FilterType.PIXEL

    @classmethod
    def require_histogram(cls):
        return False

    @classmethod
    def required_colorspace(cls):
        return Colorspace.COLOR

    @classmethod
    def identifier(cls):
        return cls.__name__.replace('DeconvolutionFilter', '').replace('2', '-')


# H&E DAB deconvolution filters

class HEDAB2HematoxylinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hed_from_rgb, rgb_from_hed, 0)

    @classmethod
    def get_name(cls):
        return "H&E DAB -> Hematoxylin"

    @classmethod
    def aliases(cls):
        return ["hedab-haematoxylin"]


class HEDAB2EosinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hed_from_rgb, rgb_from_hed, 1)

    @classmethod
    def get_name(cls):
        return "H&E DAB -> Eosin"


class HEDAB2DABDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hed_from_rgb, rgb_from_hed, 2)

    @classmethod
    def get_name(cls):
        return "H&E DAB -> DAB"


# Giemsa

class Giemsa2MethylBlueDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, bex_from_rgb, rgb_from_bex, 0)

    @classmethod
    def get_name(cls):
        return "Giemsa -> Methyl Blue"


class Giemsa2EosinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, bex_from_rgb, rgb_from_bex, 1)

    @classmethod
    def get_name(cls):
        return "Giemsa -> Eosin"


# Hematoxylin & DAB

class HDAB2HematoxylinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hdx_from_rgb, rgb_from_hdx, 0)

    @classmethod
    def get_name(cls):
        return "H DAB -> Hematoxylin"

    @classmethod
    def aliases(cls):
        return ["hdab-haematoxylin"]


class HDAB2DABDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hdx_from_rgb, rgb_from_hdx, 1)

    @classmethod
    def get_name(cls):
        return "H DAB -> DAB"

    @classmethod
    def aliases(cls):
        return ["hdab-dab"]


# H&E

# From https://github.com/fiji/Colour_Deconvolution/blob/master/src/main/resources/sc/fiji/colourDeconvolution/colourdeconvolution.txt#L2
rgb_from_hex = np.array(
    [[0.644211, 0.716556, 0.266844],
     [0.092789, 0.954111, 0.283111],
     [0.0, 0.0, 0.0]]
)
rgb_from_hex[2, :] = np.cross(rgb_from_hex[0, :], rgb_from_hex[1, :])
hex_from_rgb = linalg.inv(rgb_from_hex)


class HE2HematoxylinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hex_from_rgb, rgb_from_hex, 0)

    @classmethod
    def get_name(cls):
        return "H&E -> Hematoxylin"

    @classmethod
    def aliases(cls):
        return ["he-haematoxylin"]


class HE2EosinDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, hex_from_rgb, rgb_from_hex, 1)

    @classmethod
    def get_name(cls):
        return "H&E -> Eosin"

    @classmethod
    def aliases(cls):
        return ["he-eosin"]


# RGB substractive

rgb_from_rgbsub = np.array(
    [[0.0, 1.0, 1.0],
     [1.0, 0.0, 1.0],
     [1.0, 1.0, 0.0]]
)
rgbsub_from_rgb = linalg.inv(rgb_from_rgbsub)


class RGB2RedDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, rgbsub_from_rgb, rgb_from_rgbsub, 0)

    @classmethod
    def get_name(cls):
        return "RGB- -> Red"

    @classmethod
    def aliases(cls):
        return ["r_rgb"]


class RGB2GreenDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, rgbsub_from_rgb, rgb_from_rgbsub, 1)

    @classmethod
    def get_name(cls):
        return "RGB- -> Green"

    @classmethod
    def aliases(cls):
        return ["g_rgb"]


class RGB2BlueDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, rgbsub_from_rgb, rgb_from_rgbsub, 2)

    @classmethod
    def get_name(cls):
        return "RGB- -> Blue"

    @classmethod
    def aliases(cls):
        return ["b_rgb"]


# CMY substractive

rgb_from_cmy = np.array(
    [[1.0, 0.0, 0.0],
     [0.0, 1.0, 0.0],
     [0.0, 0.0, 1.0]]
)
cmy_from_rgb = linalg.inv(rgb_from_cmy)


class CMY2CyanDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, cmy_from_rgb, rgb_from_cmy, 0)

    @classmethod
    def get_name(cls):
        return "CMY- -> Cyan"

    @classmethod
    def aliases(cls):
        return ["c_cmy"]


class CMY2MagentaDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, cmy_from_rgb, rgb_from_cmy, 1)

    @classmethod
    def get_name(cls):
        return "CMY- -> Magenta"

    @classmethod
    def aliases(cls):
        return ["m_cmy"]


class CMY2YellowDeconvolutionFilter(AbstractColorDeconvolutionFilter):
    def __init__(self):
        super().__init__()
        self._impl[np.ndarray] = self._numpy_impl

    def _numpy_impl(self, img, *args, **kwargs):
        return color_deconvolution(img, cmy_from_rgb, rgb_from_cmy, 2)

    @classmethod
    def get_name(cls):
        return "CMY- -> Yellow"

    @classmethod
    def aliases(cls):
        return ["y_cmy"]

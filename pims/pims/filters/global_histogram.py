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

from abc import ABC, abstractmethod
from functools import cached_property

import numpy as np
from pyvips import Image as VIPSImage

from pims.api.utils.models import Colorspace, FilterType
from pims.filters import AbstractFilter
from pims.processing.histograms.utils import clamp_histogram


class AbstractGlobalFilter(AbstractFilter, ABC):
    @classmethod
    def get_type(cls):
        return FilterType.GLOBAL


class AbstractGlobalThresholdFilter(AbstractGlobalFilter, ABC):
    @classmethod
    def require_histogram(cls):
        return True

    @classmethod
    def required_colorspace(cls):
        return Colorspace.GRAY

    def __init__(self, histogram=None, white_objects=False):
        super().__init__(histogram)
        self.white_objects = white_objects
        self._impl[VIPSImage] = self._vips_impl

    @cached_property
    @abstractmethod
    def threshold(self):
        pass

    def _vips_impl(self, img, *args, **kwargs):
        if self.white_objects:
            return img <= self.threshold
        else:
            return img > self.threshold

    @classmethod
    def get_name(cls):
        return f"{super().get_name()} Threshold"


class OtsuThresholdFilter(AbstractGlobalThresholdFilter):
    @classmethod
    def identifier(cls):
        return "Otsu"

    @classmethod
    def get_description(cls):
        return "Otsu global filtering"

    @cached_property
    def threshold(self):
        from skimage.filters import threshold_otsu
        return threshold_otsu(hist=clamp_histogram(self.histogram))


class IsodataThresholdFilter(AbstractGlobalThresholdFilter):
    @classmethod
    def identifier(cls):
        return "IsoData"

    @cached_property
    def threshold(self):
        from skimage.filters import threshold_isodata
        return threshold_isodata(hist=clamp_histogram(self.histogram))

    @classmethod
    def get_description(cls):
        return "Isodata global filtering"

    @classmethod
    def aliases(cls):
        # Default ImageJ auto threshold is a slight variant of Isodata threshold
        # https://imagej.net/plugins/auto-threshold
        return ["binary"]


class YenThresholdFilter(AbstractGlobalThresholdFilter):
    @classmethod
    def identifier(cls):
        return "Yen"

    @cached_property
    def threshold(self):
        from skimage.filters import threshold_yen
        return threshold_yen(hist=clamp_histogram(self.histogram))

    @classmethod
    def get_description(cls):
        return "Yen global filtering"


class MinimumThresholdFilter(AbstractGlobalThresholdFilter):
    @classmethod
    def identifier(cls):
        return "Minimum"

    @cached_property
    def threshold(self):
        from skimage.filters import threshold_minimum
        return threshold_minimum(hist=clamp_histogram(self.histogram))

    @classmethod
    def get_description(cls):
        return "Minimum global filtering"


class MeanThresholdFilter(AbstractGlobalThresholdFilter):
    @cached_property
    def threshold(self):
        hist, _ = clamp_histogram(self.histogram)
        return np.average(np.arange(hist.size), weights=hist)

    @classmethod
    def identifier(cls):
        return "Mean"

    @classmethod
    def get_description(cls):
        return "Mean global filtering"

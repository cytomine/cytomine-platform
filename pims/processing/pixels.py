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
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import List, TYPE_CHECKING, Union

import numpy as np
from PIL.Image import Image as PILImage
from pyvips import (
    Image as VIPSImage,
    Interpretation as VIPSInterpretation, Size as VIPSSize  # noqa
)

from pims.api.utils.mimetype import OutputExtension
from pims.api.utils.models import ChannelReduction, Colorspace
from pims.processing.adapters import RawImagePixels, convert_to, numpy_to_vips, pil_to_numpy
from pims.processing.colormaps import LookUpTable, StackedLookUpTables, get_lut_from_stacked
from pims.utils.vips import bandjoin, bandreduction, vips_dtype, vips_format_to_dtype

if TYPE_CHECKING:
    from pims.filters import AbstractFilter

DEFAULT_WEBP_QUALITY = 75
DEFAULT_WEBP_LOSSLESS = False
DEFAULT_PNG_COMPRESSION = 6
DEFAULT_JPEG_QUALITY = 75


class ImagePixelsImpl(ABC):
    def __init__(self, pixels):
        self._context = None
        self.pixels = pixels

    @abstractmethod
    def implementation(self):
        pass

    @property
    def context(self) -> ImagePixels:
        return self._context

    @context.setter
    def context(self, value: ImagePixels):
        self._context = value

    @abstractmethod
    def append_channel(self, pixels) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def prepare_channels(self, required_indexes: List[int]) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def apply_lut(self, lut: LookUpTable) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def apply_lut_stack(
        self, lut_stack: StackedLookUpTables, reduction: ChannelReduction, is_rgb: bool
    ) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def resize(self, width: int, height: int) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def channel_reduction(self, reduction: ChannelReduction) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def change_colorspace(self, colorspace: Colorspace) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def int_clip(self) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def add_transparency(
        self, bg_transparency: int, transparency_mask: np.ndarray
    ) -> ImagePixelsImpl:
        pass

    @abstractmethod
    def draw_on(self, draw: np.ndarray, condition_mask: np.ndarray) -> ImagePixelsImpl:
        pass

    def apply_filter(self, im_filter: AbstractFilter) -> ImagePixelsImpl:
        if self.implementation() in im_filter.implementations:
            self.pixels = im_filter(self.pixels)
            return self

        return self.context.transition_to(
            im_filter.implementations[0]
        ).apply_filter(im_filter)

    @abstractmethod
    def compress(self, format: OutputExtension, bitdepth: int, **format_params) -> bytes:
        pass


class NumpyImagePixels(ImagePixelsImpl):
    def __init__(self, pixels: np.ndarray):
        super().__init__(pixels)
        self.pixels = np.atleast_3d(pixels)

    def append_channel(self, pixels: np.ndarray) -> ImagePixelsImpl:
        self.pixels = np.dstack((self.pixels, pixels))
        return self

    def prepare_channels(self, required_indexes: List[int]) -> ImagePixelsImpl:
        self.pixels = self.pixels[:, :, required_indexes]
        return self

    def apply_lut(self, lut: LookUpTable) -> ImagePixelsImpl:
        # TODO
        return self.context.transition_to(VIPSImage).apply_lut(lut)

    def apply_lut_stack(
        self, lut_stack: StackedLookUpTables, reduction: ChannelReduction, is_rgb: bool
    ) -> ImagePixelsImpl:
        # TODO
        return self.context.transition_to(
            VIPSImage
        ).apply_lut_stack(lut_stack, reduction, is_rgb)

    def resize(self, width: int, height: int) -> ImagePixelsImpl:
        return self.context.transition_to(VIPSImage).resize(width, height)

    def channel_reduction(self, reduction: ChannelReduction) -> ImagePixelsImpl:
        dtype = self.pixels.dtype
        # TODO
        if reduction == ChannelReduction.MED:
            self.pixels = np.rint(np.median(self.pixels, axis=2), dtype=dtype)
        elif reduction == ChannelReduction.MAX:
            self.pixels = np.max(self.pixels, axis=2)
        elif reduction == ChannelReduction.MIN:
            self.pixels = np.min(self.pixels, axis=2)
        # elif reduction == ChannelReduction.AVG:
        #     self.pixels = np.rint(np.average(self.pixels, axis=2), dtype=dtype)
        else:
            raise ValueError(f"Not implemented {reduction}")

        return self

    def change_colorspace(self, colorspace: Colorspace) -> ImagePixelsImpl:
        return self.context.transition_to(VIPSImage).change_colorspace(
            colorspace
        )

    def int_clip(self) -> ImagePixelsImpl:
        dtype = self.pixels.dtype
        if dtype == np.uint8 or dtype == np.int8:
            self.pixels = np.clip(self.pixels, 0, 255).astype(np.uint8)
        else:
            self.pixels = np.clip(self.pixels, 0, 65535).astype(np.uint16)
        return self

    def add_transparency(
        self, bg_transparency: int, transparency_mask: np.ndarray
    ) -> ImagePixelsImpl:
        transparency_mask = transparency_mask.astype(self.pixels.dtype)
        return self.append_channel(transparency_mask)

    def draw_on(self, draw: np.ndarray, condition_mask: np.ndarray) -> ImagePixelsImpl:
        draw = draw.astype(self.pixels.dtype)
        draw = np.atleast_3d(draw)

        condition_mask = np.atleast_3d(condition_mask)
        self.pixels = np.where(condition_mask, self.pixels, draw)  # check broadcast
        return self

    def compress(self, format: OutputExtension, bitdepth: int, **format_params) -> bytes:
        return self.context.transition_to(VIPSImage).compress(
            format, bitdepth, **format_params
        )

    def implementation(self):
        return np.ndarray


class VipsImagePixels(ImagePixelsImpl):
    def append_channel(self, pixels):
        self.pixels = self.pixels.bandjoin(pixels)
        return self

    def prepare_channels(self, required_indexes: List[int]) -> ImagePixelsImpl:
        self.pixels = bandjoin([self.pixels[i] for i in required_indexes])
        return self

    def apply_lut(self, lut: LookUpTable) -> ImagePixelsImpl:
        """
        Apply lookup table
        out shape: in.h, in.w, n where
        * n = in.c if lut.n_component == 1 or lut.n_component == in.c
        * n = lut.n_components if in.c == 1
        dtype: lut dtype
        """
        lut = lut[np.newaxis, :, :]
        self.pixels = self.pixels.maplut(convert_to(lut, VIPSImage))
        return self

    def apply_lut_stack(
        self, lut_stack: StackedLookUpTables, reduction: ChannelReduction, is_rgb: bool
    ) -> ImagePixelsImpl:
        stack_size, _, n_components = lut_stack.shape
        if stack_size == 1:
            # As stack size is 1, reduction can be ignored.
            return self.apply_lut(get_lut_from_stacked(lut_stack))
        elif n_components == 1:
            lut_stack = np.swapaxes(lut_stack, 0, 2)
            pixels = self.apply_lut(get_lut_from_stacked(lut_stack))
            if not is_rgb:
                return pixels.channel_reduction(reduction)
            return pixels
        else:
            channels = list()
            for i, channel in enumerate(self.pixels.bandsplit()):
                lut = get_lut_from_stacked(lut_stack, i, as_stack=True)
                channels.append(channel.maplut(convert_to(lut, VIPSImage)))

            if reduction != ChannelReduction.ADD:
                raise ValueError(f"{reduction} should not happen here!")

            self.pixels = bandreduction(channels, ChannelReduction.ADD)
            return self

    def resize(self, width: int, height: int) -> ImagePixelsImpl:
        if self.pixels.width != width or self.pixels.height != height:
            self.pixels = self.pixels.thumbnail_image(
                width, height=height, size=VIPSSize.FORCE
            )
        return self

    def channel_reduction(self, reduction: ChannelReduction) -> ImagePixelsImpl:
        bands = self.pixels.bandsplit()
        self.pixels = bandreduction(bands, reduction)
        return self

    def change_colorspace(self, colorspace: Colorspace) -> ImagePixelsImpl:
        new_colorspace = None

        if self.pixels.format == 'uchar':
            # As libvips makes a distinction between format and interpretation,
            # and due to our various libs usage, there is sometimes
            # inconsistencies. As a quick fix, force interpretation casting
            # when it differs from format.
            # Should be treated more efficiently.
            # Info: https://github.com/libvips/libvips/issues/580
            if self.pixels.interpretation == VIPSInterpretation.RGB16:
                self.pixels = self.pixels.copy(
                    interpretation=VIPSInterpretation.SRGB
                )
            elif self.pixels.interpretation == VIPSInterpretation.GREY16:
                self.pixels = self.pixels.copy(
                    interpretation=VIPSInterpretation.B_W
                )

        if (self.pixels.interpretation == VIPSInterpretation.RGB16
                and colorspace == Colorspace.GRAY):
            new_colorspace = VIPSInterpretation.GREY16
        elif (self.pixels.interpretation == VIPSInterpretation.GREY16
              and colorspace == Colorspace.COLOR):
            new_colorspace = VIPSInterpretation.RGB16
        elif colorspace == Colorspace.COLOR:
            new_colorspace = VIPSInterpretation.SRGB
        elif colorspace == Colorspace.GRAY:
            new_colorspace = VIPSInterpretation.B_W

        if new_colorspace:
            self.pixels = self.pixels.colourspace(new_colorspace)
        return self

    def int_clip(self) -> ImagePixelsImpl:
        format = self.pixels.format
        if format in ('uchar', 'char'):
            self.pixels = self.pixels.cast('uchar')
        else:
            self.pixels = self.pixels.cast('ushort')
        return self

    def add_transparency(self, bg_transparency, transparency_mask: np.ndarray) -> ImagePixelsImpl:
        mask_dtype = vips_format_to_dtype[self.pixels.format]
        transparency_mask = transparency_mask.astype(mask_dtype)
        self.pixels = self.pixels.bandjoin(numpy_to_vips(transparency_mask))
        return self

    def draw_on(self, draw: np.ndarray, condition_mask: np.ndarray) -> ImagePixelsImpl:
        draw_dtype = vips_format_to_dtype[self.pixels.format]
        draw = numpy_to_vips(draw.astype(draw_dtype))

        condition_mask = numpy_to_vips(condition_mask)

        self.pixels = condition_mask.ifthenelse(self.pixels, draw)
        return self

    def compress(self, format: OutputExtension, bitdepth: int, **params) -> bytes:
        clean_params = {}
        if format == OutputExtension.JPEG:
            clean_params['Q'] = params.get(
                'quality',
                params.get('jpeg_quality', DEFAULT_JPEG_QUALITY)
            )
            clean_params['strip'] = True
        elif format == OutputExtension.PNG:
            clean_params['compression'] = params.get(
                'compression',
                params.get('png_compression', DEFAULT_PNG_COMPRESSION)
            )
        elif format == OutputExtension.WEBP:
            clean_params['lossless'] = params.get(
                'lossless',
                params.get('webp_lossless', DEFAULT_WEBP_LOSSLESS)
            )
            clean_params['strip'] = True
            clean_params['Q'] = params.get(
                'quality',
                params.get('webp_quality', DEFAULT_WEBP_QUALITY)
            )

        # Clip by casting image
        image = self.pixels.cast(vips_dtype(bitdepth))
        buffer = image.write_to_buffer(format, **clean_params)
        del image
        return buffer

    def implementation(self):
        return VIPSImage


class ImagePixels:
    _impl: ImagePixelsImpl

    def __init__(self, pixels: Union[RawImagePixels, ImagePixelsImpl]):
        self._impl = None  # noqa

        if not isinstance(pixels, ImagePixelsImpl):
            if type(pixels) is VIPSImage:
                pixels = VipsImagePixels(pixels)
            elif type(pixels) is np.ndarray:
                pixels = NumpyImagePixels(pixels)
            elif isinstance(pixels, PILImage):
                pixels = NumpyImagePixels(pil_to_numpy(pixels))
            else:
                raise ValueError(f"{type(pixels)} is invalid")

        self.set_pixels_impl(pixels)

    def set_pixels_impl(self, impl: ImagePixelsImpl):
        self._impl = impl
        self._impl.context = self

    def transition_to(self, implementation):
        if implementation is VIPSImage:
            pixels = VipsImagePixels(convert_to(self._impl.pixels, VIPSImage))
        elif implementation is np.ndarray:
            pixels = NumpyImagePixels(convert_to(self._impl.pixels, np.ndarray))
        else:
            raise ValueError(f"Invalid {implementation}")

        self.set_pixels_impl(pixels)
        return self._impl

    def np_array(self):
        return convert_to(self._impl.pixels, np.ndarray)

    def append_channel(self, pixels) -> ImagePixels:
        self._impl.append_channel(pixels)
        return self

    def prepare_channels(self, required_indexes: List[int]) -> ImagePixels:
        self._impl.prepare_channels(required_indexes)
        return self
    
    def apply_lut(self, lut: LookUpTable) -> ImagePixels:
        self._impl.apply_lut(lut)
        return self

    def apply_lut_stack(
        self, lut_stack: StackedLookUpTables, reduction: ChannelReduction, is_rgb: bool
    ) -> ImagePixels:
        self._impl.apply_lut_stack(lut_stack, reduction, is_rgb)
        return self
    
    def resize(self, width: int, height: int) -> ImagePixels:
        self._impl.resize(width, height)
        return self
    
    def channel_reduction(self, reduction: ChannelReduction) -> ImagePixels:
        self._impl.channel_reduction(reduction)
        return self
    
    def change_colorspace(self, colorspace: Colorspace) -> ImagePixels:
        self._impl.change_colorspace(colorspace)
        return self

    def int_clip(self) -> ImagePixels:
        self._impl.int_clip()
        return self
    
    def add_transparency(self, transparency_mask: np.ndarray) -> ImagePixels:
        self._impl.add_transparency(100, transparency_mask)
        return self
    
    def draw_on(self, draw: np.ndarray, condition_mask: np.ndarray) -> ImagePixels:
        self._impl.draw_on(draw, condition_mask)
        return self

    def apply_filter(self, im_filter: AbstractFilter) -> ImagePixels:
        self._impl.apply_filter(im_filter)
        return self
    
    def compress(self, format: OutputExtension, bitdepth: int, **format_params) -> bytes:
        return self._impl.compress(format, bitdepth, **format_params)

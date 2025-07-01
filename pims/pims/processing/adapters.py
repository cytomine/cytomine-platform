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

from typing import Callable, Dict, Tuple, Type, Union

import numpy as np
from PIL import Image as PILImage
from pyvips import Image as VIPSImage
from pyvips.vimage import FORMAT_TO_TYPESTR


def numpy_to_vips(
    np_array: np.ndarray,
) -> VIPSImage:
    """
    Convert a Numpy array to a VIPS image.

    Parameters
    ----------
    np_array : array-like
        Numpy array to convert.
        If 1D, it is expected it contains flattened image data.

    Returns
    -------
    image
        VIPS image representation of the array

    Raises
    ------
    ValueError
        If it is impossible to convert provided array.
    """
    return VIPSImage.new_from_array(np_array)


def vips_to_numpy(vips_image: VIPSImage) -> np.ndarray:
    """
    Convert a VIPS image to a Numpy array.

    Parameters
    ----------
    vips_image : VIPSImage
        VIPS image to convert

    Returns
    -------
    image
        Array representation of VIPS image.
        Shape is always (height, width, bands).
    """
    return np.frombuffer(
        vips_image.write_to_memory(), dtype=FORMAT_TO_TYPESTR[vips_image.format]
    ).reshape(vips_image.height, vips_image.width, vips_image.bands)


def numpy_to_pil(np_array: np.ndarray) -> PILImage.Image:
    """
    Convert a Numpy array to a Pillow image.

    Parameters
    ----------
    np_array
        Numpy array to convert

    Returns
    -------
    image
        Pillow image representation of the array
    """
    return PILImage.fromarray(np_array)


def pil_to_numpy(pil_image: PILImage.Image) -> np.ndarray:
    """
    Convert a Pillow image to a Numpy array.

    Parameters
    ----------
    pil_image : PILImage
        Pillow image to convert

    Returns
    -------
    image
        Array representation of Pillow image.
    """
    return np.asarray(pil_image)  # noqa


def pil_to_vips(pil_image: PILImage.Image) -> VIPSImage:
    """
    Convert a Pillow image to a VIPS image.
    Potentially slow as conversion is 2-step,
    with numpy used as intermediate.

    Parameters
    ----------
    pil_image : PILImage.Image
        Pillow image to convert

    Returns
    -------
    image
        VIPS image
    """
    return VIPSImage.new_from_array(pil_image)


def vips_to_pil(vips_image: VIPSImage) -> PILImage.Image:
    """
    Convert a VIPS image to a Pillow image.
    Potentially slow as conversion is 2-step,
    with numpy used as intermediate.

    Parameters
    ----------
    vips_image
        Vips image to convert

    Returns
    -------
    image
        Pillow image
    """
    return numpy_to_pil(vips_to_numpy(vips_image))


def identity(v):
    return v


RawImagePixels = Union[np.ndarray, VIPSImage, PILImage.Image]
RawImagePixelsType = Union[Type[np.ndarray], Type[VIPSImage], Type[PILImage.Image]]

imglib_adapters: Dict[Tuple[RawImagePixelsType, RawImagePixelsType], Callable] = {
    (np.ndarray, VIPSImage): numpy_to_vips,
    (np.ndarray, PILImage.Image): numpy_to_pil,
    (np.ndarray, np.ndarray): identity,
    (PILImage.Image, VIPSImage): pil_to_vips,
    (PILImage.Image, np.ndarray): pil_to_numpy,
    (PILImage.Image, PILImage.Image): identity,
    (VIPSImage, np.ndarray): vips_to_numpy,
    (VIPSImage, PILImage.Image): vips_to_pil,
    (VIPSImage, VIPSImage): identity
}


def convert_to(
    image: RawImagePixels, new_image_type: RawImagePixelsType
) -> RawImagePixels:
    """
    Convert a convertible image (pixels) to a new convertible image type.

    Parameters
    ----------
    image
        Convertible image (pixels)
    new_image_type
        New convertible image type

    Returns
    -------
    converted
        The image (pixels) in the new type
    """
    return imglib_adapters.get((type(image), new_image_type))(image)

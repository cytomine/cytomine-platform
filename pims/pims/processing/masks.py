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

from typing import Tuple

import numpy as np
from rasterio.features import rasterize
from shapely.affinity import affine_transform
from shapely.geometry.base import BaseGeometry

from pims.api.utils.models import PointCross
from pims.processing.annotations import (
    ParsedAnnotation, ParsedAnnotations, contour,
    stretch_contour
)
from pims.utils.color import np_int2rgb
from pims.utils.dtypes import dtype_to_bits
from pims.utils.iterables import find_first_available_int
from pims.utils.math import max_intensity


def transparency_mask(
    mask: np.ndarray, bg_transparency: int, dtype: np.dtype
) -> np.ndarray:
    mi = max_intensity(dtype_to_bits(dtype))
    if mask.ndim == 3:
        mask = mask[:, :, 0]
    mask = mask.astype(dtype)
    mask[mask > 0] = 1 * mi
    mask[mask == 0] = (1 - bg_transparency / 100) * mi
    return mask


def draw_condition_mask(draw: np.ndarray, rgb_int_background: int) -> np.ndarray:
    """
    True -> image
    False -> drawing
    """
    if draw.ndim == 3:
        bg = np_int2rgb(rgb_int_background)
        return np.all(draw == np.asarray(bg), axis=-1).astype(np.uint8)
    else:
        mask = np.ones_like(draw, dtype=np.uint8)
        mask[draw != rgb_int_background] = 0
        return mask


def rescale_draw(draw: np.ndarray, dtype: np.dtype) -> np.ndarray:
    bitdepth = dtype_to_bits(dtype)
    if bitdepth > 8:
        draw = draw.astype(float)
        draw /= 255
        draw *= max_intensity(bitdepth)

    draw = draw.astype(dtype)
    return draw


def rasterize_mask(
    annots: ParsedAnnotations, affine: np.ndarray, out_width: int, out_height: int
) -> np.ndarray:
    """
    Rasterize annotations to a mask.
    """
    def _to_shape(
        annot: ParsedAnnotation, is_grayscale: bool = True
    ) -> Tuple[BaseGeometry, int]:
        geometry = affine_transform(annot.geometry, affine)
        if is_grayscale:
            value = annot.fill_color.as_rgb_tuple()[0]
        else:
            value = annot.fill_color.as_int()
        return geometry, value

    out_shape = (out_height, out_width)
    dtype = np.uint8 if annots.is_fill_grayscale else np.uint32

    def shape_generator():
        for annot in annots:
            yield _to_shape(annot, annots.is_fill_grayscale)

    rasterized = rasterize(
        shape_generator(), out_shape=out_shape, dtype=dtype
    )
    if not annots.is_grayscale:
        return np_int2rgb(rasterized)
    return rasterized


def rasterize_draw(
    annots: ParsedAnnotations, affine: np.ndarray, out_width: int, out_height: int,
    point_style: PointCross
) -> Tuple[np.ndarray, int]:
    """
    Rasterize annotations contours.
    """
    out_shape = (out_height, out_width)

    def _contour_width(stroke_width: int) -> int:
        return round(stroke_width * (0.75 + max(out_shape) / 1000))

    def _to_shape(
        annot: ParsedAnnotation, is_grayscale: bool = True
    ) -> Tuple[BaseGeometry, int]:
        width = _contour_width(annot.stroke_width)
        geometry = stretch_contour(
            affine_transform(
                contour(annot.geometry, point_style=point_style),
                affine
            ), width=width
        )
        value = annot.stroke_color.as_rgb_tuple()[
            0] if is_grayscale else annot.stroke_color.as_int()
        return geometry, value

    dtype = np.uint8 if annots.is_stroke_grayscale else np.uint32

    def shape_generator():
        for annot in annots:
            if not annot.stroke_color:
                continue
            yield _to_shape(
                annot, annots.is_stroke_grayscale
            )

    def background_color() -> int:
        """
        Find an integer to use for background (cannot be 0 if one of stroke
        color is black).
        """
        if annots.is_stroke_grayscale:
            values = [
                a.stroke_color.as_rgb_tuple()[0]
                for a in annots if a.stroke_color
            ]
            return find_first_available_int(values, 0, 65536)
        else:
            values = [a.stroke_color.as_int() for a in annots if a.stroke_color]
            return find_first_available_int(values, 0, 4294967296)

    bg = background_color()
    try:
        rasterized = rasterize(
            shape_generator(), out_shape=out_shape, dtype=dtype, fill=bg
        )
    except ValueError:
        # No valid geometry objects found for rasterize
        rasterized = np.full(out_shape, bg)
    if not annots.is_grayscale:
        return np_int2rgb(rasterized), bg
    return rasterized, bg

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
from collections.abc import MutableSequence
from math import floor
from typing import Optional, Tuple

import numpy as np
from shapely.geometry import GeometryCollection, LineString, Point
from shapely.geometry.base import BaseGeometry

from pims.api.utils.models import PointCross
from pims.files.file import Image
from pims.processing.region import Region
from pims.utils.color import Color


class ParsedAnnotation:
    """
    An input annotation
    """

    def __init__(
        self, geometry: BaseGeometry, fill_color: Optional[Color] = None,
        stroke_color: Optional[Color] = None, stroke_width: int = None,
        point_envelope_length: float = 1
    ):
        self.geometry = geometry
        self.fill_color = fill_color
        self.stroke_color = stroke_color
        self.stroke_width = stroke_width

        self.custom_bounds = None
        if self.geometry.geom_type == 'Point' and point_envelope_length is not None:
            pt = self.geometry
            length = point_envelope_length / 2
            self.custom_bounds = (
                pt.x - length, pt.y - length,  # noqa
                pt.x + length, pt.y + length  # noqa
            )

    @property
    def is_fill_grayscale(self) -> bool:
        return self.fill_color.is_grayscale() if self.fill_color else True

    @property
    def is_stroke_grayscale(self) -> bool:
        return self.stroke_color.is_grayscale() if self.stroke_color else True

    @property
    def is_grayscale(self) -> bool:
        return self.is_fill_grayscale and self.is_stroke_grayscale

    @property
    def bounds(self) -> Tuple[float, float, float, float]:
        """
        Returns a (minx, miny, maxx, maxy) tuple (float values)
        that bounds the object.
        Ported from Shapely.
        """
        return self.custom_bounds \
            if self.custom_bounds \
            else self.geometry.bounds

    @property
    def region(self) -> Region:
        left, top, right, bottom = self.bounds
        return Region(top, left, right - left, bottom - top)

    def __eq__(self, other) -> bool:
        return isinstance(other, ParsedAnnotation) \
               and self.geometry.equals(other.geometry) \
               and self.fill_color == other.fill_color \
               and self.stroke_color == other.stroke_color \
               and self.stroke_width == other.stroke_width

    def __str__(self):
        return f"Annotation {self.geometry.wkt} " \
            f"| Fill: {self.fill_color} " \
            f"| Stroke: {self.stroke_color}({self.stroke_width})"


class ParsedAnnotations(MutableSequence):
    def __init__(self):
        self._data = []

    def insert(self, index: int, value: ParsedAnnotation):
        if not isinstance(value, ParsedAnnotation):
            raise TypeError(
                f"Value of type {value.__class__.__name__} "
                f"not allowed in {self.__class__.__name__}."
            )
        self._data.insert(index, value)

    def __len__(self):
        return len(self._data)

    def __getitem__(self, index):
        return self._data[index]

    def __setitem__(self, index, value):
        if not isinstance(value, ParsedAnnotation):
            raise TypeError(
                f"Value of type {value.__class__.__name__} "
                f"not allowed in {self.__class__.__name__}."
            )
        self._data[index] = value

    def __delitem__(self, index):
        del self._data[index]

    @property
    def is_fill_grayscale(self) -> bool:
        return all(annot.is_fill_grayscale for annot in self._data)

    @property
    def is_stroke_grayscale(self) -> bool:
        return all(annot.is_stroke_grayscale for annot in self._data)

    @property
    def is_grayscale(self) -> bool:
        return all(annot.is_grayscale for annot in self._data)

    @property
    def bounds(self) -> Tuple[float, float, float, float]:
        """
        Returns a (minx, miny, maxx, maxy) tuple (float values)
        that bounds the whole collection.
        """
        bounds = np.asarray([annot.bounds for annot in self._data])
        mini = np.min(bounds, axis=0)
        maxi = np.max(bounds, axis=0)
        return mini[0], mini[1], maxi[2], maxi[3]

    @property
    def region(self) -> Region:
        left, top, right, bottom = self.bounds
        return Region(top, left, right - left, bottom - top)

    def __str__(self):
        return '/'.join([str(i) for i in self._data])


def annotation_crop_affine_matrix(
    annot_region: Region, in_region: Region, out_width: int, out_height: int
) -> np.ndarray:
    """
    Compute affine transformation matrix to apply to annotations in the given
    region.
    """

    rx = out_width / in_region.true_width
    ry = out_height / in_region.true_height
    tx = -annot_region.left * rx + (annot_region.left - in_region.true_left) * rx
    ty = -annot_region.top * ry + (annot_region.top - in_region.true_top) * ry
    return np.asarray([rx, 0, 0, ry, tx, ty])


def contour(
    geom: BaseGeometry, point_style: PointCross = PointCross.CROSS
) -> BaseGeometry:
    """
    Extract geometry's contour.

    Parameters
    ----------
    geom : shapely.geometry.Geometry
        Geometry which contour is extracted from.
    point_style : PointCross
        Style of contour for points.

    Returns
    -------
    contour = shapely.geometry
        Contour
    """
    if isinstance(geom, Point):
        x, y = geom.x, geom.y

        def center_coord(coord):
            if coord % 1 < 0.5:
                return floor(coord) + 0.5
            return coord

        x, y = center_coord(x), center_coord(y)

        if point_style == PointCross.CIRCLE:
            return Point(x, y).buffer(6).boundary
        elif point_style == PointCross.CROSSHAIR:
            circle = Point(x, y).buffer(6).boundary
            left_line = LineString([(x - 10, y), (x - 3, y)])
            right_line = LineString([(x + 3, y), (x + 10, y)])
            top_line = LineString([(x, y - 10), (x, y - 3)])
            bottom_line = LineString([(x, y + 3), (x, y + 10)])
            return GeometryCollection(
                [circle, left_line, right_line, top_line, bottom_line]
            )
        elif point_style == PointCross.CROSS:
            horizontal = LineString([(x - 10, y), (x + 10, y)])
            vertical = LineString([(x, y - 10), (x, y + 10)])
            return GeometryCollection([horizontal, vertical])
    elif isinstance(geom, LineString):
        return geom
    else:
        return geom.boundary


def stretch_contour(geom: BaseGeometry, width: float = 1) -> BaseGeometry:
    """
    Stretch geometry (expected to be a geometry contour) to given width scale.
    """
    if width > 1 and geom:
        buf = 1 + (width - 1) / 10
        return geom.buffer(buf)
    return geom


def get_annotation_region(
    in_image: Image, annots: ParsedAnnotations, context_factor: float = 1.0,
    try_square: bool = False
) -> Region:
    """
    Get the region describing the rectangular envelope of all
    annotations multiplied by an optional context factor.

    Parameters
    ----------
    in_image
        Image in which region is extracted.
    annots
        List of parsed annotations
    context_factor
        Context factor
    try_square
        Try to adapt region's width or height to have a square region.
    Returns
    -------
    Region
    """

    # All computation are done in non normalized float.
    minx, miny, maxx, maxy = annots.bounds
    left = minx
    top = miny
    width = maxx - minx
    height = maxy - miny
    if context_factor and context_factor != 1.0:
        left -= width * (context_factor - 1) / 2.0
        top -= height * (context_factor - 1) / 2.0
        width *= context_factor
        height *= context_factor

    if try_square:
        if width < height:
            delta = height - width
            left -= delta / 2
            width += delta
        elif height < width:
            delta = width - height
            top -= delta / 2
            height += delta

    width = min(width, in_image.width)
    if left < 0:
        left = 0
    else:
        left = min(left, in_image.width - width)

    height = min(height, in_image.height)
    if top < 0:
        top = 0
    else:
        top = min(top, in_image.height - height)

    return Region(top, left, width, height)

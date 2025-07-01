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
from typing import Dict, Iterable, List, Optional

from shapely.affinity import affine_transform
from shapely.errors import ShapelyError
from shapely.validation import explain_validity, make_valid
from shapely.wkt import loads as wkt_loads

from pims.api.exceptions import InvalidGeometryException
from pims.api.utils.header import AnnotationOrigin
from pims.processing.annotations import ParsedAnnotation, ParsedAnnotations
from pims.utils.color import Color


def parse_annotations(
    annotations: Iterable[Dict], ignore_fields: Optional[List[str]] = None,
    default: Optional[Dict] = None, point_envelope_length: Optional[float] = None,
    origin: AnnotationOrigin = AnnotationOrigin.LEFT_TOP, im_height: Optional[int] = None
) -> ParsedAnnotations:
    """
    Parse a list of annotations.

    Parameters
    ----------
    annotations
        List of annotations, as defined in API spec.
    ignore_fields
        List of field names to ignore for parsing.
    default
        Default value for fields. Default value for missing fields is None.
    point_envelope_length
        Envelope length for Point geometries.
    origin
        The origin of coordinate system in which annotations are described
    im_height
        The image height for coordinate system transformation
        Mandatory if `origin` is `LEFT_BOTTOM`.

    Returns
    -------
    ParsedAnnotations
        A list of parsed annotations
    """

    al = ParsedAnnotations()
    for annotation in annotations:
        al.append(
            parse_annotation(
                **annotation, ignore_fields=ignore_fields,
                default=default, point_envelope_length=point_envelope_length,
                origin=origin, im_height=im_height
            )
        )

    return al


def parse_annotation(
    geometry: str, fill_color: Optional[Color] = None, stroke_color: Optional[Color] = None,
    stroke_width: Optional[int] = None, ignore_fields: Optional[List[str]] = None,
    default: Optional[Dict] = None, point_envelope_length: float = 1.0,
    origin: AnnotationOrigin = AnnotationOrigin.LEFT_TOP, im_height: Optional[int] = None
) -> ParsedAnnotation:
    """
    Parse an annotation.

    Parameters
    ----------
    geometry
        WKT string to parse (parsed geometry can be invalid)
    fill_color
        Fill color to parse
    stroke_color
        Stroke color to parse
    stroke_width
        Stroke width to parse
    ignore_fields
        List of field names to ignore for parsing.
    default
        Default value for fields. Default value for missing fields is None.
    point_envelope_length
        Envelope length for Point geometries.
    origin
        The origin of coordinate system in which annotations are described
    im_height
        The image height for coordinate system transformation
        Mandatory if `origin` is `LEFT_BOTTOM`.

    Returns
    -------
    ParsedAnnotation
        A parsed annotation

    Raises
    ------
    BadRequestProblem
        If geometry is unreadable or invalid, even after trying to make it valid.
    """
    if ignore_fields is None:
        ignore_fields = []

    if default is None:
        default = dict()

    try:
        geom = wkt_loads(geometry)
    except ShapelyError:
        raise InvalidGeometryException(geometry, "WKT reading error")

    if origin == 'LEFT_BOTTOM':
        geom = affine_transform(geom, [1, 0, 0, -1, 0, im_height - 0.5])

    if not geom.is_valid:
        geom = make_valid(geom)

    if not geom.is_valid:
        raise InvalidGeometryException(geometry, explain_validity(geom))
    parsed = {'geometry': geom}

    if geom.geom_type == 'Point' and point_envelope_length is not None:
        parsed['point_envelope_length'] = point_envelope_length

    if 'fill_color' not in ignore_fields:
        default_color = default.get('fill_color')
        parsed['fill_color'] = fill_color \
            if fill_color is not None else default_color

    if 'stroke_color' not in ignore_fields:
        default_color = default.get('stroke_color')
        parsed['stroke_color'] = stroke_color \
            if stroke_color is not None else default_color

    if 'stroke_width' not in ignore_fields:
        parsed['stroke_width'] = stroke_width \
            if stroke_width is not None else default.get('stroke_width')

    return ParsedAnnotation(**parsed)


def is_wkt(value: str) -> bool:
    """
    Whether a value is a Well-Known Text string.
    The underlying geometry validity is not checked.

    Parameters
    ----------
    value : str
        Value expected to be a WKT string.

    Returns
    -------
    bool
        Whether the value is a WKT string or not
    """
    try:
        wkt_loads(str(value))
        return True
    except ShapelyError:
        return False

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

from shapely.geometry import Point

from pims.api.utils.annotation_parameter import is_wkt, parse_annotation
from pims.processing.annotations import ParsedAnnotation
from pims.utils.color import Color


def test_parse_annotation():
    annot = {"geometry": "POINT (10 10)"}
    assert parse_annotation(**annot) == ParsedAnnotation(Point(10, 10))

    red = Color("red")
    white = Color("white")

    annot = {"geometry": "POINT (10 10)", "fill_color": white}
    default = {"fill_color": red, "stroke_color": white}

    assert parse_annotation(
        **annot, default=default
    ) == ParsedAnnotation(Point(10, 10), white, white)
    assert parse_annotation(**annot) == ParsedAnnotation(Point(10, 10), white)
    assert parse_annotation(
        **annot, ignore_fields=['fill_color']
    ) == ParsedAnnotation(Point(10, 10))
    assert parse_annotation(
        **annot, ignore_fields=['fill_color'],
        default=default
    ) == ParsedAnnotation(Point(10, 10), stroke_color=white)


def test_is_wkt():
    assert is_wkt('POINT(10 10)') is True
    assert is_wkt('POINT()') is False

    # Valid WKT, invalid geometry
    assert is_wkt('POLYGON ((0 0, 0 2, 1 1, 2 2, 2 0, 1 1, 0 0))') is True

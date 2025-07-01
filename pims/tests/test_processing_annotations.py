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

from shapely.geometry import box

from pims.processing.annotations import ParsedAnnotation, ParsedAnnotations, get_annotation_region
from pims.processing.region import Region
from pims.utils.color import Color


def test_annotation():
    geom = box(10, 20, 30, 40)
    annot = ParsedAnnotation(geom)
    assert annot.is_fill_grayscale is True
    assert annot.is_stroke_grayscale is True
    assert annot.bounds == (10, 20, 30, 40)

    grey = Color("grey")
    red = Color("red")
    annot = ParsedAnnotation(geom, fill_color=red, stroke_color=grey)
    assert annot.is_fill_grayscale is False
    assert annot.is_stroke_grayscale is True
    assert annot.is_grayscale is False

    annot = ParsedAnnotation(geom, fill_color=grey, stroke_color=grey)
    assert annot.is_grayscale is True


def test_annotation_list():
    grey = Color("grey")
    white = Color("white")
    red = Color("red")

    annot1 = ParsedAnnotation(box(10, 20, 30, 40), fill_color=white, stroke_color=red)
    annot2 = ParsedAnnotation(box(5, 100, 20, 200), fill_color=grey, stroke_color=white)

    al = ParsedAnnotations()
    al.append(annot1)
    al.append(annot2)

    assert len(al) == 2
    assert al[1] == annot2
    assert al.is_fill_grayscale is True
    assert al.is_stroke_grayscale is False
    assert al.is_grayscale is False
    assert al.bounds == (5, 20, 30, 200)


def test_annotation_region():
    class FakeImage:
        def __init__(self, w, h):
            self.width = w
            self.height = h

    al = ParsedAnnotations()
    al.append(ParsedAnnotation(box(10, 20, 30, 40)))
    assert get_annotation_region(
        FakeImage(100, 100), al
    ) == Region(20, 10, 20, 20)
    assert get_annotation_region(
        FakeImage(100, 100), al, context_factor=1.5
    ) == Region(15, 5, 30, 30)

    al = ParsedAnnotations()
    al.append(ParsedAnnotation(box(10, 20, 30, 30)))
    assert get_annotation_region(
        FakeImage(100, 100), al, try_square=True
    ) == Region(15, 10, 20, 20)

    al = ParsedAnnotations()
    al.append(ParsedAnnotation(box(20, 10, 30, 30)))
    assert get_annotation_region(
        FakeImage(100, 100), al, try_square=True
    ) == Region(10, 15, 20, 20)

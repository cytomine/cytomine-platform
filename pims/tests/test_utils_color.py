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
import numpy as np

from pims.utils.color import Color, np_int2rgb


def test_rgb_int_conversion():
    assert np.array_equal(
        Color(Color((10, 255, 0)).as_int()).as_rgb_tuple(alpha=False),
        (10, 255, 0)
    )


def test_rgba_int_conversion():
    assert np.allclose(
        Color(Color((10, 255, 0, 0.5)).as_int()).as_rgb_tuple(alpha=True),
        (10, 255, 0, 0.5),
        atol=1e-2
    )


def test_np_int2rgb():
    a = np_int2rgb(np.array([1684300800]))
    assert np.array_equal(a, np.array([100, 100, 100]))

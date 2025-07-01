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

from pims.formats.utils.structures.planes import PlanesInfo


def test_plane_info():
    pi = PlanesInfo(3, 5, 1, ['index'], [np.int64])
    pi.set(0, 0, 0, index=2)
    assert pi.get(0, 0, 0, 'index') == 2
    assert pi.get(0, 0, 0, 'invalid') is None

    pi = PlanesInfo(3, 5, 1)
    assert pi.get(0, 0, 0, 'invalid') is None

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

import pint

from pims.api.utils.response import convert_quantity, response_list


def test_response_list():
    items = list()
    resp = response_list(items)
    assert resp == dict(items=[], size=0)

    items = ["a", "b"]
    resp = response_list(items)
    assert resp == dict(items=items, size=len(items))


def test_convert_quantity():
    assert convert_quantity(None, 'meters') is None
    assert convert_quantity(3, 'meters') == 3

    ureg = pint.UnitRegistry()
    assert convert_quantity(3 * ureg('cm'), 'meters') == 0.03

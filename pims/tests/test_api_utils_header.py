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
from pims.api.utils.header import add_image_size_limit_header, serialize_header


def test_serialize_header():
    assert serialize_header(5) == str(5)
    assert serialize_header([1, 2]) == '1,2'
    assert serialize_header(dict(a=2, b='c'), explode=True) == 'a=2,b=c'
    assert serialize_header(dict(a=2, b='c'), explode=False) == 'a,2,b,c'


def test_add_image_size_limit_header():
    assert len(add_image_size_limit_header(dict(), 100, 100, 100, 100)) == 0
    assert len(add_image_size_limit_header(dict(), 100, 100, 50, 50)) == 1

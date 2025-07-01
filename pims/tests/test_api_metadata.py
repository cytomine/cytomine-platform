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
import pytest

from tests.conftest import fake_files_info


@pytest.mark.parametrize("ff", fake_files_info)
def test_file(client, fake_files, ff):
    response = client.get(f"/file/{ff['filepath']}/info")
    assert response.status_code == 200

    json = response.json()
    assert json["size"] == 0
    assert json["stem"] == ff['filepath'].split("/")[-1].split(".")[0]
    assert json["role"] in [ff['role'].upper().replace("VISUALISATION", "SPATIAL"), "NONE"]
    assert json["file_type"] == ("COLLECTION" if ff['collection'] else "SINGLE")
    assert json["is_symbolic"] == (ff['filetype'] == 'l')


def test_file_not_exists(client):
    response = client.get("/file/abc/info")
    assert response.status_code == 404


@pytest.mark.skip(reason="Can only work with true files, need to implement this")
@pytest.mark.parametrize("ff", fake_files_info)
def test_image(client, fake_files, ff):
    response = client.get(f"/image/{ff['filepath']}/info/image")
    assert response.status_code == (200 if not ff['collection'] else 404)

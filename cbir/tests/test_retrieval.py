#  Copyright 2023 Cytomine ULiège
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""API tests"""

import os

from fastapi.testclient import TestClient

from cbir.app import app
from cbir.config import DatabaseSetting


def test_index_image() -> None:
    """Test image indexing."""

    database_settings = DatabaseSetting.get_settings()

    with open("tests/data/image.png", "rb") as image:
        files = {"image": image.read()}

    with TestClient(app) as client:
        response = client.post("/api/images/index", files=files)

    assert response.status_code == 200
    assert os.path.isfile(database_settings.filename) is True


def test_retrieve_image() -> None:
    """Test image retrieval."""

    with open("tests/data/image.png", "rb") as image:
        files = {"image": image.read()}

    with TestClient(app) as client:
        response = client.post(
            "/api/images/retrieve",
            data={"nrt_neigh": "10"},
            files=files,
        )

    assert response.status_code == 200

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

    with TestClient(app) as client, open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images/index",
            files={"image": image},
        )

    assert response.status_code == 200
    assert os.path.isfile(database_settings.get_database_path()) is True


def test_retrieve_one_image() -> None:
    """Test image retrieval for one image."""

    with open("tests/data/image.png", "rb") as image:
        files = {"image": image.read()}

    with TestClient(app) as client:
        response = client.post(
            "/api/images/retrieve",
            data={"nrt_neigh": "1"},
            files=files,
        )

    data = response.json()

    assert response.status_code == 200
    assert "distances" in data
    assert "filenames" in data
    assert isinstance(data["distances"], list)
    assert isinstance(data["filenames"], list)
    assert len(data["distances"]) == 1
    assert len(data["filenames"]) == 1


def test_remove_image_not_found() -> None:
    """Test remove an image that do not exist in the database."""

    with TestClient(app) as client:
        response = client.delete(
            "/api/images/remove",
            params={"filename": "notfound.png"},
        )

    assert response.status_code == 404


def test_remove_image() -> None:
    """Test remove an image from the database."""

    with TestClient(app) as client:
        response = client.delete(
            "/api/images/remove",
            params={"filename": "image.png"},
        )

    assert response.status_code == 200


def test_retrieve_image() -> None:
    """Test image retrieval."""

    with TestClient(app) as client, open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images/retrieve",
            data={"nrt_neigh": "10"},
            files={"image": image},
        )

    data = response.json()

    assert response.status_code == 200
    assert "distances" in data
    assert "filenames" in data
    assert isinstance(data["distances"], list)
    assert isinstance(data["filenames"], list)

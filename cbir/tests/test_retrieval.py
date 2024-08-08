"""API tests"""

from fastapi.testclient import TestClient


def test_index_image(client: TestClient) -> None:
    """
    Test 'POST /api/images' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    index_name = "test_index"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": storage_name, "index": index_name},
        )

    assert response.status_code == 200
    assert response.json() == {
        "ids": [0],
        "storage": storage_name,
        "index": index_name,
    }


def test_index_image_with_same_filename(client: TestClient) -> None:
    """
    Test 'POST /api/images' endpoint with the same filename.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    index_name = "test_index"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": storage_name, "index": index_name},
        )
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": storage_name, "index": index_name},
        )

    assert response.status_code == 409
    assert response.json() == {"detail": "Image filename already exist!"}


def test_index_image_with_no_storage(client: TestClient) -> None:
    """
    Test 'POST /api/images' endpoint with no storage.

    Args:
        client: A test client instance used to send requests to the application.
    """

    index_name = "test_index"
    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": "", "index": index_name},
        )

    assert response.status_code == 404
    assert response.json() == {"detail": "Storage is required"}


def test_remove_image_not_found(client: TestClient) -> None:
    """
    Test 'DELETE /api/images/{filename}' endpoint where the image does not exist.

    Args:
        client: A test client instance used to send requests to the application.
    """

    filename = "notfound.png"
    storage_name = "test_storage"
    index_name = "test_index"
    response = client.delete(
        f"/api/images/{filename}",
        params={
            "storage": storage_name,
            "index": index_name,
        },
    )

    assert response.status_code == 404, response.json()
    assert response.json() == {"detail": f"{filename} not found"}


def test_remove_image(client: TestClient) -> None:
    """
    Test 'DELETE /api/images/{filename}' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    filename = "image.png"
    storage_name = "test_storage"
    index_name = "test_index"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    with open(f"tests/data/{filename}", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": storage_name, "index": index_name},
        )
    assert response.status_code == 200

    response = client.delete(
        f"/api/images/{filename}",
        params={
            "storage": storage_name,
            "index": index_name,
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "id": 0,
        "storage": storage_name,
        "index": index_name,
    }


def test_retrieve_one_image(client: TestClient) -> None:
    """
    Test 'POST /api/images/retrieve' for one neighbour.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    index_name = "test_index"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images",
            files={"image": image},
            params={"storage": storage_name, "index": index_name},
        )
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images/retrieve",
            files={"image": image},
            params={
                "nrt_neigh": "1",
                "storage": storage_name,
                "index": index_name,
            },
        )

    data = response.json()

    assert response.status_code == 200
    assert "distances" in data
    assert "filenames" in data
    assert isinstance(data["distances"], list)
    assert isinstance(data["filenames"], list)
    assert len(data["distances"]) == 1
    assert len(data["filenames"]) == 1


def test_retrieve_image(client: TestClient) -> None:
    """
    Test 'POST /api/images/retrieve' for several neighbours.

    Args:
        client: A test client instance used to send requests to the application.
    """

    nrt_neigh = 10
    storage_name = "test_storage"
    index_name = "test_index"
    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/images/retrieve",
            params={
                "nrt_neigh": nrt_neigh,
                "storage": storage_name,
                "index": index_name,
            },
            files={"image": image},
        )

    data = response.json()

    assert response.status_code == 200
    assert "distances" in data
    assert "filenames" in data
    assert isinstance(data["distances"], list)
    assert isinstance(data["filenames"], list)

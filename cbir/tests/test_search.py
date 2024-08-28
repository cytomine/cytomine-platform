"""Search tests"""

from fastapi.testclient import TestClient


def test_search_one_image(client: TestClient) -> None:
    """
    Test 'POST /api/search' for one neighbour.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    index_name = "test_index"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as file:
        response = client.post(
            "/api/images",
            files={"image": file},
            params={"storage": storage_name, "index": index_name},
        )
    assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/search",
            files={"image": image},
            params={
                "nrt_neigh": "1",
                "storage": storage_name,
                "index": index_name,
            },
        )

    data = response.json()

    assert response.status_code == 200
    assert "similarities" in data
    assert isinstance(data["similarities"], list)
    assert len(data["similarities"]) == 1


def test_search_one_image_with_storages(client: TestClient) -> None:
    """
    Test 'POST /api/search' for one neighbour with several storages.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storages = ["test_storage1", "test_storage2"]
    index_name = "test_index"

    for storage in storages:
        response = client.post("/api/storages", json={"name": storage})
        assert response.status_code == 200

        with open("tests/data/image.png", "rb") as file:
            response = client.post(
                "/api/images",
                files={"image": file},
                params={"storage": storage, "index": index_name},
            )
        assert response.status_code == 200

    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/search",
            files={"image": image},
            params={
                "nrt_neigh": "1",
                "storage": storages,
                "index": index_name,
            },
        )

    data = response.json()

    assert response.status_code == 200
    assert "similarities" in data
    assert isinstance(data["similarities"], list)
    assert len(data["similarities"]) == 1


def test_search_images(client: TestClient) -> None:
    """
    Test 'POST /api/search' for several neighbours.

    Args:
        client: A test client instance used to send requests to the application.
    """

    nrt_neigh = 10
    storage_name = "test_storage"
    index_name = "test_index"
    with open("tests/data/image.png", "rb") as image:
        response = client.post(
            "/api/search",
            params={
                "nrt_neigh": nrt_neigh,
                "storage": storage_name,
                "index": index_name,
            },
            files={"image": image},
        )

    data = response.json()

    assert response.status_code == 200
    assert "similarities" in data
    assert isinstance(data["similarities"], list)

"""Storage tests"""

from fastapi.testclient import TestClient


def test_get_storages(client: TestClient) -> None:
    """
    Test 'GET /api/storages' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    response = client.get("/api/storages")
    assert response.status_code == 200
    assert response.json() == []


def test_create_storage(client: TestClient) -> None:
    """
    Test 'POST /api/storages' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200
    assert response.json() == {"message": f"Created storage with name: {storage_name}"}

    response = client.get("/api/storages")
    assert response.status_code == 200
    assert storage_name in response.json()


def test_create_existing_storage(client: TestClient) -> None:
    """
    Test 'POST /api/storages/{name}' endpoint for an existing storage.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 409
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' already exists."
    }


def test_get_storage(client: TestClient) -> None:
    """
    Test 'GET /api/storages/{name}' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    response = client.get(f"/api/storages/{storage_name}")
    assert response.status_code == 200
    assert response.json() == {"name": storage_name}


def test_get_storage_not_found(client: TestClient) -> None:
    """
    Test 'GET /api/storages/{name}' endpoint for non-existent storage.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "non_existent_storage"

    response = client.get(f"/api/storages/{storage_name}")
    assert response.status_code == 404
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' not found."
    }


def test_delete_storage(client: TestClient) -> None:
    """
    Test 'DELETE /api/storages/{name}' endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    response = client.post("/api/storages", json={"name": storage_name})
    assert response.status_code == 200

    response = client.delete(f"/api/storages/{storage_name}")
    assert response.status_code == 200
    assert response.json() == {"message": f"Deleted storage with name: {storage_name}"}

    response = client.get(f"/api/storages/{storage_name}")
    assert response.status_code == 404


def test_delete_storage_not_found(client: TestClient) -> None:
    """
    Test 'DELETE /api/storages/{name}' endpoint where the storage does not exist.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "non_existent_storage"

    response = client.delete(f"/api/storages/{storage_name}")
    assert response.status_code == 404
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' not found."
    }

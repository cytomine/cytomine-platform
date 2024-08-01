"""Storage tests"""

import shutil
import tempfile
from typing import Any, Generator

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from cbir.api.storages import router


@pytest.fixture
def app() -> Generator[Any, Any, Any]:
    """
    Create and provide a FastAPI application instance for testing.

    The application is configured with:
    - A router included in the FastAPI instance.
    - A mock database with temporary settings stored in a temporary directory.

    Yields:
        local_app: The FastAPI app instance with router and mock database.
    """

    local_app = FastAPI()
    local_app.include_router(router)

    settings = type("Settings", (object,), {"data_path": tempfile.mkdtemp()})
    local_app.state.database = type("Database", (object,), {"settings": settings})

    yield local_app

    # Cleanup after tests
    shutil.rmtree(local_app.state.database.settings.data_path)  # type: ignore


@pytest.fixture
def client(app: FastAPI) -> TestClient:
    """
    Provide a test client for the FastAPI application.

    Args:
        app (FastAPI): The FastAPI application instance to be tested.

    Returns:
        TestClient: An instance of `TestClient` that can be used to send
        requests to the FastAPI application and inspect the responses.
    """
    return TestClient(app)


def test_get_storages(client: TestClient) -> None:
    """
    Test the GET /storages endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    response = client.get("/storages")
    assert response.status_code == 200
    assert response.json() == []


def test_create_storage(client: TestClient) -> None:
    """
    Test the POST /storages endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"

    response = client.post("/storages", json={"name": storage_name})
    assert response.status_code == 200
    assert response.json() == {"message": f"Created storage with name: {storage_name}"}

    response = client.get("/storages")
    assert response.status_code == 200
    assert storage_name in response.json()


def test_get_storage(client: TestClient) -> None:
    """
    Test the GET /storages/{name} endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    client.post("/storages", json={"name": storage_name})

    response = client.get(f"/storages/{storage_name}")
    assert response.status_code == 200
    assert response.json() == {"name": storage_name}


def test_get_storage_not_found(client: TestClient) -> None:
    """
    Test the GET /storages/{name} endpoint for non-existent storage.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "non_existent_storage"

    response = client.get(f"/storages/{storage_name}")
    assert response.status_code == 404
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' not found."
    }


def test_create_existing_storage(client: TestClient) -> None:
    """
    Test the POST /storages/{name} endpoint for an existing storage.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    client.post("/storages", json={"name": storage_name})

    response = client.post("/storages", json={"name": storage_name})
    assert response.status_code == 409
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' already exists."
    }


def test_delete_storage(client: TestClient) -> None:
    """
    Test the DELETE /storages/{name} endpoint.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "test_storage"
    client.post("/storages", json={"name": storage_name})

    response = client.delete(f"/storages/{storage_name}")
    assert response.status_code == 200
    assert response.json() == {"message": f"Deleted storage with name: {storage_name}"}

    response = client.get(f"/storages/{storage_name}")
    assert response.status_code == 404


def test_delete_storage_not_found(client: TestClient) -> None:
    """
    Test the DELETE /storages/{name} endpoint when the storage does not exist.

    Args:
        client: A test client instance used to send requests to the application.
    """

    storage_name = "non_existent_storage"

    response = client.delete(f"/storages/{storage_name}")
    assert response.status_code == 404
    assert response.json() == {
        "detail": f"Storage with name '{storage_name}' not found."
    }

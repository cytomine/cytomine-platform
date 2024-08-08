"""Pytest configuration and fixtures for the test suite."""

import shutil
import tempfile
from typing import Generator

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from redis import Redis  # type: ignore

from cbir import app as main
from cbir import config


@pytest.fixture(scope="function", autouse=True)
def redis_client() -> Generator[Redis, None, None]:
    """
    Provide a Redis client for testing and clean up afterward.

    Yields:
        Redis: A Redis client instance.
    """

    client = Redis(host="localhost", port=6379, db=1)
    yield client
    client.flushdb()


@pytest.fixture(scope="function")
def test_directory() -> Generator[str, None, None]:
    """
    Provide a temporary directory for testing and clean up afterward.

    Yields:
        str: The path to the temporary directory.
    """

    tmp_directory = tempfile.mkdtemp()
    yield tmp_directory
    shutil.rmtree(tmp_directory)


def get_settings(test_directory: str) -> config.Settings:
    """
    Get the tests settings.

    Args:
        test_directory (str): The path to the temporary directory.

    Returns:
        (Settings): The test environment settings.
    """
    return config.Settings(data_path=test_directory, db=1)


@pytest.fixture
def app(test_directory: str) -> FastAPI:
    """
    Create and provide a FastAPI application instance for testing.

    Args:
        test_directory (str): The path to the temporary directory.

    Returns:
        app: The FastAPI app instance to be tested.
    """

    main.app.dependency_overrides[config.get_settings] = lambda: get_settings(
        test_directory
    )

    main.app.state.model = main.load_model(get_settings(test_directory))

    return main.app


@pytest.fixture
def client(app: FastAPI) -> TestClient:
    """
    Provide a test client for the FastAPI application.

    Args:
        app (FastAPI): The FastAPI application instance to be tested.

    Returns:
        TestClient: An instance of `TestClient`.
    """
    return TestClient(app)

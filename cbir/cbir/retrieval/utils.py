"""Utility functions for the retrieval module."""

from fastapi import Depends
from redis import Redis  # type: ignore

from cbir.config import Settings, get_settings
from cbir.retrieval.indexer import Indexer


def get_redis(settings: Settings = Depends(get_settings)) -> Redis:
    """
    Get the Redis client.

    Args:
        settings (Settings): The database settings.

    Returns:
        Redis: The Redis client.
    """
    return Redis(host=settings.host, port=settings.port, db=settings.db)


def get_indexer(
    data_path: str = "/data",
    n_features: int = 100,
    storage_name: str = "storage",
    index_name: str = "index",
    gpu: bool = False,
) -> Indexer:
    """
    Get the indexer.

    Args:
        data_path (str): Path to the base storage.
        n_features (int): Number of features in the index.
        storage_name (str): The name of the storage.
        index_name (str): The name of the index.
        gpu (bool): Whether to use GPU for indexing or not.

    Returns:
        Indexer: The indexer.
    """
    return Indexer(data_path, storage_name, index_name, n_features, gpu)

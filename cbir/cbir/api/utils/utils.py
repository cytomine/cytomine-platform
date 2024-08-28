"""Utility functions for dependency injection."""

from typing import List

from fastapi import Depends, Query
from redis import Redis  # type: ignore

from cbir.config import Settings, get_settings
from cbir.retrieval.indexer import Indexer
from cbir.retrieval.retrieval import ImageRetrieval
from cbir.retrieval.store import Store
from cbir.retrieval.utils import get_redis


def get_store(
    storage_name: str = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    redis: Redis = Depends(get_redis),
) -> Store:
    """
    Instantiate a Store object.

    Args:
        storage_name (str): The name of the storage.
        index_name (str): The name of the index.
        redis (Redis): An instance of the Redis client.

    Returns:
        Store: An instance of the Store.
    """
    return Store(storage_name, redis, index_name)


def get_stores(
    storage_names: List[str] = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    redis: Redis = Depends(get_redis),
) -> List[Store]:
    """
    Instantiate a list of Store objects based on the provided storage names.

    Args:
        storage_names (List[str]): The names of the storages.
        index_name (str): The name of the index.
        redis (Redis): An instance of the Redis client.

    Returns:
        List[Store]: A list of Store instances.
    """
    return [Store(storage_name, redis, index_name) for storage_name in storage_names]


def get_indexer(
    storage_name: str = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    settings: Settings = Depends(get_settings),
) -> Indexer:
    """
    Instantiate an Indexer object.

    Args:
        storage_name (str): The name of the storage.
        index_name (str): The name of the index.
        settings (Settings): The app settings.

    Returns:
        Indexer: An instance of the Indexer.
    """
    return Indexer(
        settings.data_path,
        storage_name,
        index_name,
        settings.n_features,
        settings.device.type == "cuda",
    )


def get_indexers(
    storage_names: List[str] = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    settings: Settings = Depends(get_settings),
) -> List[Indexer]:
    """
    Instantiate a list of Indexer objects based on the provided storage names.

    Args:
        storage_names (List[str]): The names of the storages.
        index_name (str): The name of the index.
        settings (Settings): The app settings.

    Returns:
        List[Indexer]: A list of Indexer instances.
    """
    device = settings.device.type == "cuda"
    return [
        Indexer(
            settings.data_path,
            storage_name,
            index_name,
            settings.n_features,
            device,
        )
        for storage_name in storage_names
    ]


def get_retrieval(
    store: Store = Depends(get_store),
    indexer: Indexer = Depends(get_indexer),
) -> ImageRetrieval:
    """
    Instantiate an ImageRetrieval object.

    Args:
        store (Store): An instance of the Store.
        indexer (Indexer): An instance of the Indexer.

    Returns:
        ImageRetrieval: An instance of the ImageRetrieval.
    """
    return ImageRetrieval(store, indexer)


def get_retrievals(
    stores: List[Store] = Depends(get_stores),
    indexers: List[Indexer] = Depends(get_indexers),
) -> List[ImageRetrieval]:
    """
    Instantiate a list of ImageRetrieval objects, one for each store and indexer.

    Args:
        stores (List[Store]): A list of Store instances.
        indexers (List[Indexer]): A list of Indexer instances.

    Returns:
        List[ImageRetrieval]: A list of ImageRetrieval instances.
    """
    return [ImageRetrieval(store, indexer) for store, indexer in zip(stores, indexers)]

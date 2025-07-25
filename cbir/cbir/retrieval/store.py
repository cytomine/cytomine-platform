"""Store module"""

from typing import Optional

from redis import Redis  # type: ignore


class Store:
    """A class for managing key-value storage using Redis."""

    def __init__(
        self,
        storage_name: str,
        redis: Redis,
        index_name: str = "index",
    ) -> None:
        """
        Store initialisation.

        Args:
            data_path (str): The path to the data source.
            storage_name (str): The name of the storage.
            redis (Redis): An instance of the Redis client.
            index_name (str, optional): The name of the index.
        """
        self.storage_name = storage_name
        self.index_name = index_name
        self.redis = redis

        self.prefix = f"{self.storage_name}:{self.index_name}"

    def get(self, key: str) -> Optional[str]:
        """
        Retrieves the value associated with the given key.

        Args:
            key (str): The key whose value is to be retrieved.

        Returns:
            Optional[str]: The value for the given key, or None if it does not exist.
        """
        value = self.redis.get(f"{self.prefix}:{key}")
        return value.decode("UTF-8") if value is not None else None

    def set(self, key: str, value: str) -> None:
        """
        Sets the value for the specified key in the Redis database.

        Args:
            key (str): The key for which the value is to be set.
            value (str): The value to be set for the specified key.
        """
        self.redis.set(f"{self.prefix}:{key}", value)

    def last(self) -> int:
        """
        Retrieves the value of the key "last_id".

        Returns:
            int: The value of the "last_id" key, or 0 if the key does not exist.
        """
        return int(self.get("last_id") or "0")

    def contains(self, key: str) -> bool:
        """
        Checks if a value exists for the given key.

        Args:
            key (str): The key to check for existence.

        Returns:
            bool: True if a value exists for the key, False otherwise.
        """
        return self.get(key) is not None

    def remove(self, key: str) -> None:
        """
        Deletes the value associated with the specified key from the Redis database.

        Args:
            key (str): The key to be deleted.
        """
        self.redis.delete(f"{self.prefix}:{key}")

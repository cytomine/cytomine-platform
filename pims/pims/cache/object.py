#  * Copyright (c) 2020-2022. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
import copy
from functools import cached_property as _cached_property
from typing import Any, Callable, Dict, KeysView, Union

from pims.utils.copy import SafelyCopiable

safe_cached_property = _cached_property


class cached_property:  # noqa
    """
    Attribute whose value is computed on first access.

    These cached properties are not thread-safe.
    """

    __slots__ = ('func', '__dict__')

    def __init__(self, func):
        """Initialize instance from decorated function."""
        self.func = func
        self.__doc__ = func.__doc__
        self.__module__ = func.__module__
        self.__name__ = func.__name__
        self.__qualname__ = func.__qualname__

    def __get__(self, instance, owner):
        if instance is None:
            return self
        try:
            value = self.func(instance)
        except AttributeError as exc:
            raise RuntimeError(exc)
        if value is NotImplemented:
            return getattr(super(owner, instance), self.func.__name__)
        setattr(instance, self.func.__name__, value)
        return value


DictCache = Dict[str, Any]


class SimpleDataCache(SafelyCopiable):
    """
    A simple wrapper to add caching mechanisms to a class.
    """
    def __init__(self, existing_cache: DictCache = None):
        self._cache = dict()

        if existing_cache is dict:
            self._cache = copy.deepcopy(existing_cache)

    def cache_value(self, key: str, value: Any, force: bool = False):
        """
        Cache a value at some key in the cache.

        Parameters
        ----------
        key
            The cache key
        value
            The content to cache
        force
            Whether to force to re-cache content if key is already cached.
        """
        if force or key not in self._cache:
            self._cache[key] = value

    def cache_func(self, key: str, delayed_func: Callable, *args, **kwargs):
        """
        Cache a function result at some key in the cache.

        Parameters
        ----------
        key
            The cache key
        delayed_func
            The function to call to get result to cache
        args
            The arguments to pass to `delayed_func`
        kwargs
            The keyword arguments to pass to `delayed_func`
        """
        self.cache_value(key, delayed_func(*args, **kwargs))

    def get_cached(
        self, key: str, delayed_func_or_value: Union[Callable, Any],
        *args, **kwargs
    ) -> Any:
        """
        Get cache content at given key, otherwise cache new content for this key.

        Parameters
        ----------
        key
            The cache key
        delayed_func_or_value
            If key is not in cache, cache the function result (if it is callable)
            or the variable content.
        args
            The arguments to pass to the delayed function if it is callable
        kwargs
            The keyword arguments to pass to the delayed function if it is
            callable.

        Returns
        -------
        content
            Cached content
        """
        if not self.is_in_cache(key):
            if callable(delayed_func_or_value):
                delayed_func = delayed_func_or_value
                self.cache_func(key, delayed_func, *args, **kwargs)
            else:
                value = delayed_func_or_value
                self.cache_value(key, value)
        return self._cache[key]

    @property
    def cache(self) -> DictCache:
        return self._cache

    @property
    def cached_keys(self) -> KeysView[str]:
        return self._cache.keys()

    def is_in_cache(self, key) -> bool:
        return key in self._cache

    def clear_cache(self):
        self._cache.clear()

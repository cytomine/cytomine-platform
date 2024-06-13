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

import hashlib
import inspect
import pickle
from enum import Enum
from functools import partial, wraps
from typing import Any, Callable, List, Optional, Tuple, Type

from redis import asyncio as aioredis
from starlette.responses import Response

from pims.api.utils.mimetype import VISUALISATION_MIMETYPES, get_output_format
from pims.config import get_settings
from pims.utils.background_task import add_background_task
from pims.utils.concurrency import exec_func_async

# Note: Parts of this implementation are inspired from
# https://github.com/long2ice/fastapi-cache

HEADER_CACHE_CONTROL = "Cache-Control"
HEADER_ETAG = "ETag"
HEADER_IF_NONE_MATCH = "If-None-Match"
HEADER_PIMS_CACHE = "X-Pims-Cache"
CACHE_KEY_PIMS_VERSION = "PIMS_VERSION"


def _hashable_dict(d: dict, separator: str = ":"):
    hashable = str()
    for k, v in d.items():
        if isinstance(v, Enum):
            v = v.value
        elif type(v) == dict:
            v = _hashable_dict(v, separator)
        hashable += f"{separator}{k}={str(v)}"
    return hashable


def all_kwargs_key_builder(
    func, kwargs, excluded_parameters, prefix
):
    copy_kwargs = kwargs.copy()
    if excluded_parameters is None:
        excluded_parameters = []
    for excluded in excluded_parameters:
        if excluded in copy_kwargs:
            copy_kwargs.pop(excluded)

    hashable = f"{func.__module__}:{func.__name__}" \
               f"{_hashable_dict(copy_kwargs, ':')}"
    hashed = hashlib.md5(hashable.encode()).hexdigest()
    cache_key = f"{prefix}:{hashed}"
    return cache_key


def _image_response_key_builder(
    func, kwargs, excluded_parameters, prefix, supported_mimetypes
):
    copy_kwargs = kwargs.copy()
    headers = copy_kwargs.get('headers')
    if headers and 'headers' not in excluded_parameters:
        # Find true output extension
        accept = headers.get('accept')
        extension = copy_kwargs.get('extension')
        format, _ = get_output_format(extension, accept, supported_mimetypes)
        copy_kwargs['extension'] = format

        # Extract other custom headers
        extra_headers = ('safe_mode', 'annotation_origin')
        for eh in extra_headers:
            v = headers.get(eh)
            if v:
                copy_kwargs[f"headers.{eh}"] = v
        del copy_kwargs['headers']

    return all_kwargs_key_builder(
        func, copy_kwargs, excluded_parameters, prefix
    )


class Codec:
    @classmethod
    def encode(cls, value: Any):
        raise NotImplementedError

    @classmethod
    def decode(cls, value: Any):
        raise NotImplementedError


class PickleCodec(Codec):
    @classmethod
    def encode(cls, value: Any):
        return pickle.dumps(value)

    @classmethod
    def decode(cls, value: Any):
        return pickle.loads(value)


class RedisBackend:
    def __init__(self, redis_url: str):
        self.redis = aioredis.from_url(redis_url, socket_connect_timeout=10)

    async def get_with_ttl(self, key: str) -> Tuple[int, str]:
        async with self.redis.pipeline(transaction=True) as pipe:
            return await (pipe.ttl(key).get(key).execute())

    async def get(self, key) -> str:
        return await self.redis.get(key)

    async def set(self, key: str, value: str, expire: int = None):
        return await self.redis.set(key, value, ex=expire)

    async def clear(self, namespace: str = None, key: str = None) -> int:
        if namespace:
            lua = f"for i, name in ipairs(redis.call('KEYS', '{namespace}:*')) " \
                  f"do redis.call('DEL', name); " \
                  f"end"
            return await self.redis.eval(lua, numkeys=0)
        elif key:
            return await self.redis.delete(key)

    async def exists(self, key) -> bool:
        return await self.redis.exists(key)


class PIMSCache:
    _enabled = False
    _backend = None
    _prefix = None
    _expire = None
    _init = False
    _codec = None
    _key_builder = None

    @classmethod
    async def init(
        cls, backend, prefix: str = "", expire: int = None
    ):
        if cls._init:
            return
        cls._init = True
        cls._backend = backend
        cls._prefix = prefix
        cls._expire = expire
        cls._codec = PickleCodec
        cls._key_builder = all_kwargs_key_builder

        try:
            await cls._backend.get(CACHE_KEY_PIMS_VERSION)
            cls._enabled = True
        except ConnectionError:
            cls._enabled = False

    @classmethod
    def get_backend(cls):
        if not cls._enabled:
            raise ConnectionError("Cache is not enabled.")
        return cls._backend

    @classmethod
    def get_cache(cls):
        return cls.get_backend()

    @classmethod
    def is_enabled(cls):
        return cls._enabled

    @classmethod
    def get_prefix(cls):
        return cls._prefix

    @classmethod
    def get_expire(cls):
        return cls._expire

    @classmethod
    def get_codec(cls):
        return cls._codec

    @classmethod
    def get_key_builder(cls):
        return cls._key_builder

    @classmethod
    async def clear(cls, namespace: str = None, key: str = None):
        namespace = cls._prefix + ":" + namespace if namespace else None
        return await cls._backend.clear(namespace, key)


async def startup_cache(pims_version):
    settings = get_settings()
    if not settings.cache_enabled:
        return

    await PIMSCache.init(
        RedisBackend(settings.cache_url), prefix="pims-cache",
    )

    # Flush the cache if persistent and PIMS version has changed.
    cache = PIMSCache.get_cache()  # noqa
    cached_version = await cache.get(CACHE_KEY_PIMS_VERSION)
    if cached_version is not None:
        cached_version = cached_version.decode('utf-8')
    if cached_version != pims_version:
        await cache.clear(PIMSCache.get_prefix())
        await cache.set(CACHE_KEY_PIMS_VERSION, pims_version)


def default_cache_control_builder(ttl=0):
    """
    Cache-Control header is not intuitive.
    * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control
    * https://web.dev/http-cache/#flowchart
    * https://jakearchibald.com/2016/caching-best-practices/
    * https://www.azion.com/en/blog/what-is-http-caching-and-how-does-it-work/
    """
    params = ["private", "must-revalidate"]
    if ttl:
        params += [f"max-age={ttl}"]
    return ','.join(params)


def cache_data(
    expire: int = None,
    vary: Optional[List] = None,
    codec: Type[Codec] = None,
    key_builder: Callable = None,
    cache_control_builder: Callable = None
):
    def wrapper(func: Callable):
        @wraps(func)
        async def inner(*args, **kwargs):
            nonlocal expire
            nonlocal vary
            nonlocal codec
            nonlocal key_builder
            nonlocal cache_control_builder
            signature = inspect.signature(func)
            bound_args = signature.bind_partial(*args, **kwargs)
            bound_args.apply_defaults()
            all_kwargs = bound_args.arguments
            request = all_kwargs.pop("request", None)
            response = all_kwargs.pop("response", None)

            if not PIMSCache.is_enabled() or \
                    (request and request.headers.get(HEADER_CACHE_CONTROL) == "no-store"):
                return await exec_func_async(func, *args, **kwargs)

            expire = expire or PIMSCache.get_expire()
            codec = codec or PIMSCache.get_codec()
            key_builder = key_builder or PIMSCache.get_key_builder()
            backend = PIMSCache.get_backend()
            prefix = PIMSCache.get_prefix()

            cache_key = key_builder(func, all_kwargs, vary, prefix)
            ttl, encoded = await backend.get_with_ttl(cache_key)
            if not request:
                if encoded is not None:
                    return codec.decode(encoded)
                data = await exec_func_async(func, *args, **kwargs)
                encoded = codec.encode(data)
                await backend.set(
                    cache_key, encoded,
                    expire or PIMSCache.get_expire()
                )
                return data

            if_none_match = request.headers.get(HEADER_IF_NONE_MATCH.lower())
            if encoded is not None:
                if response:
                    cache_control_builder = \
                        cache_control_builder or default_cache_control_builder
                    response.headers[HEADER_CACHE_CONTROL] = \
                        cache_control_builder(ttl=ttl)
                    etag = f"W/{hash(encoded)}"
                    response.headers[HEADER_ETAG] = etag
                    response.headers[HEADER_PIMS_CACHE] = "HIT"
                    if if_none_match == etag:
                        response.status_code = 304
                        return response
                decoded = codec.decode(encoded)
                if isinstance(decoded, Response):
                    decoded.headers[HEADER_CACHE_CONTROL] = \
                        response.headers.get(HEADER_CACHE_CONTROL)
                    decoded.headers[HEADER_ETAG] = \
                        response.headers.get(HEADER_ETAG)
                    decoded.headers[HEADER_PIMS_CACHE] = \
                        response.headers.get(HEADER_PIMS_CACHE)
                return decoded

            data = await exec_func_async(func, *args, **kwargs)
            encoded = codec.encode(data)

            async def _save(cache_key_, data_, expire_):
                await backend.set(cache_key_, data_, expire_)

            if response:
                cache_control_builder = \
                    cache_control_builder or default_cache_control_builder
                response.headers[HEADER_CACHE_CONTROL] = \
                    cache_control_builder(ttl=expire)
                etag = f"W/{hash(encoded)}"
                response.headers[HEADER_ETAG] = etag
                response.headers[HEADER_PIMS_CACHE] = "MISS"
                add_background_task(response, _save, cache_key, encoded, expire)
                if isinstance(data, Response):
                    data.headers[HEADER_CACHE_CONTROL] = \
                        response.headers.get(HEADER_CACHE_CONTROL)
                    data.headers[HEADER_ETAG] = \
                        response.headers.get(HEADER_ETAG)
                    data.headers[HEADER_PIMS_CACHE] = \
                        response.headers.get(HEADER_PIMS_CACHE)
                    data.background = response.background
            else:
                await _save(cache_key, encoded, expire)

            return data

        return inner

    return wrapper


def cache_image_response(
    expire: int = None,
    vary: Optional[List] = None,
    supported_mimetypes=None
):
    if supported_mimetypes is None:
        supported_mimetypes = VISUALISATION_MIMETYPES
    key_builder = partial(
        _image_response_key_builder, supported_mimetypes=supported_mimetypes
    )
    codec = PickleCodec
    return cache_data(expire, vary, codec, key_builder)

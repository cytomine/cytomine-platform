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
import gc
import hashlib
import inspect
import logging
import pickle
from enum import Enum
from functools import partial, wraps
from typing import Any, Callable, List, Optional, Tuple, Type

from fastapi_utils.tasks import repeat_every
from redis import asyncio as aioredis
from starlette.responses import Response

from pims.api.utils.mimetype import VISUALISATION_MIMETYPES, get_output_format
from pims.config import get_settings
from pims.utils.background_task import add_background_task
from pims.utils.concurrency import exec_func_async

from .. import __version__

# Note: Parts of this implementation are inspired from
# https://github.com/long2ice/fastapi-cache

HEADER_CACHE_CONTROL = "Cache-Control"
HEADER_ETAG = "ETag"
HEADER_IF_NONE_MATCH = "If-None-Match"
HEADER_PIMS_CACHE = "X-Pims-Cache"
CACHE_KEY_PIMS_VERSION = "PIMS_VERSION"

CACHE_KEY_NAMESPACE_IMAGE_FORMAT_METADATA = "pims-fmd"
CACHE_KEY_NAMESPACE_IMAGE_RESPONSE = "pims-img"
CACHE_KEY_NAMESPACE_RESPONSE = "pims-resp"

MANAGE_CACHE_INTERVAL = 60 * 5 # in seconds


log = logging.getLogger("pims.app")

def stable_hash(data: bytes) -> str:
    """
    Compute a stable and deterministic hash of the given bytes.

    Python `hash()` function CANNOT BE USED as it is salted by default, and thus do not produce the same value in
    different processes. See https://docs.python.org/3.8/reference/datamodel.html#object.__hash__
    """
    return hashlib.md5(data).hexdigest()


def _hashable_value(v: Any, separator: str = ":") -> str:
    if type(v) == list:
        v = ','.join(map(_hashable_value, v))
    if isinstance(v, Enum):
        v = v.value
    elif type(v) == dict:
        v = _hashable_dict(v, separator)

    return str(v)

def _hashable_dict(d: dict, separator: str = ":"):
    hashable = str()
    for k, v in d.items():
        hashable += f"{separator}{k}={_hashable_value(v, separator)}"
    return hashable


def all_kwargs_key_builder(
    func, kwargs, excluded_parameters, namespace
):
    copy_kwargs = kwargs.copy()
    if excluded_parameters is None:
        excluded_parameters = []
    for excluded in excluded_parameters:
        if excluded in copy_kwargs:
            copy_kwargs.pop(excluded)

    hashable = f"{func.__module__}:{func.__name__}" \
               f"{_hashable_dict(copy_kwargs, ':')}"
    hashed = stable_hash(hashable.encode())
    cache_key = f"{namespace}:{hashed}"
    return cache_key


def _image_response_key_builder(
    func, kwargs, excluded_parameters, namespace, supported_mimetypes
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
        func, copy_kwargs, excluded_parameters, namespace
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

    async def get_with_ttl(self, key: str, namespace: str = None) -> Tuple[int, str]:
        async with self.redis.pipeline(transaction=True) as pipe:
            key = f"{namespace}:{key}" if namespace else key
            return await (pipe.ttl(key).get(key).execute())

    async def get(self, key, namespace: str = None) -> str:
        key = f"{namespace}:{key}" if namespace else key
        return await self.redis.get(key)

    async def set(self, key: str, value: str, expire: int = None, namespace: str = None):
        key = f"{namespace}:{key}" if namespace else key
        return await self.redis.set(key, value, ex=expire)

    async def clear(self, namespace: str = None, key: str = None) -> int:
        if namespace:
            lua = f"for i, name in ipairs(redis.call('KEYS', '{namespace}:*')) " \
                  f"do redis.call('DEL', name); " \
                  f"end"
            return await self.redis.eval(lua, numkeys=0)
        elif key:
            return await self.redis.delete(key)

    async def exists(self, key: str, namespace: str = None) -> bool:
        key = f"{namespace}:{key}" if namespace else key
        return await self.redis.exists(key)

    async def close(self):
        return await self.redis.aclose()


class PIMSCache:
    _init = False
    _enabled = False
    _backend = None
    _default_expire = None
    _default_codec = None
    _default_key_builder = None
    _disabled_namespaces = None

    @classmethod
    async def init(
        cls, backend, default_expire: int = None, disabled_namespaces: List[str] = None
    ):
        if cls._init:
            return
        cls._init = True
        cls._backend = backend
        cls._default_expire = default_expire
        cls._default_codec = PickleCodec
        cls._default_key_builder = all_kwargs_key_builder
        cls._disabled_namespaces = disabled_namespaces if disabled_namespaces is not None else []

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
    def get_default_expire(cls):
        return cls._default_expire

    @classmethod
    def get_default_codec(cls):
        return cls._default_codec

    @classmethod
    def get_default_key_builder(cls):
        return cls._default_key_builder

    @classmethod
    def get_disabled_namespaces(cls):
        return cls._disabled_namespaces

    @classmethod
    def is_disabled_namespace(cls, namespace):
        return  namespace in cls._disabled_namespaces

    @classmethod
    async def clear(cls, namespace: str = None, key: str = None):
        return await cls._backend.clear(namespace, key)


async def startup_cache(pims_version):
    settings = get_settings()
    if not settings.cache_enabled:
        return

    namespaces = {
        CACHE_KEY_NAMESPACE_IMAGE_FORMAT_METADATA: settings.cache_image_format_metadata,
        CACHE_KEY_NAMESPACE_IMAGE_RESPONSE: settings.cache_image_responses,
        CACHE_KEY_NAMESPACE_RESPONSE: settings.cache_responses
    }
    disabled_namespaces = [k for k, v in namespaces.items() if v is False]

    await PIMSCache.init(
        RedisBackend(settings.cache_url), disabled_namespaces=disabled_namespaces
    )

    # Flush the cache if persistent and PIMS version has changed.
    cache = PIMSCache.get_cache()  # noqa
    cached_version = await cache.get(CACHE_KEY_PIMS_VERSION)
    if cached_version is not None:
        cached_version = cached_version.decode('utf-8')
    if cached_version != pims_version:
        log.info("PIMS version changed. Clearing PIMS cache.")
        await cache.clear()
        await cache.set(CACHE_KEY_PIMS_VERSION, pims_version)

    for disabled_namespace in PIMSCache.get_disabled_namespaces():
        log.info(f"[DEBUG MODE ONLY] Disabled namespace: {disabled_namespace}. Clearing related keys from cache.")
        await cache.clear(disabled_namespace)


@repeat_every(seconds=MANAGE_CACHE_INTERVAL, wait_first=MANAGE_CACHE_INTERVAL)
async def manage_cache() -> None:
    # As cache as an LRU policy, we need to periodically update the cache key with the PIMS VERSION
    gc.collect()
    settings = get_settings()
    if not settings.cache_enabled:
        return

    cache = PIMSCache.get_cache()
    await cache.set(CACHE_KEY_PIMS_VERSION, __version__)


async def shutdown_cache() -> None:
    settings = get_settings()
    if not settings.cache_enabled:
        return

    await PIMSCache.get_backend().close()
    log.info("Gracefully shutdown pims cache.")


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


def image_response_cache_control_builder(ttl=0):
    return default_cache_control_builder(ttl=get_settings().image_response_cache_control_max_age)


def cache_data(
    expire: int = None,
    ignored_variable_parameters: Optional[List] = None,
    codec: Type[Codec] = None,
    key_builder: Callable = None,
    cache_control_builder: Callable = None,
    namespace: str = None
):
    def wrapper(func: Callable):
        @wraps(func)
        async def inner(*args, **kwargs):
            nonlocal expire
            nonlocal ignored_variable_parameters
            nonlocal codec
            nonlocal key_builder
            nonlocal cache_control_builder
            nonlocal namespace
            signature = inspect.signature(func)
            bound_args = signature.bind_partial(*args, **kwargs)
            bound_args.apply_defaults()
            all_kwargs = bound_args.arguments
            request = all_kwargs.pop("request", None)
            response = all_kwargs.pop("response", None)

            # --- CACHE BYPASS ---
            if not PIMSCache.is_enabled() or \
                    (request and request.headers.get(HEADER_CACHE_CONTROL) == "no-store") or \
                    PIMSCache.is_disabled_namespace(namespace):
                return await exec_func_async(func, *args, **kwargs)

            # --- CACHE USED ---
            expire = expire or PIMSCache.get_default_expire()
            codec = codec or PIMSCache.get_default_codec()
            key_builder = key_builder or PIMSCache.get_default_key_builder()
            cache_key = key_builder(func, all_kwargs, ignored_variable_parameters, namespace)

            backend = PIMSCache.get_backend()
            ttl, encoded = await backend.get_with_ttl(cache_key)
            # ------- NON REQUEST DATA -------
            if not request:
                # CACHE HIT
                if encoded is not None:
                    return codec.decode(encoded)

                # CACHE MISS
                data = await exec_func_async(func, *args, **kwargs)
                encoded = codec.encode(data)
                await backend.set(
                    cache_key, encoded,
                    expire or PIMSCache.get_default_expire()
                )
                return data

            # ------- REQUEST DATA ------
            if_none_match = request.headers.get(HEADER_IF_NONE_MATCH.lower())
            # CACHE HIT
            if encoded is not None:
                if response:
                    cache_control_builder = \
                        cache_control_builder or default_cache_control_builder
                    response.headers[HEADER_CACHE_CONTROL] = \
                        cache_control_builder(ttl=ttl)
                    etag = f"W/{stable_hash(encoded)}"
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

            # CACHE MISS
            data = await exec_func_async(func, *args, **kwargs)
            encoded = codec.encode(data)

            async def _save(cache_key_, data_, expire_):
                await backend.set(cache_key_, data_, expire_)

            if response:
                cache_control_builder = \
                    cache_control_builder or default_cache_control_builder
                response.headers[HEADER_CACHE_CONTROL] = \
                    cache_control_builder(ttl=expire)
                etag = f"W/{stable_hash(encoded)}"
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
    ignored_variable_parameters: Optional[List] = None,
    supported_mimetypes=None
):
    """
    Cache an image response. An image response is expected to be an instance of
    `from starlette.responses import Response`.

    `ignored_variable_parameters`is an optional list of parameters of the decorated function that shouldn't
    be used for cache key.
    """

    if ignored_variable_parameters is None:
        ignored_variable_parameters = []
    ignored_variable_parameters += ['config', 'request', 'response']

    if supported_mimetypes is None:
        supported_mimetypes = VISUALISATION_MIMETYPES
    key_builder = partial(
        _image_response_key_builder, supported_mimetypes=supported_mimetypes
    )
    codec = PickleCodec
    return cache_data(
        expire, ignored_variable_parameters, codec, key_builder,
        image_response_cache_control_builder,
        namespace=CACHE_KEY_NAMESPACE_IMAGE_RESPONSE
    )


def cache_response(
    expire: int = None,
    ignored_variable_parameters: Optional[List] = None,
):
    if ignored_variable_parameters is None:
        ignored_variable_parameters = []
    ignored_variable_parameters += ['config', 'request', 'response']

    codec = PickleCodec
    return cache_data(
        expire, ignored_variable_parameters, codec,
        cache_control_builder=image_response_cache_control_builder,
        namespace=CACHE_KEY_NAMESPACE_RESPONSE
    )
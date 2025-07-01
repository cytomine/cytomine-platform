#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
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

import logging
import sys

from . import __api_version__, __version__
from .cache.redis import shutdown_cache, manage_cache

logger = logging.getLogger("pims.app")
logger.info("PIMS initialization...")
logger.info("PIMS version: {} ; api version: {}".format(__version__, __api_version__))

from pims.fastapi_tweaks import apply_fastapi_tweaks

apply_fastapi_tweaks()

import time
from fastapi import FastAPI, Request
from pydantic import ValidationError
from redis.exceptions import ConnectionError, TimeoutError

from pims.utils.background_task import add_background_task
from pims.cache import startup_cache
from pims.config import get_settings
from pims.api.exceptions import add_problem_exception_handler
from pims.api import (
    server, housekeeping, formats, metadata, thumb, window, resized, annotation, tile,
    operations,
    histograms, filters, colormaps
)

app = FastAPI(
    title="Cytomine Python Image Management Server PIMS",
    description="Cytomine Python Image Management Server (PIMS) HTTP API. "
                "While this API is intended to be internal, a lot of the "
                "following specification can be ported to the "
                "external (public) Cytomine API.",
    version=__api_version__,
    docs_url=None,
    redoc_url=f"{get_settings().api_base_path}/docs",
)


@app.on_event("startup")
async def startup():
    # Check PIMS configuration
    try:
        settings = get_settings()
        logger.info("PIMS is starting with config:")
        for k, v in settings.model_dump().items():
            logger.info(f"* {k}: {v}")
    except ValidationError as e:
        logger.error("Impossible to read or parse some PIMS settings:")
        logger.error(e)
        exit(-1)

    import pyvips
    pyvips_binary = pyvips.API_mode
    if not pyvips_binary:
        logger.warning("Pyvips is running in non binary mode.")
    pyvips.cache_set_max(get_settings().vips_cache_max_items)
    pyvips.cache_set_max_mem(get_settings().vips_cache_max_memory)
    pyvips.cache_set_max_files(get_settings().vips_cache_max_files)

    from shapely.speedups import enabled as shapely_speedups
    if not shapely_speedups:
        logger.warning("Shapely is running without speedups.")

    # Caching
    if not get_settings().cache_enabled:
        logger.warning(f"Cache is disabled by configuration.")
    else:
        try:
            logger.info(f" Try to reach cache ... ")
            await startup_cache(__version__)
            await manage_cache()
            logger.info(f"Cache is ready!")
        except ConnectionError:
            sys.exit(
                f"Impossible to connect to cache \"{get_settings().cache_url}\" "
                f"while cache is enabled by configuration."
            )
        except TimeoutError as e:
            sys.exit(
                f"Timeout while connecting to cache \"{get_settings().cache_url}\": {str(e)}."
            )


@app.on_event("shutdown")
async def shutdown() -> None:
    await shutdown_cache()


def _log(request_, response_, duration_):
    args = dict(request_.query_params)

    cached = response_.headers.get("X-Pims-Cache")
    log_cached = None
    if cached is not None:
        log_cached = ('cache', cached)

    log_params = [
        ('method', request_.method),
        ('path', request_.url.path),
        ('status', response_.status_code),
        ('duration', f"{duration_:.2f}ms"),
        ('params', args),
    ]

    if log_cached:
        log_params.insert(-1, log_cached)

    parts = []
    for name, value in log_params:
        parts.append(f"{value}")
    line = " ".join(parts)
    logger.info(line)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    now = time.time()
    duration = (now - start) * 1000

    # https://github.com/tiangolo/fastapi/issues/2215
    add_background_task(response, _log, request, response, duration)
    return response

app.include_router(metadata.router)
app.include_router(tile.router)
app.include_router(thumb.router)
app.include_router(resized.router)
app.include_router(window.router)
app.include_router(annotation.router)
app.include_router(histograms.router)
app.include_router(formats.router)
app.include_router(filters.router)
app.include_router(colormaps.router)
app.include_router(operations.router)
app.include_router(housekeeping.router)
app.include_router(server.router)

add_problem_exception_handler(app)

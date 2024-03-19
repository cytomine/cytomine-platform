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
logger = logging.getLogger("pims.app")
logger.info("[green bold]PIMS initialization...")
logger.info("[green bold]PIMS version: {} ; api version: {}".format(__version__, __api_version__))

from pims.fastapi_tweaks import apply_fastapi_tweaks

apply_fastapi_tweaks()

import time
from fastapi import FastAPI, Request, APIRouter
from pydantic import ValidationError
from aioredis.exceptions import ConnectionError, TimeoutError

from pims.utils.background_task import add_background_task
from pims.cache import startup_cache
from pims.config import get_settings
from pims.docs import get_redoc_html
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
    redoc_url=None,
)


@app.on_event("startup")
async def startup():
    # Check PIMS configuration
    try:
        settings = get_settings()
        logger.info("[green bold]PIMS is starting with config:[/]")
        for k, v in settings.dict().items():
            logger.info(f"[green]* {k}:[/] [blue]{v}[/]", extra={"highlight": False})
    except ValidationError as e:
        logger.error("Impossible to read or parse some PIMS settings:")
        logger.error(e)
        exit(-1)

    # Check optimisation are enabled for external libs
    from pydantic import compiled as pydantic_compiled
    if not pydantic_compiled:
        logger.warning(f"[red]Pydantic is running in non compiled mode.")

    import pyvips
    pyvips_binary = pyvips.API_mode
    if not pyvips_binary:
        logger.warning("[red]Pyvips is running in non binary mode.")
    pyvips.leak_set(get_settings().vips_allow_leak)
    pyvips.cache_set_max(get_settings().vips_cache_max_items)
    pyvips.cache_set_max_mem(get_settings().vips_cache_max_memory * 1048576)
    pyvips.cache_set_max_files(get_settings().vips_cache_max_files)

    from shapely.speedups import enabled as shapely_speedups
    if not shapely_speedups:
        logger.warning("[red]Shapely is running without speedups.")

    # Caching
    if not get_settings().cache_enabled:
        logger.warning(f"[orange3]Cache is disabled by configuration.")
    else:
        try:
            logger.info(f"[orange3] Try to reach cache ... ")
            await startup_cache(__version__)
            logger.info(f"[green]Cache is ready!")
        except ConnectionError:
            sys.exit(
                f"Impossible to connect to cache \"{get_settings().cache_url}\" "
                f"while cache is enabled by configuration."
            )
        except TimeoutError as e:
            sys.exit(
                f"Timeout while connecting to cache \"{get_settings().cache_url}\": {str(e)}."
            )

def _log(request_, response_, duration_):
    args = dict(request_.query_params)

    cached = response_.headers.get("X-Pims-Cache")
    log_cached = None
    if cached is not None:
        color = "red" if cached == "MISS" else "green"
        log_cached = ('cache', cached, color)

    log_params = [
        ('method', request_.method, 'magenta'),
        ('path', request_.url.path, 'blue'),
        ('status', response_.status_code, 'yellow'),
        ('duration', f"{duration_:.2f}ms", 'green'),
        ('params', args, 'blue'),
    ]

    if log_cached:
        log_params.insert(-1, log_cached)

    parts = []
    for name, value, color in log_params:
        parts.append(f"[{color}]{value}[/]")
    line = " ".join(parts)
    logger.info(line, extra={"highlight": False})


@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    now = time.time()
    duration = (now - start) * 1000

    # https://github.com/tiangolo/fastapi/issues/2215
    add_background_task(response, _log, request, response, duration)
    return response


documentation_router = APIRouter(prefix=get_settings().api_base_path)


@documentation_router.get("/docs", include_in_schema=False)
def docs(req: Request):
    root_path = req.scope.get("root_path", "").rstrip("/")
    openapi_url = root_path + app.openapi_url
    return get_redoc_html(openapi_url=openapi_url, title=app.title)


app.include_router(documentation_router)
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

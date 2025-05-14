"""Content Based Image Retrieval API"""

from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from cbir import __version__
from cbir.api import images, searches, storages
from cbir.config import get_settings
from cbir.models.utils import load_model


@asynccontextmanager
async def lifespan(local_app: FastAPI) -> AsyncGenerator[None, None]:
    """Lifespan of the app."""

    # Initialisation
    local_app.state.model = load_model(get_settings())

    yield


PREFIX = get_settings().api_base_path

app = FastAPI(
    title="Cytomine Content Based Image Retrieval Server",
    description="Cytomine Content Based Image Retrieval Server (CBIR) HTTP API.",
    version=__version__,
    lifespan=lifespan,
    license_info={
        "name": "Apache 2.0",
        "identifier": "Apache-2.0",
        "url": "https://www.apache.org/licenses/LICENSE-2.0.html",
    },
)
app.include_router(router=images.router, prefix=PREFIX)
app.include_router(router=searches.router, prefix=PREFIX)
app.include_router(router=storages.router, prefix=PREFIX)

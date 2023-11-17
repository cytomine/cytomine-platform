#  Copyright 2023 Cytomine ULiège
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""Content Based Image Retrieval API"""

from contextlib import asynccontextmanager

from cbir_tfe.db import Database
from cbir_tfe.models import Model
from fastapi import FastAPI

from cbir import __version__
from cbir.api import image
from cbir.config import DatabaseSetting, ModelSetting
from cbir.utils import check_database


def load_model(settings):
    """Load the weights of the model.

    Args:
        settings (ModelSetting): The settings of the model.

    Returns:
        Model: The loaded model.
    """
    return Model(
        model=settings.extractor,
        use_dr=settings.use_dr,
        num_features=settings.n_features,
        name=settings.weights,
        device=settings.device,
    )


def init_database(model, settings):
    """Initialise the database.

    Args:
        settings (DatabaseSetting): The settings of the database.


    Returns:
        Database: The initialised database.
    """
    return Database(
        settings.filename,
        model,
        load=check_database(settings.filename),
        device=model.device,
    )


@asynccontextmanager
async def lifespan(local_app: FastAPI):
    """Lifespan of the app.

    Args:
        app (FastAPI): The FastAPI app.
    """

    # Settings
    local_app.state.model_settings = ModelSetting.get_settings()
    local_app.state.database_settings = DatabaseSetting.get_settings()

    # Initialisation
    local_app.state.model = load_model(local_app.state.model_settings)
    local_app.state.database = init_database(
        local_app.state.model,
        local_app.state.database_settings,
    )

    yield


app = FastAPI(
    title="Cytomine Content Based Image Retrieval Server",
    description="Cytomine Content Based Image Retrieval Server (CBIR) HTTP API.",
    version=__version__,
    lifespan=lifespan,
)
app.include_router(router=image.router, prefix="/api")

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

"""Image indexing"""

import os
from pathlib import Path

from cbir_tfe.db import Database
from cbir_tfe.models import Model
from fastapi import APIRouter, File, UploadFile

from cbir.config import DatabaseSetting, ModelSetting
from cbir.utils import check_database

database_settings = DatabaseSetting.get_settings()
model_settings = ModelSetting.get_settings()
router = APIRouter()


@router.post("/images/index")
async def index_image(image: UploadFile = File(...)):
    """Index the given image."""

    content = await image.read()
    image_path = Path(os.path.join(database_settings.image_path, image.filename))
    image_path.parent.mkdir(parents=True, exist_ok=True)
    image_path.write_bytes(content)

    model = Model(
        model=model_settings.extractor,
        use_dr=model_settings.use_dr,
        num_features=model_settings.n_features,
        name=model_settings.weights,
        device=model_settings.device,
    )

    database = Database(
        database_settings.filename,
        model,
        load=check_database(database_settings.filename),
        device=model_settings.device,
    )
    database.add_dataset(
        database_settings.image_path,
        model_settings.extractor,
        model_settings.generalise,
    )

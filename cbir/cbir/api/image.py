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

"""Image indexing and retrieval"""

import os
from io import BytesIO
from pathlib import Path
from typing import List, Tuple

from fastapi import APIRouter, File, Form, HTTPException, Request, UploadFile
from PIL import Image
from torchvision import transforms

router = APIRouter()


@router.post("/images/index")
async def index_image(request: Request, image: UploadFile = File()) -> None:
    """Index the given image.

    Args:
        request (Request): The request.
        image (UploadedFile): The image to index.
    """

    if image.filename is None:
        raise HTTPException(status_code=404, detail="Image filename not found")

    database = request.app.state.database
    database_settings = request.app.state.database_settings
    model_settings = request.app.state.model_settings

    content = await image.read()
    image_path = Path(os.path.join(database_settings.image_path, image.filename))
    image_path.parent.mkdir(parents=True, exist_ok=True)
    image_path.write_bytes(content)

    database.add_dataset(
        database_settings.image_path,
        model_settings.extractor,
        model_settings.generalise,
    )


@router.post("/images/retrieve")
async def retrieve_image(
    request: Request,
    nrt_neigh: int = Form(),
    image: UploadFile = File(),
) -> Tuple[List[str], List[float]]:
    """Retrieve similar images from the database.

    Args:
        request (Request): The request.
        nrt_neigh (int): The number of nearest images to retrieve.
        image (UploadedFile): The query image to retrieve similar images.

    Returns:
        filenames (list): The filenames of the most similar images.
        distances (list): The distance between the similar images and the query image.
    """

    database = request.app.state.database
    model_settings = request.app.state.model_settings

    content = await image.read()
    features_extraction = transforms.Compose(
        [
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ]
    )
    features = features_extraction(Image.open(BytesIO(content)))

    filenames, distances, _, _, _ = database.search(
        features,
        model_settings.extractor,
        nrt_neigh=nrt_neigh,
    )

    return filenames, distances.tolist()

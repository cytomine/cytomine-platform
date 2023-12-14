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

import json
from io import BytesIO

from fastapi import (
    APIRouter,
    File,
    Form,
    HTTPException,
    Request,
    Response,
    UploadFile,
)
from PIL import Image
from torchvision import transforms

router = APIRouter()


@router.post("/images/index")
async def index_image(request: Request, image: UploadFile = File()) -> None:
    """Index the given image."""

    if image.filename is None:
        raise HTTPException(status_code=404, detail="Image filename not found")

    database = request.app.state.database
    model = request.app.state.model

    content = await image.read()
    features_extraction = transforms.Compose(
        [
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ]
    )

    database.index_image(
        model,
        features_extraction(Image.open(BytesIO(content))),
        image.filename,
    )


@router.post("/images/retrieve")
async def retrieve_image(
    request: Request,
    nrt_neigh: int = Form(),
    image: UploadFile = File(),
) -> Response:
    """Retrieve similar images from the database."""

    database = request.app.state.database
    model = request.app.state.model

    content = await image.read()
    features_extraction = transforms.Compose(
        [
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ]
    )

    filenames, distances = database.search_similar_images(
        model,
        features_extraction(Image.open(BytesIO(content))),
        nrt_neigh=nrt_neigh,
    )

    return Response(
        content=json.dumps({"filenames": filenames, "distances": distances.tolist()}),
    )

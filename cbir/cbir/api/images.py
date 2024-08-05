"""Image API"""

import json
from io import BytesIO
from pathlib import Path

from fastapi import (
    APIRouter,
    File,
    Form,
    HTTPException,
    Query,
    Request,
    Response,
    UploadFile,
)
from fastapi.responses import JSONResponse
from PIL import Image
from torchvision import transforms

router = APIRouter()


@router.post("/images")
async def index_image(
    request: Request,
    image: UploadFile,
    storage_name: str = Query(alias="storage"),
    index_name: str = Query(default="index", alias="index"),
) -> JSONResponse:
    """
    Index the given image into the specified storage and index.

    Args:
        request (Request): The incoming HTTP request.
        image (UploadFile): The image file to be indexed.
        storage_name (str): The name of the storage where the index is stored.
        index_name (str): The name of the index where the image features will be added.

    Returns:
        JSONResponse: A JSON response containing the ID of the newly indexed image.
    """

    if image.filename is None:
        raise HTTPException(status_code=404, detail="Image filename not found")

    if not storage_name:
        raise HTTPException(status_code=404, detail="Storage is required")

    database = request.app.state.database

    base_path = Path(database.settings.data_path)
    storage_path = base_path / storage_name
    if not storage_path.is_dir():
        raise HTTPException(
            status_code=404,
            detail=f"Storage '{storage_name}' not found.",
        )

    model = request.app.state.model

    content = await image.read()
    features_extraction = transforms.Compose(
        [
            transforms.Resize((224, 224)),
            transforms.ToTensor(),
            transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
        ]
    )

    ids = database.index_image(
        model,
        features_extraction(Image.open(BytesIO(content))),
        image.filename,
    )

    return JSONResponse(
        content={
            "ids": ids,
            "storage": storage_name,
            "index": index_name,
        }
    )


@router.delete("/images/{filename}")
def remove_image(request: Request, filename: str) -> None:
    """Remove an indexed image."""

    database = request.app.state.database

    if not database.contains(filename):
        raise HTTPException(status_code=404, detail=f"{filename} not found")

    database.remove(filename)


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
        content=json.dumps({"filenames": filenames, "distances": distances}),
    )

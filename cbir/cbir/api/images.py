"""Image API"""

from pathlib import Path

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Query,
    Request,
    UploadFile,
)
from fastapi.responses import JSONResponse

from cbir.api.utils.utils import get_retrieval
from cbir.config import Settings, get_settings
from cbir.retrieval.retrieval import ImageRetrieval

router = APIRouter()


@router.post("/images")
async def index_image(
    request: Request,
    image: UploadFile,
    storage_name: str = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    retrieval: ImageRetrieval = Depends(get_retrieval),
    settings: Settings = Depends(get_settings),
) -> JSONResponse:
    """
    Index the given image into the specified storage and index.

    Args:
        request (Request): The incoming HTTP request.
        image (UploadFile): The image file to be indexed.
        storage_name (str): The name of the storage where the index is stored.
        index_name (str): The name of the index where the image features will be added.
        retrieval (ImageRetrieval): The image retrieval object.
        settings (DatabaseSetting): The database settings.

    Returns:
        JSONResponse: A JSON response containing the ID of the newly indexed image.
    """

    if image.filename is None:
        raise HTTPException(status_code=404, detail="Image filename not found!")

    if retrieval.store.contains(image.filename):
        raise HTTPException(status_code=409, detail="Image filename already exist!")

    if not storage_name:
        raise HTTPException(status_code=404, detail="Storage is required")

    base_path = Path(settings.data_path)
    storage_path = base_path / storage_name
    if not storage_path.is_dir():
        raise HTTPException(
            status_code=404,
            detail=f"Storage '{storage_name}' not found.",
        )

    content = await image.read()

    model = request.app.state.model

    ids = retrieval.index_image(model, content, image.filename)

    return JSONResponse(
        content={
            "ids": ids,
            "storage": storage_name,
            "index": index_name,
        }
    )


@router.delete("/images/{filename}")
def remove_image(
    filename: str,
    storage_name: str = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    retrieval: ImageRetrieval = Depends(get_retrieval),
) -> JSONResponse:
    """
    Remove an indexed image.

    Args:
        filename (str): The name of the image to be removed.
        storage_name (str): The name of the storage where the index is stored.
        index_name (str): The name of the index where the image features will be added.
        retrieval (ImageRetrieval): The image retrieval object.

    Returns:
        JSONResponse: A JSON response containing the ID of the deleted image.
    """

    if not retrieval.store.contains(filename):
        raise HTTPException(status_code=404, detail=f"{filename} not found")

    label = retrieval.remove_image(filename)

    return JSONResponse(
        content={
            "id": label,
            "storage": storage_name,
            "index": index_name,
        }
    )

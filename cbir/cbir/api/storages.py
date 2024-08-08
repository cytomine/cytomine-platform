"""Storage API"""

import shutil
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import JSONResponse

from cbir.api.utils.models import Storage
from cbir.config import Settings, get_settings

router = APIRouter()


@router.get("/storages")
def get_storages(settings: Settings = Depends(get_settings)) -> JSONResponse:
    """
    Get all storages.

    Args:
        settings (Settings): The database settings.

    Returns:
        JSONResponse: A JSON response containing the list of storage names.
    """

    base_path = Path(settings.data_path)
    if not base_path.is_dir():
        raise HTTPException(
            status_code=404,
            detail="Base path not found or is not a directory",
        )

    folder_names = [f.name for f in base_path.iterdir() if f.is_dir()]

    return JSONResponse(content=folder_names)


@router.post("/storages")
async def create_storage(
    body: Storage,
    settings: Settings = Depends(get_settings),
) -> JSONResponse:
    """
    Create a new storage.

    Args:
        body (Storage): The body of the request.
        settings (Settings): The database settings.

    Returns:
        JSONResponse: A JSON response containing the message of the creation.
    """

    base_path = Path(settings.data_path)
    storage_path = base_path / body.name

    if storage_path.exists():
        raise HTTPException(
            status_code=409,
            detail=f"Storage with name '{body.name}' already exists.",
        )

    try:
        storage_path.mkdir()
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to create storage: {str(e)}",
        ) from e

    return JSONResponse(content={"message": f"Created storage with name: {body.name}"})


@router.get("/storages/{name}")
async def get_storage(
    name: str,
    settings: Settings = Depends(get_settings),
) -> JSONResponse:
    """
    Get a specific storage.

    Args:
        name (str): The name of the storage.
        settings (Settings): The database settings.

    Returns:
        JSONResponse: A JSON response containing the name of the storage.
    """

    base_path = Path(settings.data_path)
    storage_path = base_path / name

    if not storage_path.is_dir():
        raise HTTPException(
            status_code=404,
            detail=f"Storage with name '{name}' not found.",
        )

    return JSONResponse(content={"name": name})


@router.delete("/storages/{name}")
async def delete_storage(
    name: str,
    settings: Settings = Depends(get_settings),
) -> JSONResponse:
    """
    Delete a specific storage and its content.

    Args:
        name (str): The name of the storage to delete.
        settings (Settings): The database settings.

    Returns:
        JSONResponse: A JSON response containing the message of the deletion.
    """

    base_path = Path(settings.data_path)
    storage_path = base_path / name

    if not storage_path.is_dir():
        raise HTTPException(
            status_code=404,
            detail=f"Storage with name '{name}' not found.",
        )

    try:
        shutil.rmtree(storage_path)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to delete storage: {str(e)}",
        ) from e

    return JSONResponse(content={"message": f"Deleted storage with name: {name}"})

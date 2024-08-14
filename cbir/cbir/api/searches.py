"""Search API"""

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
from cbir.retrieval.retrieval import ImageRetrieval

router = APIRouter()


@router.post("/search")
async def retrieve_image(
    request: Request,
    image: UploadFile,
    nrt_neigh: int = Query(...),
    storage_name: str = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    retrieval: ImageRetrieval = Depends(get_retrieval),
) -> JSONResponse:
    """
    Search for similar images from the index.

    Args:
        request (Request): The incoming HTTP request.
        image (UploadFile): The query image.
        nrt_neigh (int): The number of nearest neighbors to retrieve.
        storage_name (str): The name of the storage where the index is stored.
        index_name (str): The name of the index where the image features will be added.
        retrieval (ImageRetrieval): The image retrieval object.

    Returns:
        JSONResponse: A JSON containing the list of similarities.
    """

    if not storage_name:
        raise HTTPException(status_code=404, detail="Storage is required")

    model = request.app.state.model
    content = await image.read()

    similarities = retrieval.search(model, content, nrt_neigh)

    return JSONResponse(
        content={
            "query": image.filename,
            "storage": storage_name,
            "index": index_name,
            "similarities": similarities,
        }
    )

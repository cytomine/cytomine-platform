"""Search API"""

from typing import List

from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    Query,
    Request,
    UploadFile,
)
from fastapi.responses import JSONResponse

from cbir.api.utils.utils import get_retrievals
from cbir.retrieval.retrieval import ImageRetrieval

router = APIRouter()


@router.post("/search")
async def retrieve_image(
    request: Request,
    image: UploadFile,
    nrt_neigh: int = Query(...),
    storage_names: List[str] = Query(..., alias="storage"),
    index_name: str = Query(default="index", alias="index"),
    retrievals: List[ImageRetrieval] = Depends(get_retrievals),
) -> JSONResponse:
    """
    Search for similar images from the index.

    Args:
        request (Request): The incoming HTTP request.
        image (UploadFile): The query image.
        nrt_neigh (int): The number of nearest neighbors to retrieve.
        storage_names (List[str]): The list of storage names.
        index_name (str): The name of the index where the image features will be added.
        retrievals (List[ImageRetrieval]): The image retrieval object.

    Returns:
        JSONResponse: A JSON containing the list of similarities.
    """

    if not storage_names:
        raise HTTPException(status_code=404, detail="Storage is required")

    model = request.app.state.model
    content = await image.read()

    similarities = [
        result
        for retrieval in retrievals
        for result in retrieval.search(model, content, nrt_neigh)
    ]
    similarities = sorted(similarities, key=lambda x: x[1])[:nrt_neigh]

    return JSONResponse(
        content={
            "query": image.filename,
            "storage": storage_names,
            "index": index_name,
            "similarities": similarities,
        }
    )

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

from fastapi import APIRouter, FastAPI

from cbir import __version__
from cbir.api import index

router = APIRouter(prefix="/api")


@router.get("/images/{image_id}")
def get_image():
    """Get an image given its ID from the database"""


@router.delete("/images/{image_id}")
def delete_image():
    """Delete an image from the database"""


@router.post("/images/retrieve")
def retrieve_image():
    "Retrieve the nearest images given a query image."


app = FastAPI(
    title="Cytomine Content Based Image Retrieval Server",
    description="Cytomine Content Based Image Retrieval Server (CBIR) HTTP API.",
    version=__version__,
)
app.include_router(router)
app.include_router(router=index.router, prefix="/api")

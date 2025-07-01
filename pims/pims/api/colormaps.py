#  * Copyright (c) 2020-2021. Authors: see NOTICE file.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      http://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.

from typing import List

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, Field

from pims.api.exceptions import ColormapNotFoundProblem
from pims.api.utils.header import ImageRequestHeaders
from pims.api.utils.mimetype import (
    OutputExtension, PROCESSING_MIMETYPES,
    extension_path_parameter, get_output_format
)
from pims.api.utils.models import CollectionSize, ColormapId
from pims.api.utils.response import FastJsonResponse, response_list
from pims.config import get_settings
from pims.processing.colormaps import COLORMAPS, ColormapType
from pims.processing.image_response import ColormapRepresentationResponse
from typing_extensions import Annotated

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Colormaps']


class Colormap(BaseModel):
    """
    A colormap is a function that maps colors of an input image to the colors of a target image.
    """
    id: ColormapId
    name: str = Field(
        ..., description='A human readable name for the colormap.'
    )
    type: ColormapType = Field(...)


class ColormapsList(CollectionSize):
    items: List[Colormap] = Field(None, description='Array of colormaps', title='Colormap')


def _serialize_colormap(cmap):
    return Colormap(
        id=cmap.identifier,
        name=cmap.name,
        type=cmap.ctype,
    )


@router.get(
    '/colormaps', response_model=ColormapsList, tags=api_tags,
    response_class=FastJsonResponse
)
async def list_colormaps(
    with_inverted: bool = Query(
        False, description="Also list inverted colormaps"
    )
):
    """
    List all colormaps
    """
    colormaps = [
        _serialize_colormap(cmap)
        for cmap in COLORMAPS.values()
        if with_inverted or not cmap.inverted
    ]
    return response_list(colormaps)


@router.get(
    '/colormaps/{colormap_id}', response_model=Colormap, tags=api_tags,
    response_class=FastJsonResponse
)
async def show_colormap(colormap_id: str):
    """
    Get a colormap
    """
    colormap_id = colormap_id.upper()
    if colormap_id not in COLORMAPS.keys():
        raise ColormapNotFoundProblem(colormap_id)
    return _serialize_colormap(COLORMAPS[colormap_id])


@router.get('/colormaps/{colormap_id}/representation{extension:path}', tags=api_tags)
async def show_colormap_representation(
    colormap_id: str,
    width: Annotated[int, Field(gt=10, le=512)] = Query(
        100, description="Width of the graphic representation, in pixels."
    ),
    height: Annotated[int, Field(gt=0, le=512)] = Query(
        10, description="Height of the graphic representation, in pixels."
    ),
    headers: ImageRequestHeaders = Depends(),
    extension: OutputExtension = Depends(extension_path_parameter),
):
    """
    Get a graphic representation of a colormap
    """

    if colormap_id not in COLORMAPS.keys():
        raise ColormapNotFoundProblem(colormap_id)

    out_format, mimetype = get_output_format(
        extension, headers.accept, PROCESSING_MIMETYPES
    )

    return ColormapRepresentationResponse(
        COLORMAPS[colormap_id], width, height, out_format
    ).http_response(mimetype)

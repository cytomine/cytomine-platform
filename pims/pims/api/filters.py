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
from typing import List, Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field

from pims.api.exceptions import FilterNotFoundProblem
from pims.api.utils.models import CollectionSize, FilterId, FilterType
from pims.api.utils.response import FastJsonResponse, response_list
from pims.config import get_settings
from pims.filters import FILTERS

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Filters']


class Filter(BaseModel):
    """
    An image filter is used to change the appearance of an image and helps at understanding the
    source image.
    """
    id: FilterId
    aliases: List[FilterId] = Field(
        [], description='List of filter id aliases'
    )
    name: str = Field(
        ..., description='A human readable name for the image filter.'
    )
    type: FilterType = Field(...)
    description: Optional[str] = Field(
        None, description='Filter description, explaining how it works, in Markdown.'
    )


class FiltersList(CollectionSize):
    items: List[Filter] = Field(None, description='Array of filters', title='Filter')


def _serialize_filter(imgfilter):
    return Filter(
        id=imgfilter.get_identifier(),
        name=imgfilter.get_name(),
        type=imgfilter.get_type(),
        description=imgfilter.get_description(),
        aliases=imgfilter.get_aliases()
    )


@router.get(
    '/filters', response_model=FiltersList, tags=api_tags,
    response_class=FastJsonResponse
)
async def list_filters():
    """
    List all filters
    """
    filters = [_serialize_filter(imgfilter) for imgfilter in FILTERS.values()]
    return response_list(filters)


@router.get(
    '/filters/{filter_id}', response_model=Filter, tags=api_tags,
    response_class=FastJsonResponse
)
async def show_filter(filter_id: str):
    """
    Get a filter
    """
    filter_id = filter_id.upper()
    if filter_id not in FILTERS.keys():
        raise FilterNotFoundProblem(filter_id)
    return _serialize_filter(FILTERS[filter_id])

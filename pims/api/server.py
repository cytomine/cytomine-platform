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

from fastapi import APIRouter
from pydantic import BaseModel, Field

from pims import __api_version__, __version__
from pims.config import ReadableSettings, get_settings

router = APIRouter(prefix=get_settings().api_base_path)


class ServerInfo(BaseModel):
    version: str = Field(..., description='PIMS version')
    api_version: str = Field(..., description='PIMS API specification version')
    settings: ReadableSettings


@router.get('/info', response_model=ServerInfo, tags=['Server'])
async def show_status() -> ServerInfo:
    """
    PIMS Server status.
    """
    return ServerInfo(
        version=__version__, api_version=__api_version__, settings=get_settings()
    )

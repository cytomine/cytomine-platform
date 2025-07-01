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
from typing import Optional

from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing_extensions import Annotated

from pims.api.exceptions import NotADirectoryProblem, check_path_existence
from pims.api.utils.response import FastJsonResponse
from pims.config import get_settings
from pims.files.file import Path

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Housekeeping']


class DiskUsage(BaseModel):
    mount_point: Optional[str] = Field(
        None,
        description='The mounting point of the file system having the directory.'
    )
    mount_available_size: Annotated[int, Field(ge=0)] = Field(
        ...,
        description='Available space on the mounted file system having the directory, in bytes.',
    )
    mount_total_size: Annotated[int, Field(ge=0)] = Field(
        ...,
        description='Total space on the mounted file system having the directory, in bytes.',
    )
    mount_used_size: Annotated[int, Field(ge=0)] = Field(
        ...,
        description='Used space on the mounted file system having the directory, in bytes',
    )
    mount_used_size_percentage: Annotated[float, Field(ge=0.0, le=100.0)] = Field(
        ...,
        description='Percentage of used space regarding total space of the mounted file system',
    )
    used_size: Annotated[int, Field(ge=0)] = Field(
        ...,
        description='Used space by the directory, in bytes.'
    )
    used_size_percentage: Annotated[float, Field(ge=0.0, le=100.0)] = Field(
        ...,
        description='Percentage of directory used space regarding total space of the mounted '
                    'file system',
    )


def _serialize_usage(path):
    usage = path.mount_disk_usage()
    size = path.size
    mount_point = path.mount_point()
    return DiskUsage(
        **{
            "mount_point": str(mount_point) if mount_point else None,
            "mount_available_size": usage.free,
            "mount_total_size": usage.total,
            "mount_used_size": usage.used,
            "mount_used_size_percentage": float(usage.used) / float(usage.total) * 100,
            "used_size": size,
            "used_size_percentage": float(size) / float(usage.total) * 100
        }
    )


@router.get(
    '/directory/{directorypath:path}/disk-usage', response_model=DiskUsage,
    tags=api_tags, response_class=FastJsonResponse
)
async def show_path_usage(
    directorypath: str,
) -> DiskUsage:
    """
    Directory disk usage
    """
    path = Path.from_filepath(directorypath)
    check_path_existence(path)
    if not path.is_dir():
        raise NotADirectoryProblem(directorypath)

    return _serialize_usage(path)


@router.get(
    '/disk-usage', response_model=DiskUsage, tags=api_tags,
    response_class=FastJsonResponse
)
async def show_disk_usage() -> DiskUsage:
    """
    PIMS disk usage
    """
    return _serialize_usage(Path.from_filepath("."))


class DiskUsageLegacy(BaseModel):
    used: int
    available: int
    usedP: float
    hostname: Optional[str] = None
    mount: Optional[str] = None
    ip: Optional[str] = None


@router.get(
    '/storage/size.json', response_model=DiskUsageLegacy, tags=api_tags,
    response_class=FastJsonResponse
)
async def show_disk_usage_v1():
    """
    Get storage space (v1.x)
    """
    data = _serialize_usage(Path.from_filepath("."))
    return {
        "available": data.mount_available_size,
        "used": data.mount_used_size,
        "usedP": data.mount_used_size_percentage / 100,
        "hostname": None,
        "ip": None,
        "mount": data.mount_point
    }

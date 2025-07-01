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
from enum import Enum
from typing import Optional

from fastapi import Depends, Header

from pims.config import get_settings

DEFAULT_SAFE_MODE = get_settings().default_image_size_safety_mode
DEFAULT_ANNOTATION_ORIGIN = get_settings().default_annotation_origin


def serialize_header(value, style='simple', explode=False):  # noqa
    """
    Serialize a header according to https://swagger.io/docs/specification/serialization/.

    Parameters
    ----------
    value :
        Value to serialize
    style : str ('simple')
        Serialization style.
    explode : bool
        Explode the object serialization.

    Returns
    -------
    str
        Serialized header.
    """
    if type(value) is list:
        return ','.join([str(item) for item in value])
    elif type(value) is dict:
        sep = '=' if explode else ','
        return ','.join(['{}{}{}'.format(k, sep, v) for k, v in value.items()])
    else:
        return str(value)


def add_image_size_limit_header(
    headers: dict, request_width: int, request_height: int, safe_width: int,
    safe_height: int
) -> dict:
    """
    Add X-Image-Size-Limit header to existing header dict if necessary.

    Parameters
    ----------
    headers
        Dict of headers to modify in place
    request_width
        Width requested by the user
    request_height
        Height requested by the user
    safe_width
        Safe width for this request
    safe_height
        Safe height for this request

    Returns
    -------
    headers
        The header dict possibly updated with X-Image-Size-Limit
    """
    ratio = safe_width / request_width
    if ratio != 1.0:
        header = {
            'request_width': request_width,
            'request_height': request_height,
            'safe_width': safe_width,
            'safe_height': safe_height,
            'ratio': ratio
        }
        headers['X-Image-Size-Limit'] = serialize_header(header, explode=True)

    return headers


class SafeMode(str, Enum):
    SAFE_REJECT = "SAFE_REJECT"
    SAFE_RESIZE = "SAFE_RESIZE"
    UNSAFE = "UNSAFE"


def accept_header(
    accept: Optional[str] = Header(None, alias='Accept')
):
    return accept


def safe_mode_header(
    safe_mode: SafeMode = Header(
        DEFAULT_SAFE_MODE,
        alias="X-Image-Size-Safety",
        description="This header provides hints about the way the server has to deal "
                    "with too large image responses.\n"
                    "* `SAFE_REJECT` - Reject too large image response and throw a `400 Bad "
                    "Request`.\n "
                    "* `SAFE_RESIZE` - Resize the image response to an acceptable image size. "
                    "Information about the resize are returned in `X-Image-Size-Limit` header.\n"
                    "* `UNSAFE` - **At your own risk!** Try to fulfill the request but can cause "
                    "unintended side effects (unreadable response, server slow down, server "
                    "failure, "
                    "...). It should only be used in rare controlled situations."
    )
):
    return safe_mode


class AnnotationOrigin(str, Enum):
    LEFT_TOP = "LEFT_TOP"
    LEFT_BOTTOM = "LEFT_BOTTOM"


def annotation_origin_header(
    annot_origin: AnnotationOrigin = Header(
        DEFAULT_ANNOTATION_ORIGIN,
        alias="X-Annotation-Origin",
        description="This header give the origin coordinate system "
                    "in which are described provided annotations."
    )
):
    return annot_origin


class ImageRequestHeaders:
    def __init__(
        self,
        accept: str = Depends(accept_header),
        safe_mode: SafeMode = Depends(safe_mode_header),
    ):
        self.accept = accept
        self.safe_mode = safe_mode

    def get(self, header, default=None):
        return getattr(self, header, default)


class ImageAnnotationRequestHeaders(ImageRequestHeaders):
    def __init__(
        self,
        accept: str = Depends(accept_header),
        safe_mode: SafeMode = Depends(safe_mode_header),
        annot_origin: AnnotationOrigin = Depends(annotation_origin_header)
    ):
        super().__init__(accept, safe_mode)
        self.annot_origin = annot_origin

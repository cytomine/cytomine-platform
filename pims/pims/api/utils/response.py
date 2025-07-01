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
import logging
from typing import Any, Optional

import orjson
from cytomine.models import Model as CytomineModel
from fastapi.responses import ORJSONResponse
from fastapi.types import IncEx
from pint import Quantity
from pydantic import BaseModel, RootModel
from starlette.background import BackgroundTask

log = logging.getLogger("pims")


def response_list(list_):
    """Format a list for response serialization.
    """
    return {
        "items": list_,
        "size": len(list_)
    }


def convert_quantity(quantity: Optional[Quantity], unit: str, ndigits: int = 6) -> Optional[float]:
    """
    Convert a quantity to the unit required by API specification.

    Parameters
    ----------
    quantity
        Quantity to convert
    unit
        Pint understandable unit
    ndigits
        Number of digits to keep for rounding

    Returns
    -------
    float
        Converted quantity to given unit if `quantity` is Quantity
    """
    if quantity is None:
        return None
    elif isinstance(quantity, Quantity):
        return round(quantity.to(unit).magnitude, ndigits)

    log.warning(
        f'The quantity {quantity} is not of type Quantity and is thus not converted.'
    )
    return round(quantity, ndigits)


def serialize_cytomine_model(o):
    if isinstance(o, CytomineModel):
        d = dict((k, v) for k, v in o.__dict__.items() if v is not None and not k.startswith("_"))
        if "uri_" in d:
            d["uri"] = d.pop("uri_")
        return d
    log.warning(f"The object {o} is not a Cytomine model and is thus not serialized.")
    return o


class FastJsonResponse(ORJSONResponse):
    """
    Fast JSON response using `orjson` encoder.

    It bypasses some FastAPI features and has some limitations: some response parts could be
    unvalidated, and rendering (i.e json encoding) may fail if `orjson` is not able to
    serialize the data.

    On large responses, it can be 10-30x faster than default FastAPI encoding.
    """
    def __init__(
        self,
        content: Any = None,
        status_code: int = 200,
        headers: dict = None,
        media_type: str = None,
        background: BackgroundTask = None,
        include: Optional[IncEx]  = None,
        exclude: Optional[IncEx] = None,
        by_alias: bool = True,
        exclude_unset: bool = False,
        exclude_defaults: bool = False,
        exclude_none: bool = False,
    ):
        if include is not None and not isinstance(include, (set, dict)):
            include = set(include)
        if exclude is not None and not isinstance(exclude, (set, dict)):
            exclude = set(exclude)

        self.include = include
        self.exclude = exclude
        self.by_alias = by_alias
        self.exclude_unset = exclude_unset
        self.exclude_defaults = exclude_defaults
        self.exclude_none = exclude_none

        super().__init__(content, status_code, headers, media_type, background)

    def default(self, o: Any):
        if isinstance(o, BaseModel):
            obj_dict = o.model_dump(
                include=self.include,  # type: ignore # in Pydantic
                exclude=self.exclude,  # type: ignore # in Pydantic
                by_alias=self.by_alias,
                exclude_unset=self.exclude_unset,
                exclude_none=self.exclude_none,
                exclude_defaults=self.exclude_defaults,
            )
            if isinstance(o, RootModel):
                obj_dict = obj_dict["root"]
            return obj_dict
        elif isinstance(o, CytomineModel):
            d = dict(
                (k, v) for k, v in o.__dict__.items()
                if v is not None and not k.startswith("_")
            )
            if "uri_" in d:
                d["uri"] = d.pop("uri_")
            return d
        raise TypeError

    def render(self, content: Any) -> bytes:
        assert orjson is not None, "orjson must be installed to use ORJSONResponse"

        return orjson.dumps(
            content, option=orjson.OPT_SERIALIZE_NUMPY, default=self.default
        )

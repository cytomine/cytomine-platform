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
from typing import Any, Dict, List, Optional, Union

from shapely.geometry.base import BaseGeometry

from pims.utils.iterables import ensure_list

PlaneIndex = Union[int, List[int]]


class ParsedMetadataAnnotation:
    """
    Parsed annotation from an image format metadata.
    This is NOT an input annotation.
    """
    def __init__(
        self, geometry: BaseGeometry, c: PlaneIndex, z: PlaneIndex, t: PlaneIndex,
        terms: Optional[List[str]] = None,
        properties: Optional[Dict[str, Any]] = None
    ):
        """
        Initialize an annotation from image metadata.

        Parameters
        ----------
        geometry
            A valid geometry
        c
            The channel(s) which the annotation is linked to
        z
            The z-slices(s) which the annotation is linked to
        t
            The timepoint(s) which the annotation is linked to
        terms
            The terms (labels) associated to the annotation
        properties
            Other properties (key-value pairs) associated to the annotation
        """
        self.geometry = geometry

        self.channels = ensure_list(c)
        self.z_slices = ensure_list(z)
        self.timepoints = ensure_list(t)

        if terms is None:
            terms = []
        self.terms = terms

        if properties is None:
            properties = dict()
        self.properties = properties

    @property
    def wkt(self) -> str:
        return self.geometry.wkt

    def add_term(self, term: str):
        if term not in self.terms:
            self.terms.append(term)

    def add_property(self, key: str, value: Any):
        if key not in self.properties.keys():
            self.properties[key] = value
        else:
            i = 1
            while f"{key}.{i}" in self.properties.keys():
                i = i+1
            self.properties[f"{key}.{i}"] = value

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
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING, Type

if TYPE_CHECKING:
    from pims.formats import AbstractFormat
    from pims.files.file import Path


class AbstractConvertor(ABC):
    """
    Base convertor. All convertors must extend this class.
    """
    def __init__(self, source: AbstractFormat):
        """
        Initializer.

        Parameters
        ----------
        source
            The image format to convert
        """
        self.source = source

    def convert(self, dest_path: Path) -> bool:
        """
        Convert the image in this format to another one at a given destination
        path.

        Returns
        -------
        result
            Whether the conversion succeeded or not
        """
        raise NotImplementedError()

    @abstractmethod
    def conversion_format(self) -> Type[AbstractFormat]:
        """
        Get the format to which the image in this format will be converted,
        if needed.
        """
        raise NotImplementedError()

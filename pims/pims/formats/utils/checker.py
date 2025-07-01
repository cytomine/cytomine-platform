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
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from pims.formats.utils.abstract import CachedDataPath


class AbstractChecker(ABC):
    """
    Base checker. All format checkers must extend this class.
    """

    @classmethod
    @abstractmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        """Whether the path is in this format or not."""
        pass


class SignatureChecker(AbstractChecker, ABC):
    """
    Base signature checker. Add helper to get file signature.
    """
    @classmethod
    def get_signature(cls, pathlike: CachedDataPath) -> bytearray:
        """Get cached file signature"""
        return pathlike.get_cached('signature', pathlike.path.signature)

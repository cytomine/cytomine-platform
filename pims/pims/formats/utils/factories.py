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

from typing import Optional, TYPE_CHECKING

from pims.formats import FORMATS, FormatsByExt
from pims.formats.utils.abstract import AbstractFormat, CachedDataPath

if TYPE_CHECKING:
    from pims.files.file import Path


class FormatFactory:
    def __init__(self, match_on_ext: bool = False, formats: FormatsByExt = None):
        """
        Initialize factory of image formats.

        Parameters
        ----------
        match_on_ext
            Whether to identify the format on PIMS extension basis.
            Extensions are defined by PIMS and does not correspond to regular
            image file extensions. Should be set to True to identify an
            already PIMS-processed image format.
        formats
            The list of formats to test in this factory.
        """
        if formats is None:
            formats = FORMATS
        self.formats = formats
        self.match_on_ext = match_on_ext

    def match(self, path: Path) -> Optional[AbstractFormat]:
        """
        Identify a matching format for given path.

        Parameters
        ----------
        path
            The filepath to try against format checkers.

        Returns
        -------
        format
            An image format initialized for the given filepath, if there is
            a match. None otherwise.
        """
        if self.match_on_ext:
            extension = path.extension
            if len(extension) > 0:
                extension = extension[1:]
            format = self.formats.get(extension)
            if format is not None:
                return format(path)
        proxy = CachedDataPath(path)
        for format in self.formats.values():
            if format.match(proxy):
                return format.from_proxy(proxy)

        return None


class ImportableFormatFactory(FormatFactory):
    def __init__(self, match_on_ext: bool = False):
        formats = {
            e: f
            for e, f in FORMATS.items()
            if f.is_importable()
        }
        super(ImportableFormatFactory, self).__init__(match_on_ext, formats)


class SpatialReadableFormatFactory(FormatFactory):
    def __init__(self, match_on_ext: bool = False):
        formats = {
            e: f
            for e, f in FORMATS.items()
            if f.is_spatial() and f.is_readable()
        }
        super(SpatialReadableFormatFactory, self).__init__(match_on_ext, formats)


class SpectralReadableFormatFactory(FormatFactory):
    def __init__(self, match_on_ext: bool = False):
        formats = {
            e: f
            for e, f in FORMATS.items()
            if f.is_spectral() and f.is_readable()
        }
        super(SpectralReadableFormatFactory, self).__init__(match_on_ext, formats)

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

import shutil
import sys
from functools import lru_cache
from typing import List, Optional, Callable
from zipfile import ZipFile

from pims.api.exceptions import NoMatchingFormatProblem
from pims.files.file import Path


class ArchiveError(OSError):
    pass


class ArchiveFormat:
    def __init__(self, name: str, description: str, match: Callable):
        self.name = name
        self.description = description
        self.match = match

    def get_identifier(self, uppercase: bool = True) -> str:
        if uppercase:
            return self.name.upper()
        return self.name

    def get_name(self) -> str:
        return self.get_identifier(False)

    def get_remarks(self) -> str:
        return self.description


def zip_match(signature: bytearray) -> bool:
    return (len(signature) > 3 and
            signature[0] == 0x50 and signature[1] == 0x4B and
            (signature[2] == 0x3 or signature[2] == 0x5 or
             signature[2] == 0x7) and
            (signature[3] == 0x4 or signature[3] == 0x6 or
             signature[3] == 0x8))


def tar_match(signature: bytearray) -> bool:
    return (len(signature) > 261 and
            signature[257] == 0x75 and
            signature[258] == 0x73 and
            signature[259] == 0x74 and
            signature[260] == 0x61 and
            signature[261] == 0x72)


def gztar_match(signature: bytearray) -> bool:
    return (len(signature) > 2 and
            signature[0] == 0x1F and
            signature[1] == 0x8B and
            signature[2] == 0x8)


def bztar_match(signature: bytearray) -> bool:
    return (len(signature) > 2 and
            signature[0] == 0x42 and
            signature[1] == 0x5A and
            signature[2] == 0x68)


def xztar_match(signature: bytearray) -> bool:
    return (len(signature) > 5 and
            signature[0] == 0xFD and
            signature[1] == 0x37 and
            signature[2] == 0x7A and
            signature[3] == 0x58 and
            signature[4] == 0x5A and
            signature[5] == 0x00)


@lru_cache
def _build_archive_format_list() -> List[ArchiveFormat]:
    formats = []
    extensions = shutil.get_archive_formats()
    for name, description in extensions:
        match_fn_name = f"{name}_match"
        match = getattr(sys.modules[__name__], match_fn_name, None)
        if match is not None:
            formats.append(ArchiveFormat(name, description, match))
    return formats


ARCHIVE_FORMATS = _build_archive_format_list()


class Archive(Path):
    def __init__(self, *pathsegments, format: Optional[ArchiveFormat] = None):
        super().__init__(pathsegments)

        _format = None
        if format:
            _format = format
        else:
            signature = self.signature()
            for possible_format in ARCHIVE_FORMATS:
                if possible_format.match(signature):
                    _format = possible_format
                    break

        if _format is None:
            raise NoMatchingFormatProblem(Path(self))
        else:
            self._format = _format

    def extract(self, path: Path, clean: bool = True):
        """
        Extract the archive content.

        Parameters
        ----------
        path : Path
            A non-existing directory path where the archive content is extracted
        clean : bool (default: True)
            Whether the archive content has to be cleaned from Mac OS hidden
            files (.DS_STORE, __MACOSX).
        """
        if path.exists() and not path.is_dir():
            raise ArchiveError(
                f"{self} cannot be extracted in {path} because "
                f"it already exists or it is not a directory"
            )

        try:
            # shutil.unpack_archive function (prior to python 3.9) performs archive unpacking in memory causing
            # high RAM memory usage for large ZIP archive extraction so need to use another library
            # see /pims-ce/-/issues/89
            if self._format.name.lower == "zip":
                with ZipFile(self.absolute(), 'r') as zip_ref:
                    zip_ref.extractall(path)
            else:
                shutil.unpack_archive(self.absolute(), path, self._format.name)
        except shutil.ReadError as e:
            raise ArchiveError(str(e))

        if clean:
            bad_filenames = ['.DS_STORE', '__MACOSX']
            for bad_filename in bad_filenames:
                for bad_path in path.rglob(bad_filename):
                    bad_path.unlink(missing_ok=True)

    @classmethod
    def from_path(cls, path):
        try:
            return cls(path)
        except NoMatchingFormatProblem:
            return None

    @property
    def format(self) -> ArchiveFormat:
        return self._format


def make_zip_archive(archive_path: Path, content_path: Path) -> ZipFile:
    """
    Make a zip archive at `archive_path` location with `content_path`
    inside.

    Note: cannot use `shutil` to write archives as it is not thread safe !
    """
    def walk(path):
        for p in Path(path).iterdir():
            if p.is_dir():
                yield from walk(p)
                continue
            yield p.resolve()

    with ZipFile(archive_path, 'w') as zipf:
        for file in walk(content_path):
            zipf.write(file, file.relative_to(content_path))

    return zipf

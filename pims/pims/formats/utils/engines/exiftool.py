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

import json
import subprocess
from abc import ABC
from typing import TYPE_CHECKING

from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.structures.metadata import MetadataStore

if TYPE_CHECKING:
    from pims.files.file import Path


def is_valid_key(key: str) -> bool:
    """Check if an exiftool key is valid and interesting."""
    # https://exiftool.org/TagNames/Extra.html
    file_keys = (
        'FileName', 'Directory', 'FileSize', 'FileModifyDate', 'FileAccessDate',
        'FileInodeChangeDate', 'FilePermissions', 'FileType', 'FileType',
        'FileTypeExtension', 'MIMEType', 'ExifByteOrder'
    )
    invalid_prefixes = ("ExifTool", "System", "SourceFile") + tuple(
        f"File:{k}" for k in file_keys
    )
    for invalid_prefix in invalid_prefixes:
        if key.startswith(invalid_prefix):
            return False

    return True


def read_raw_metadata(path: Path) -> dict:
    """
    Extract raw metadata using Exiftool.
    WARNING: this function is SLOW! Only to be used for raw metadata!
    """
    bytes_info = "use -b option to extract)"

    exiftool_exc = "exiftool"
    exiftool_opts = ["-All", "-s", "-G", "-j", "-u", "-e"]
    args = [exiftool_exc] + exiftool_opts + [str(path)]
    result = subprocess.run(args, capture_output=True)
    if result.returncode == 0:
        metadata = json.loads(result.stdout)
        if type(metadata) == list and len(metadata) > 0:
            metadata = metadata[0]
        if type(metadata) != dict:
            return dict()
        return {
            k.replace(":", "."): v.strip() if type(v) == str else v
            for k, v in metadata.items()
            if is_valid_key(k) and bytes_info not in str(v)
        }

    return dict()


class ExifToolParser(AbstractParser, ABC):
    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()

        raw = read_raw_metadata(self.format.path)
        for key, value in raw.items():
            store.set(key, value)

        return store

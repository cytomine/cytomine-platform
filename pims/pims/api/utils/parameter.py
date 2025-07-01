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

from fastapi.params import Path as PathParam
from pathvalidate import sanitize_filename as _sanitize_filename

from pims.api.exceptions import NoAppropriateRepresentationProblem, FilepathNotFoundProblem
from pims.files.file import Path

def filepath_parameter(
    filepath: str = PathParam(
        ..., description="The file path, relative to server base path.",
        examples=['123/my-file.ext']
    ),
):
    path = Path.from_filepath(filepath)
    if not path.exists():
        raise FilepathNotFoundProblem(path)
    return path


def imagepath_parameter(
    filepath: str = PathParam(
        ..., description="The file path, relative to server base path.",
        examples=['123/my-file.ext']
    )
):
    path = Path.from_filepath(filepath)
    if not path.exists():
        raise FilepathNotFoundProblem(path)
    if not path.is_single():
        raise NoAppropriateRepresentationProblem(path)
    return path


def sanitize_filename(filename: str, replacement="-"):
    sanitized = _sanitize_filename(filename, replacement_text=replacement)
    bad_chars = [" ", "(", ")", "+", "*", "/", "@", "'", '"',
                 '$', '€', '£', '°', '`', '[', ']', '#', '?']
    return "".join(c if c not in bad_chars else replacement for c in sanitized)

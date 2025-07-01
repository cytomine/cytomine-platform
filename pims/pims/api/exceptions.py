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

from typing import TYPE_CHECKING

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

if TYPE_CHECKING:
    from pims.files.file import Path

class ProblemException(Exception):
    def __init__(self, status, title=None, detail=None, **ext):
        self.status: int = status
        self.title: str = title
        self.detail = detail
        self.ext = ext

class UploadCanceledException(Exception):
    def __init__(self, message="Upload was canceled"):
        self.message = message
        super().__init__(self.message)
        
class BadRequestException(ProblemException):
    def __init__(self, title="Bad Request", detail=None, **ext):
        super().__init__(400, title, detail, **ext)


class NotFoundException(ProblemException):
    def __init__(self, title="Not Found", detail=None, **ext):
        super().__init__(404, title, detail, **ext)


class NotAcceptableException(ProblemException):
    def __init__(self, title="Not Acceptable", detail=None, **ext):
        super().__init__(406, title, detail, **ext)


class AuthenticationException(ProblemException):
    def __init__(self, title="Unauthorized", detail=None, **ext):
        super().__init__(401, title, detail, **ext)


def add_problem_exception_handler(app: FastAPI):
    @app.exception_handler(ProblemException)
    def problem_exception_handler(request: Request, exc: ProblemException):
        content = {
            "title": exc.title,
            "details": exc.detail
        }
        if exc.ext:
            content.update(exc.ext)

        return JSONResponse(
            status_code=exc.status,
            content=content
        )


class FilepathNotFoundProblem(NotFoundException):
    def __init__(self, filepath: Path):
        filepath = filepath.public_filepath
        title = 'Filepath not found'
        detail = f'The filepath {filepath} does not exist.'
        super().__init__(title, detail)


class NoAppropriateRepresentationProblem(NotFoundException):
    def __init__(self, filepath: Path, representation=None):
        filepath = filepath.public_filepath

        title = 'No appropriate representation found'
        detail = f'The filepath {filepath} does not have an appropriate representation'
        if representation:
            detail += f' (expected {representation})'
        super().__init__(title, detail, representation=representation)


class NotADirectoryProblem(BadRequestException):
    def __init__(self, filepath: Path):
        filepath = filepath.public_filepath

        title = 'Not a directory'
        detail = f'The filepath {filepath} is not a directory'
        super().__init__(title, detail)


class NotAFileProblem(BadRequestException):
    def __init__(self, filepath: Path):
        filepath = filepath.public_filepath

        title = 'Not a file'
        detail = f'The filepath {filepath} is not a file'
        super().__init__(title, detail)


class NoMatchingFormatProblem(BadRequestException):
    def __init__(self, filepath: Path):
        filepath = filepath.public_filepath

        title = "No matching format found"
        detail = f"The filepath {filepath} is recognized by any of the available formats."
        super().__init__(title, detail)


class MetadataParsingProblem(BadRequestException):
    def __init__(self, filepath: Path, detail=None, **ext):
        filepath = filepath.public_filepath

        title = "Metadata cannot be correctly understood."
        if detail is None:
            detail = f"The filepath {filepath} has unsupported metadata."
        super().__init__(title, detail, **ext)


class PyramidParsingProblem(BadRequestException):
    def __init__(self, filepath: Path, detail=None, **ext):
        filepath = filepath.public_filepath

        title = "Pyramid cannot be correctly understood"
        super().__init__(title, detail, **ext)


class FormatNotFoundProblem(NotFoundException):
    def __init__(self, format_id):
        title = 'Format not found'
        detail = f'The format {format_id} does not exist.'
        super().__init__(title, detail)


class FilterNotFoundProblem(NotFoundException):
    def __init__(self, format_id):
        title = 'Filter not found'
        detail = f'The filter {format_id} does not exist.'
        super().__init__(title, detail)


class ColormapNotFoundProblem(NotFoundException):
    def __init__(self, colormap_id):
        title = 'Colormap not found'
        detail = f'The colormap {colormap_id} does not exist.'
        super().__init__(title, detail)


class NoAcceptableResponseMimetypeProblem(NotAcceptableException):
    def __init__(self, accept_header, supported_mimetypes):
        title = 'No acceptable response mime type'
        detail = 'There is no acceptable response mime type in Accept header.'
        ext = {
            'accept_header': accept_header,
            'supported_mimetypes': supported_mimetypes
        }
        super().__init__(title, detail, **ext)


class TooLargeOutputProblem(BadRequestException):
    def __init__(self, width, height, max_size):
        title = 'Too large image output dimensions.'
        detail = 'Requested output dimensions exceed maximum admissible size. ' \
                 'The request has been rejected as X-Image-Size-Safety header is set to ' \
                 'SAFE_REJECT. '
        ext = {
            "request_width": width,
            "request_height": height,
            "max_size": max_size
        }
        super().__init__(title, detail, **ext)


class CytomineProblem(BadRequestException):
    def __init__(self, detail):
        title = 'Cytomine core communication error'
        super().__init__(title, detail)


class InvalidGeometryException(BadRequestException):
    def __init__(self, geometry: str, reason: str):
        title = 'Invalid geometry'
        detail = f'Geometry {geometry} is invalid.'
        ext = {
            "geometry": geometry,
            "reason": reason
        }
        super().__init__(title, detail, **ext)


def check_path_existence(path):
    if not path or not path.exists():
        raise FilepathNotFoundProblem(path)


def check_path_is_single(path):
    if not path.is_single():
        raise NoAppropriateRepresentationProblem(path)


def check_representation_existence(path):
    if not path or not path.exists():
        raise NoAppropriateRepresentationProblem(path)

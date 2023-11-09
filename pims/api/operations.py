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
import os
import shutil
import traceback
from typing import Optional
from distutils.util import strtobool
import aiofiles

from cytomine import Cytomine
from cytomine.models import (
    Project, ProjectCollection, Storage, UploadedFile
)
from fastapi import APIRouter, BackgroundTasks, Depends, File, Form, Query, UploadFile
from starlette.requests import Request
from starlette.responses import FileResponse, JSONResponse, Response
from starlette.formparsers import MultiPartMessage, MultiPartParser, _user_safe_decode

from pims.api.exceptions import (
    AuthenticationException, BadRequestException, CytomineProblem, UploadCanceledException,
    check_representation_existence
)
from pims.api.utils.cytomine_auth import (
    get_this_image_server, parse_authorization_header,
    parse_request_token, sign_token
)
from pims.api.utils.parameter import filepath_parameter, imagepath_parameter, sanitize_filename
from pims.api.utils.response import serialize_cytomine_model
from pims.config import Settings, get_settings
from pims.files.archive import make_zip_archive
from pims.files.file import Path
from pims.importer.importer import run_import
from pims.importer.listeners import CytomineListener
from pims.tasks.queue import Task, send_task
from pims.utils.iterables import ensure_list
from pims.utils.strings import unique_name_generator
from pims.files.archive import Archive, ArchiveError

try:
    import multipart
    from multipart.multipart import parse_options_header
except ModuleNotFoundError:  # pragma: nocover
    parse_options_header = None
    multipart = None

router = APIRouter()

cytomine_logger = logging.getLogger("pims.cytomine")

WRITING_PATH = get_settings().writing_path

@router.post('/upload', tags=['Import'])
async def import_direct_chunks(
    request: Request,
    background: BackgroundTasks,
    core: Optional[str] = None,
    cytomine: Optional[str] = None,
    storage: Optional[int] = None,
    id_storage: Optional[int] = Query(None, alias='idStorage'),
    projects: Optional[str] = None,
    id_project: Optional[str] = Query(None, alias='idProject'),
    sync: Optional[bool] = False,
    keys: Optional[str] = None,
    values: Optional[str] = None,
    config: Settings = Depends(get_settings)
):
    ''' Upload file using the request inspired by UploadFile class from FastAPI along with improved efficiency '''

    multipart_parser = MultiPartParser(request.headers, request.stream())
    filename = str(unique_name_generator())
    if not os.path.exists(WRITING_PATH):
        os.makedirs(WRITING_PATH)

    pending_path = Path(WRITING_PATH, filename)

    try:
        upload_name = await write_file(multipart_parser, pending_path)
        upload_size = request.headers['content-length']

        # Use non sanitized upload_name as UF originalFilename attribute
        cytomine, cytomine_auth, root = connexion_to_core(
            request, core, cytomine, str(pending_path), upload_size, upload_name,  id_project, id_storage,
            projects, storage, config, keys, values
        )

        # Sanitized upload name is used for path on disk in the import procedure (part of UF filename attribute)
        upload_name = sanitize_filename(upload_name)
    except Exception as e:
        debug = bool(strtobool(os.getenv('DEBUG', 'false')))
        if debug:
            traceback.print_exc()
        os.remove(pending_path)
        return JSONResponse(
            content=[{
                "status": 500,
                "error": str(e),
                "files": [{
                    "size": 0,
                    "error": str(e)
                }]
            }], status_code=400
        )

    if sync:
        try:
            run_import(
                pending_path, upload_name,
                extra_listeners=[cytomine], prefer_copy=False
            )
            root = cytomine.initial_uf.fetch()
            images = cytomine.images
            return [{
                "status": 200,
                "name": upload_name,
                "size" : upload_size,
                "uploadedFile": serialize_cytomine_model(root),
                "images": [{
                    "image": serialize_cytomine_model(image[0]),
                    "imageInstances": serialize_cytomine_model(image[1])
                } for image in images]
            }]
        except Exception as e:
            traceback.print_exc()
            return JSONResponse(
                content=[{
                    "status": 500,
                    "error": str(e),
                    "files": [{
                        "size": 0,
                        "error": str(e)
                    }]
                }], status_code=400
            )
    else:
        send_task(
            Task.IMPORT_WITH_CYTOMINE,
            args=[cytomine_auth, pending_path, upload_name, cytomine, False],
            starlette_background=background
        )

        return JSONResponse(
            content=[{
                "status": 200,
                "name": upload_name,
                "size" : upload_size,
                "uploadedFile": serialize_cytomine_model(root),
                "images": []
            }], status_code=200
        )


def import_(filepath, body):
    pass


@router.get('/file/{filepath:path}/export', tags=['Export'])
def export_file(
    background: BackgroundTasks,
    path: Path = Depends(filepath_parameter),
    filename: Optional[str] = Query(None, description="Suggested filename for returned file")
):
    """
    Export a file. All files with an identified PIMS role in the server base path can be exported.
    """
    if not (path.has_upload_role() or path.has_original_role() or path.has_spatial_role() or path.has_spectral_role()):
        raise BadRequestException()

    path = path.resolve()
    if filename is not None:
        exported_filename = filename
    else:
        exported_filename = path.name

    media_type = "application/octet-stream"
    if path.is_dir():
        tmp_export = Path(f"/tmp/{unique_name_generator()}")
        make_zip_archive(tmp_export, path)

        def cleanup(tmp):
            tmp.unlink(missing_ok=True)

        background.add_task(cleanup, tmp_export)
        exported = tmp_export

        if not exported_filename.endswith(".zip"):
            exported_filename += ".zip"

        media_type = "application/zip"
    else:
        exported = path

    return FileResponse(
        exported,
        media_type=media_type,
        filename=exported_filename
    )


@router.get('/image/{filepath:path}/export', tags=['Export'])
def export_upload(
    background: BackgroundTasks,
    path: Path = Depends(imagepath_parameter),
    filename: Optional[str] = Query(None, description="Suggested filename for returned file")
):
    """
    Export the upload representation of an image.
    """
    image = path.get_original()
    check_representation_existence(image)

    upload_file = image.get_upload().resolve()

    if filename is not None:
        exported_filename = filename
    else:
        exported_filename = upload_file.name

    media_type = image.media_type
    if upload_file.is_dir():
        # if archive has been deleted
        tmp_export = Path(f"/tmp/{unique_name_generator()}")
        make_zip_archive(tmp_export, upload_file)

        def cleanup(tmp):
            tmp.unlink(missing_ok=True)

        background.add_task(cleanup, tmp_export)
        upload_file = tmp_export

        if not exported_filename.endswith(".zip"):
            exported_filename += ".zip"

        media_type = "application/zip"

    return FileResponse(
        upload_file,
        media_type=media_type,
        filename=exported_filename
    )


@router.delete('/image/{filepath:path}', tags=['delete'])
def delete(    
    path: Path = Depends(imagepath_parameter),
):
    """
    Delete the all the representations of an image, including the related upload folder.
    """

    # Deleting an archive will be refused as it is not an *image* but a collection
    # (checked in `Depends(imagepath_parameter)`)
    image = path.get_original()
    check_representation_existence(image)
    image.delete_upload_root()

    return Response(status_code=200)



async def write_file(fastapi_parser: MultiPartParser, pending_path):
    '''
    This function is inspired by parse(self) function from formparsers.py in fastapi>=0.65.1,<=0.68.2' used to upload a file:

    We know that, besides the first chunks where it is useful to retrieve the headers "Content-Disposition" and the "Content-Type" 
    (and not write them in the file) , all the other chunks will be bytes of the image to upload an can be written right away in a file on disk.
    Therefore, we can get inspired by the parse() function of FastAPI and, by assuming that there is only one file per request
    (we do not handle multiple file upload) and no other key-value pairs, we can parse the first chunks until the headers are finished to retrieve 
    the headers "Content-Disposition" and the "Content-Type" (to get the filename) by calling process_chunks_headers(). Once the headers are process,
    we can directly write the bytes into a file.
    
    '''

    _, params = parse_options_header(fastapi_parser.headers["Content-Type"])
    charset = params.get(b"charset", "utf-8")
    if type(charset) == bytes:
        charset = charset.decode("latin-1")
    fastapi_parser._charset = charset
    original_filename = "no-name"

    boundary = params[b"boundary"]
    headers_finised = False
    callbacks = {
            "on_part_data": fastapi_parser.on_part_data,
            "on_header_field": fastapi_parser.on_header_field,
            "on_header_value": fastapi_parser.on_header_value,
            "on_header_end": fastapi_parser.on_header_end,
            "on_headers_finished": fastapi_parser.on_headers_finished,
        }
    parser = multipart.MultipartParser(boundary,callbacks)
    async with aiofiles.open(pending_path, 'wb') as f:
        try:
            async for chunk in fastapi_parser.stream:
                # we assume that there is only one key-value in the body request (that is only one file to upload and no other parameter in the request such taht there is only one headers block)
                if not headers_finised:#going through the one-only headers block of the body request and retrieve the filename
                    original_filename, headers_finised = await process_chunks_headers(parser, fastapi_parser, chunk, f, original_filename=original_filename)
                else: #enables more efficient upload by by-passing the mutlipart parser logic and just writing the data bytes directly
                    await f.write(chunk)
        except ClientDisconnect:
            raise UploadCanceledException()


    return original_filename

async def process_chunks_headers(parser, fastapi_parser, chunk, file, header_field: bytes =b"", header_value: bytes =b"", original_filename='no-name'):
    ''' 
    This function is inspired by parse(self) function from formparsers.py in fastapi>=0.65.1,<=0.68.2' used to upload a file:
    
    '''

    parser.write(chunk) # when this line is run at each chunk, it is time-consuming for big files 
    messages = list(fastapi_parser.messages)
    fastapi_parser.messages.clear()
    for message_type, message_bytes in messages:
        if message_type == MultiPartMessage.HEADER_FIELD:
            header_field += message_bytes
        elif message_type == MultiPartMessage.HEADER_VALUE:
            header_value += message_bytes
        elif message_type == MultiPartMessage.HEADER_END:
            field = header_field.lower()
            if field == b"content-disposition":
                content_disposition = header_value
        elif message_type == MultiPartMessage.HEADERS_FINISHED:
            headers_finished = True
            _, options = parse_options_header(content_disposition)
            if b"filename" in options:
                original_filename = _user_safe_decode(options[b"filename"], fastapi_parser._charset)
        elif message_type == MultiPartMessage.PART_DATA:
                await file.write(message_bytes)
    return original_filename, headers_finished

def connexion_to_core(request: Request, core: str, cytomine: str, upload_path: str, upload_size: str, upload_name: str,  id_project: str, id_storage: str, projects: str, storage: str, 
                      config: Settings,  keys: str, values: str):
    
    core = cytomine if cytomine is not None else core
    if not core:
        raise BadRequestException(detail="core or cytomine parameter missing.")

    id_storage = id_storage if id_storage is not None else storage
    if not id_storage:
        raise BadRequestException(detail="idStorage or storage parameter missing.")

    projects_to_parse = id_project if id_project is not None else projects
    try:
        id_projects = []
        if projects_to_parse:
            projects = ensure_list(projects_to_parse.split(","))
            id_projects = [int(p) for p in projects]
    except ValueError:
        raise BadRequestException(detail="Invalid projects or idProject parameter.")

    public_key, signature = parse_authorization_header(request.headers)
    cytomine_auth = (core, config.cytomine_public_key, config.cytomine_private_key)
    with Cytomine(*cytomine_auth, configure_logging=False) as c:
        if not c.current_user:
            raise AuthenticationException("PIMS authentication to Cytomine failed.")

        this = get_this_image_server(config.pims_url)
        cyto_keys = c.get(f"userkey/{public_key}/keys.json")
        private_key = cyto_keys["privateKey"]

        if sign_token(private_key, parse_request_token(request)) != signature:
            raise AuthenticationException("Authentication to Cytomine failed")

        c.set_credentials(public_key, private_key)
        user = c.current_user
        storage = Storage().fetch(id_storage)
        if not storage:
            raise CytomineProblem(f"Storage {id_storage} not found")

        projects = ProjectCollection()
        for pid in id_projects:
            project = Project().fetch(pid)
            if not project:
                raise CytomineProblem(f"Project {pid} not found")
            projects.append(project)

        keys = keys.split(',') if keys is not None else []
        values = values.split(',') if values is not None else []
        if len(keys) != len(values):
            raise CytomineProblem(f"Keys {keys} and values {values} have varying size.")
        user_properties = zip(keys, values)

        root = UploadedFile(
            upload_name, upload_path, upload_size, "", "",
            id_projects, id_storage, user.id, this.id, UploadedFile.UPLOADED
        )

        cytomine = CytomineListener(
            cytomine_auth, root, projects=projects,
            user_properties=user_properties
        )
    return cytomine, cytomine_auth, root


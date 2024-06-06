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
import traceback
import warnings
from distutils.util import strtobool
from typing import Optional

from cytomine import Cytomine
from cytomine.models import (
    Project, ProjectCollection, Storage, UploadedFile
)
from fastapi import APIRouter, BackgroundTasks, Depends, Query
from starlette.requests import Request
from starlette.responses import FileResponse, JSONResponse, Response

from pims.api.exceptions import (
    AuthenticationException, BadRequestException, CytomineProblem, check_representation_existence
)
from pims.api.utils.cytomine_auth import (
    parse_authorization_header,
    parse_request_token, sign_token
)
from pims.api.utils.multipart import FastSinglePartParser
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

router = APIRouter(prefix=get_settings().api_base_path)

cytomine_logger = logging.getLogger("pims.cytomine")

WRITING_PATH = get_settings().writing_path
INTERNAL_URL_CORE = get_settings().internal_url_core


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
    """
    Upload file using the request inspired by UploadFile class from FastAPI along with improved efficiency
    """

    if (core is not None and core != INTERNAL_URL_CORE) or (cytomine is not None and cytomine != INTERNAL_URL_CORE):
        warnings.warn("This Cytomine version no longer support PIMS to be shared between multiple CORE such that \
                      query parameters 'core' and 'cytomine' are ignored if instanciated")

    if not os.path.exists(WRITING_PATH):
        os.makedirs(WRITING_PATH)

    filename = str(unique_name_generator())
    pending_path = Path(WRITING_PATH, filename)

    try:
        multipart_parser = FastSinglePartParser(pending_path, request.headers, request.stream())
        upload_filename = await multipart_parser.parse()
        upload_size = request.headers['content-length']

        # Use non sanitized upload_name as UF originalFilename attribute
        cytomine_listener, cytomine_auth, root = connexion_to_core(
            request, str(pending_path), upload_size, upload_filename, id_project, id_storage,
            projects, storage, config, keys, values
        )

        # Sanitized upload name is used for path on disk in the import procedure (part of UF filename attribute)
        upload_filename = sanitize_filename(upload_filename)
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
                pending_path, upload_filename,
                extra_listeners=[cytomine_listener], prefer_copy=False
            )
            root = cytomine_listener.initial_uf.fetch()
            images = cytomine_listener.images
            return [{
                "status": 200,
                "name": upload_filename,
                "size": upload_size,
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
            args=[cytomine_auth, pending_path, upload_filename, cytomine_listener, False],
            starlette_background=background
        )

        return JSONResponse(
            content=[{
                "status": 200,
                "name": upload_filename,
                "size": upload_size,
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


def connexion_to_core(
        request: Request, upload_path: str, upload_size: str, upload_name: str, id_project: Optional[str],
        id_storage: Optional[int], projects: Optional[str], storage: Optional[int],
        config: Settings, keys: Optional[str], values: Optional[str]
):
    if not INTERNAL_URL_CORE:
        raise BadRequestException(detail="Internal URL core is missing.")

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
    cytomine_auth = (INTERNAL_URL_CORE, config.cytomine_public_key, config.cytomine_private_key)
    with Cytomine(*cytomine_auth, configure_logging=False) as c:
        if not c.current_user:
            raise AuthenticationException("PIMS authentication to Cytomine failed.")

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
            id_projects, id_storage, user.id, status=UploadedFile.UPLOADED
        )

        cytomine_listener = CytomineListener(
            cytomine_auth, root, projects=projects,
            user_properties=user_properties
        )
    return cytomine_listener, cytomine_auth, root

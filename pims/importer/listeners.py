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
from copy import copy
from enum import Enum
from typing import Dict, Iterator, Optional, Tuple, Union

from cytomine.models import (
    AbstractImage, AbstractSlice, AbstractSliceCollection, Annotation, AnnotationCollection,
    ImageInstance,
    ProjectCollection,
    Property,
    PropertyCollection, Term, TermCollection, UploadedFile
)
from cytomine.models.collection import CollectionPartialUploadException

from pims.api.utils.response import convert_quantity
from pims.config import get_settings
from pims.files.file import Path, Image
from pims.formats import AbstractFormat
from pims.utils.dtypes import dtype_to_bits
from pims.utils.iterables import flatten
from pims.utils.types import parse_int

PENDING_PATH = Path(get_settings().pending_path)
FILE_ROOT_PATH = Path(get_settings().root)

# TODO: move to Cytomine Python client.
UploadedFile.CHECKING_INTEGRITY = 60
UploadedFile.ERROR_INTEGRITY = 61
UploadedFile.UNPACKING = 50
UploadedFile.ERROR_UNPACKING = 51
UploadedFile.UNPACKED = 106
UploadedFile.ERROR_STORAGE = UploadedFile.ERROR_DEPLOYMENT
UploadedFile.ERROR_UNEXPECTED = UploadedFile.ERROR_DEPLOYMENT
UploadedFile.IMPORTED = UploadedFile.CONVERTED


class ImportEventType(str, Enum):
    START_DATA_EXTRACTION = "start_data_extraction"
    END_DATA_EXTRACTION = "end_data_extraction"
    MOVED_PENDING_FILE = "moved_pending_file"

    START_FORMAT_DETECTION = "start_format_detection"
    END_FORMAT_DETECTION = "end_format_detection"
    ERROR_NO_FORMAT = "error_no_format"

    START_INTEGRITY_CHECK = "start_integrity_check"
    END_INTEGRITY_CHECK = "end_integrity_check"
    ERROR_INTEGRITY_CHECK = "error_integrity"

    END_SUCCESSFUL_IMPORT = "end_successful_import"

    START_CONVERSION = "start_conversion"
    END_CONVERSION = "end_conversion"
    ERROR_CONVERSION = "error_conversion"

    START_UNPACKING = "start_unpacking"
    END_UNPACKING = "end_unpacking"
    ERROR_UNPACKING = "error_unpacking"
    REGISTER_FILE = "register_file"

    START_SPATIAL_DEPLOY = "start_spatial_deploy"
    END_SPATIAL_DEPLOY = "end_spatial_deploy"

    START_HISTOGRAM_DEPLOY = "start_histogram_deploy"
    END_HISTOGRAM_DEPLOY = "end_histogram_deploy"
    ERROR_HISTOGRAM = "error_histogram"

    FILE_NOT_MOVED = "file_not_moved"
    FILE_NOT_FOUND = "file_not_found"
    FILE_ERROR = "generic_file_error"


class ImportListener:
    def __repr__(self):
        return self.__class__.__name__

    def start_data_extraction(self, path: Path, *args, **kwargs):
        pass

    def moved_pending_file(self, old_path: Path, new_path: Path, *args, **kwargs):
        pass

    def end_data_extraction(self, path: Path, *args, **kwargs):
        pass

    def start_format_detection(self, path: Path, *args, **kwargs):
        pass

    def end_format_detection(self, path: Path, format: AbstractFormat, *args, **kwargs):
        pass

    def error_no_format(self, path: Path, *args, **kwargs):
        pass

    def start_unpacking(self, path: Path, *args, **kwargs):
        pass

    def end_unpacking(
        self, path: Path, unpacked_path: Path, *args,
        format: AbstractFormat = None, is_collection: bool = False, **kwargs
    ):
        pass

    def error_unpacking(self, path: Path, *args, **kwargs):
        pass

    def register_file(self, path: Path, parent_path: Path, *args, **kwargs):
        pass

    def start_integrity_check(self, path: Path, *args, **kwargs):
        pass

    def end_integrity_check(self, path: Path, *args, **kwargs):
        pass

    def error_integrity(self, path: Path, *args, **kwargs):
        pass

    def start_conversion(self, path: Path, parent_path: Path, *args, **kwargs):
        pass

    def end_conversion(self, path: Path, *args, **kwargs):
        pass

    def error_conversion(self, path: Path, *args, **kwargs):
        pass

    def start_spatial_deploy(self, path: Path, *args, **kwargs):
        pass

    def end_spatial_deploy(self, spatial_path: Path, *args, **kwargs):
        pass

    def start_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        pass

    def end_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        pass

    def error_histogram(self, hist_path: Path, image: Image, *args, **kwargs):
        pass

    def end_successful_import(self, path: Path, image: Image, *args, **kwargs):
        pass

    def file_not_found(self, path: Path, *args, **kwargs):
        pass

    def file_not_moved(self, path: Path, *args, **kwargs):
        pass

    def generic_file_error(self, path: Path, *args, **kwargs):
        pass


class CytomineListener(ImportListener):
    def __init__(
        self, auth: Tuple[str, str, str], uf: UploadedFile,
        root: Optional[UploadedFile] = None,
        existing_mapping: Optional[Dict[str, UploadedFile]] = None,
        projects: Optional[ProjectCollection] = None,
        user_properties: Optional[Iterator[Tuple[str, str]]] = None
    ):
        """
        Parameters
        ----------
        uf : UploadedFile
            The UploadedFile associated to the listener.
            The UploadedFile can be new, i.e. not yet saved.
        root : UploadedFile (optional).
            If set, it is supposed to already exist, i.e not new.
        """
        self.auth = auth
        self.path_uf_mapping = dict()

        if uf.is_new():
            uf.save()
        self.path_uf_mapping[uf.path] = uf
        self.initial_uf = uf

        if existing_mapping is not None:
            self.path_uf_mapping.update(copy(existing_mapping))
        else:
            if root is None:
                if uf.parent is not None:
                    root = UploadedFile().fetch(uf.parent)
                else:
                    root = uf

            while root is not None:
                self.path_uf_mapping[root.path] = root
                self.root_path = root.path
                if root.parent is None:
                    root = None
                else:
                    root = UploadedFile().fetch(root.parent)

        self.abstract_images = []
        self.projects = projects
        self.user_properties = user_properties
        self.images = []

    def new_listener_from_registered_child(self, child: Path):
        uf = self.get_uf(str(child))
        return CytomineListener(
            self.auth, uf, existing_mapping=self.path_uf_mapping,
            projects=self.projects, user_properties=self.user_properties
        )

    def _find_uf_by_id(self, id: int) -> UploadedFile:
        return next(
            (uf for uf in self.path_uf_mapping.values() if uf.id == id),
            UploadedFile().fetch(id)
        )

    def get_uf(self, path: Union[str, Path]) -> UploadedFile:
        uf = self.path_uf_mapping.get(str(path))
        if not uf:
            path = path.resolve()
            uf = self.path_uf_mapping.get(str(path))
            if not uf:
                raise KeyError(f"No UploadedFile found for {path}")
            self.path_uf_mapping[str(path)] = uf
        return uf

    @staticmethod
    def _corresponding_error_status(status: int) -> int:
        if status < 100:
            return status + 1
        else:
            return UploadedFile.ERROR_UNEXPECTED

    def propagate_error(self, uf: UploadedFile):
        # Shouldn't be a core responsibility ?
        if uf.parent:
            parent = self._find_uf_by_id(uf.parent)
            parent.status = uf.status
            parent.update()
            self.propagate_error(parent)

    def start_data_extraction(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.EXTRACTING_DATA
        uf.update()

    def moved_pending_file(self, old_path: Path, new_path: Path, *args, **kwargs):
        uf = self.get_uf(old_path)
        uf.filename = str(new_path.relative_to(FILE_ROOT_PATH))
        uf.update()
        self.path_uf_mapping[str(new_path)] = uf

    def end_data_extraction(self, path: Path, *args, **kwargs):
        pass

    def start_format_detection(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.DETECTING_FORMAT
        uf.update()

    def end_format_detection(self, path: Path, format: AbstractFormat, *args, **kwargs):
        uf = self.get_uf(path)
        uf.contentType = format.get_identifier()  # TODO: not the content type
        uf.update()

    def error_no_format(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.ERROR_FORMAT
        uf.update()
        self.propagate_error(uf)

    def start_unpacking(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.UNPACKING
        uf.update()

    def end_unpacking(
        self, path: Path, unpacked_path: Path, *args,
        format: AbstractFormat = None, is_collection: bool = False, delete_zip: bool = False, **kwargs
    ):
        parent = self.get_uf(path)
        parent.status = UploadedFile.UNPACKED
        parent.update()
        # If archive is a collection and archive deletion is asked after unpacking, PIMS cannot take the
        # responsibility to delete directly the archive uploaded file. Cytomine clients are asynchronous, so they
        # may not be informed that the archive uploaded file status has been set as "unpacked"

        if not is_collection:
            uf = parent if delete_zip else UploadedFile()
            uf.status = UploadedFile.UPLOADED  # Better status ?
            uf.contentType = format.get_identifier()
            uf.size = unpacked_path.size
            uf.filename = str(unpacked_path.relative_to(FILE_ROOT_PATH))
            uf.originalFilename = str(format.main_path.name)
            uf.ext = ""
            uf.storage = parent.storage
            uf.user = parent.user
            uf.parent = None if delete_zip else parent.id
            uf.imageServer = parent.imageServer
            uf.save()
            self.path_uf_mapping[str(unpacked_path)] = uf
            if delete_zip:
                self.path_uf_mapping[str(path)] = uf

    def register_file(self, path: Path, parent_path: Path, *args, delete_zip: bool = False, **kwargs):
        parent = self.get_uf(parent_path)

        uf = UploadedFile()
        uf.status = UploadedFile.UPLOADED
        uf.contentType = ""
        uf.size = path.size
        uf.filename = str(path.relative_to(FILE_ROOT_PATH))
        uf.originalFilename = str(path.name)
        uf.ext = ""
        uf.storage = parent.storage
        uf.user = parent.user
        uf.parent = None if delete_zip else parent.id
        uf.imageServer = parent.imageServer
        uf.save()
        self.path_uf_mapping[str(path)] = uf

    def error_unpacking(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.ERROR_UNPACKING
        uf.update()
        self.propagate_error(uf)

    def start_integrity_check(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.CHECKING_INTEGRITY
        uf.update()

    def end_integrity_check(self, path: Path, *args, **kwargs):
        pass

    def error_integrity(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.ERROR_INTEGRITY
        uf.update()
        self.propagate_error(uf)

    def start_conversion(self, path: Path, parent_path: Path, *args, **kwargs):
        uf = UploadedFile()
        uf.status = UploadedFile.CONVERTING
        uf.originalFilename = path.name
        uf.filename = str(path.relative_to(FILE_ROOT_PATH))
        uf.size = 0
        uf.ext = ""
        uf.contentType = ""

        parent = self.get_uf(parent_path)
        uf.storage = parent.storage
        uf.user = parent.user
        uf.parent = parent.id
        uf.imageServer = parent.imageServer
        uf.save()
        self.path_uf_mapping[str(path)] = uf

        parent.status = UploadedFile.CONVERTING
        parent.update()

    def end_conversion(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.size = path.size
        # uf.status = UploadedFile.CONVERTED
        uf.update()

    def error_conversion(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = UploadedFile.ERROR_CONVERSION
        uf.update()
        self.propagate_error(uf)

    def start_spatial_deploy(self, path: Path, *args, **kwargs):
        pass

    def end_spatial_deploy(self, spatial_path: Path, *args, **kwargs):
        if not spatial_path.is_symlink():
            # The spatial path is not a symbolic link
            # -> a conversion has been performed
            uf = self.get_uf(spatial_path)
            uf.status = UploadedFile.IMPORTED
            uf.update()

    def start_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        pass  # TODO ?

    def end_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        pass  # TODO ?

    def error_histogram(self, hist_path: Path, image: Image, *args, **kwargs):
        pass  # TODO ?

    def end_successful_import(self, path: Path, image: Image, *args, **kwargs):
        uf = self.get_uf(path)

        ai = AbstractImage()
        ai.uploadedFile = uf.id
        ai.originalFilename = uf.originalFilename
        ai.width = image.width
        ai.height = image.height
        ai.depth = image.depth
        ai.duration = image.duration

        # Cytomine "channels" = number of concrete channels
        ai.channels = image.n_concrete_channels
        ai.samplePerPixel = image.n_samples
        ai.bitPerSample = dtype_to_bits(image.pixel_type)

        if image.physical_size_x:
            ai.physicalSizeX = round(
                convert_quantity(image.physical_size_x, "micrometers"), 6
            )
        if image.physical_size_y:
            ai.physicalSizeY = round(
                convert_quantity(image.physical_size_y, "micrometers"), 6
            )
        if image.physical_size_z:
            ai.physicalSizeZ = round(
                convert_quantity(image.physical_size_z, "micrometers"), 6
            )
        if image.frame_rate:
            ai.fps = round(
                convert_quantity(image.frame_rate, "Hz"), 6
            )
        ai.magnification = parse_int(image.objective.nominal_magnification)

        ai.save()
        self.abstract_images.append(ai)

        asc = AbstractSliceCollection()
        for cc in range(image.n_concrete_channels):
            first_c = cc * image.n_samples

            name = image.channels[first_c].suggested_name
            color = image.channels[first_c].hex_color
            if image.n_samples != 1:
                names = [
                    image.channels[i].suggested_name
                    for i in range(first_c, first_c + image.n_samples)
                    if image.channels[i].suggested_name is not None
                ]
                names = list(dict.fromkeys(names))  # ordered uniqueness
                name = '|'.join(names)
                color = None

            for z in range(image.depth):
                for t in range(image.duration):
                    mime = "image/pyrtiff"  # TODO: remove
                    asc.append(
                        AbstractSlice(
                            ai.id, uf.id, mime, cc, z, t,
                            channelName=name, channelColor=color
                        )
                    )
        asc.save()

        uf.status = UploadedFile.DEPLOYED
        uf.update()

        properties = PropertyCollection(ai)
        for k, v in self.user_properties:
            if v is not None and str(v) != '':
                properties.append(Property(ai, k, v))
        try:
            properties.save()
        except CollectionPartialUploadException:
            pass  # TODO: improve handling of this exception, but prevent to fail the import

        instances = []
        for p in self.projects:
            instances.append(ImageInstance(ai.id, p.id).save())
        self.images.append((ai, instances))

        # TODO: temporary add annotations for backwards compatibility.
        #  BUT it should be done by core when an image instance is created.
        if image.n_planes == 1 and len(instances) > 0:
            # TODO: currently only supports metadata annots on 2D images

            metadata_annots = image.annotations
            if len(metadata_annots) > 0:
                metadata_terms = [ma.terms for ma in metadata_annots if len(ma.terms) > 0]
                metadata_terms = set(flatten(metadata_terms))

                for instance in instances:
                    project_id = instance.project
                    project = self.projects.find_by_attribute('id', project_id)
                    ontology_id = project.ontology  # noqa
                    ontology_terms = TermCollection().fetch_with_filter("project", project_id)
                    terms_id_mapping = {t.name: t.id for t in ontology_terms}

                    for metadata_term in metadata_terms:
                        if metadata_term not in terms_id_mapping:
                            # TODO: user must have ontology rights !
                            term = Term(
                                name=metadata_term, id_ontology=ontology_id,
                                color="#AAAAAA"
                            ).save()
                            terms_id_mapping[term.name] = term.id

                    annots = AnnotationCollection()
                    for metadata_annot in metadata_annots:
                        term_ids = [terms_id_mapping[t] for t in metadata_annot.terms]
                        properties = [
                            dict(key=k, value=v)
                            for k, v in metadata_annot.properties.items()
                        ]
                        annots.append(Annotation(
                            location=metadata_annot.wkt,
                            id_image=instance.id,
                            id_terms=term_ids if len(term_ids) > 0 else None,
                            properties=properties if len(properties) > 0 else None,
                            user=uf.user
                        ))

                    try:
                        annots.save()
                    except CollectionPartialUploadException:
                        pass
                        # TODO: improve handling of this exception,
                        #  but prevent to fail the import

    def file_not_moved(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = self._corresponding_error_status(uf.status)
        uf.update()
        self.propagate_error(uf)

    def file_not_found(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        uf.status = self._corresponding_error_status(uf.status)
        uf.update()
        self.propagate_error(uf)

    def generic_file_error(self, path: Path, *args, **kwargs):
        uf = self.get_uf(path)
        if uf.status % 2 == 0:
            # Only update error status if the status is not yet an error
            # (probably more detailed)
            uf.status = UploadedFile.ERROR_DEPLOYMENT
            uf.update()
        self.propagate_error(uf)


class StdoutListener(ImportListener):
    def __init__(self, name: str):
        self.log = logging.getLogger(f"upload.{name}")

    def start_data_extraction(self, path: Path, *args, **kwargs):
        self.log.info(f"Start import and data extraction for {path}")

    def moved_pending_file(self, old_path: Path, new_path: Path, *args, **kwargs):
        self.log.info(f"Moved {old_path} to {new_path}")

    def end_data_extraction(self, path: Path, *args, **kwargs):
        self.log.info(f"Finished to extract data for {path}")

    def start_format_detection(self, path: Path, *args, **kwargs):
        self.log.info(f"Start format detection for {path}")

    def end_format_detection(self, path: Path, format: AbstractFormat, *args, **kwargs):
        self.log.info(f"Identified format {format.get_name()} for {path}")

    def error_no_format(self, path: Path, *args, **kwargs):
        self.log.warning(f"No matching format for {path}")

    def start_unpacking(self, path: Path, *args, **kwargs):
        self.log.info(f"Start unpacking archive {path}")

    def end_unpacking(
        self, path: Path, unpacked_path: Path, *args,
        format: AbstractFormat = None, is_collection: bool = False, **kwargs
    ):
        self.log.info(
            f"The archive {path} is unpacked in directory "
            f"{unpacked_path}."
        )
        if is_collection:
            self.log.info(f"{path} is a collection.")
        else:
            self.log.info(
                f"Identified format {format.get_name()} "
                f"for {unpacked_path} "
            )

    def error_unpacking(self, path: Path, *args, **kwargs):
        self.log.error(
            f"Error while unpacking archive {path} "
            f"({str(kwargs.get('exception', ''))})"
        )

    def register_file(self, path: Path, parent_path: Path, *args, **kwargs):
        self.log.info(f"Found {path} in {parent_path}")

    def start_integrity_check(self, path: Path, *args, **kwargs):
        self.log.info(f"Start integrity check for {path}")

    def end_integrity_check(self, path: Path, *args, **kwargs):
        self.log.info(f"{path} passed integrity check")

    def error_integrity(self, path: Path, *args, **kwargs):
        self.log.error(f"Integrity error for {path}.")
        for integrity_error in kwargs.get('integrity_errors', []):
            attr, e = integrity_error
            self.log.error(f"- {attr}: {e}")

    def start_conversion(self, path: Path, parent_path: Path, *args, **kwargs):
        self.log.info(f"Start converting {parent_path} to {path}")

    def end_conversion(self, path: Path, *args, **kwargs):
        self.log.info(f"Finished {path} conversion !")

    def error_conversion(self, path: Path, *args, **kwargs):
        self.log.error(f"Error while converting {path}", exc_info=True)

    def start_spatial_deploy(self, path: Path, *args, **kwargs):
        self.log.info(f"--- SPATIAL representation deployment for {path} ---")

    def end_spatial_deploy(self, spatial_path: Path, *args, **kwargs):
        self.log.info(
            f"Finished to deploy spatial representation "
            f"at {spatial_path}"
        )

    def start_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        self.log.info(f"--- HISTOGRAM representation deployment for {image} ---")

    def end_histogram_deploy(self, hist_path: Path, image: Image, *args, **kwargs):
        self.log.info(
            f"Finished to deploy histogram representation "
            f"at {hist_path}"
        )

    def error_histogram(self, hist_path: Path, image: Image, *args, **kwargs):
        self.log.error(
            f"Failed to build histogram at {hist_path} "
            f"for image {image} "
            f"({kwargs.get('exception')}", exc_info=True
        )

    def end_successful_import(self, path: Path, image: Image, *args, **kwargs):
        self.log.info(f"{path} imported !")

    def file_not_found(self, path: Path, *args, **kwargs):
        self.log.error(f"File {path} is not found", exc_info=True)

    def file_not_moved(self, path: Path, *args, **kwargs):
        self.log.error(f"Failed to move {path}", exc_info=True)

    def generic_file_error(self, path: Path, *args, **kwargs):
        self.log.error(f"Generic file error for {path}", exc_info=True)

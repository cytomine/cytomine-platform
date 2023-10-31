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

import os
import shutil
from datetime import datetime
from enum import Enum
from pathlib import Path as _Path
from typing import Callable, List, TYPE_CHECKING, Union

from pims.cache import IMAGE_CACHE
from pims.formats.utils.factories import (
    FormatFactory, SpatialReadableFormatFactory,
    SpectralReadableFormatFactory
)
from pims.utils.copy import SafelyCopiable
from pims.api.exceptions import check_representation_existence

if TYPE_CHECKING:
    from pims.files.image import Image
    from pims.files.histogram import Histogram

PROCESSED_DIR = "processed"
EXTRACTED_DIR = "extracted"

UPLOAD_DIR_PREFIX = "upload"
EXTRACTED_FILE_DIR_PREFIX = "file"

ORIGINAL_STEM = "original"
SPATIAL_STEM = "visualisation"
SPECTRAL_STEM = "spectral"
HISTOGRAM_STEM = "histogram"

_NUM_SIGNATURE_BYTES = 262


class FileRole(str, Enum):
    """
    The role of a file. The same image data can be represented in different ways, in different
    files, each of them serving different purposes.

    * `UPLOAD` - This file is the one such as received by PIMS.
    * `ORIGINAL` - This file is in its original format and contains (part of) metadata.
    * `SPATIAL` - This file is used to retrieve regular 2D spatial regions from the image.
    * `SPECTRAL` - This file is used to retrieve spectral data from the image.
    * `NONE` - This file has no defined role for PIMS.
    """

    UPLOAD = 'UPLOAD'
    ORIGINAL = 'ORIGINAL'
    SPATIAL = 'SPATIAL'
    SPECTRAL = 'SPECTRAL'
    NONE = 'NONE'

    @classmethod
    def from_path(cls, path: Path) -> FileRole:
        role = cls.NONE
        if path.has_original_role():
            role = cls.ORIGINAL
        if path.has_spatial_role():
            role = cls.SPATIAL
        if path.has_spectral_role():
            role = cls.SPECTRAL
        if path.has_upload_role():
            role = cls.UPLOAD
        return role


class FileType(str, Enum):
    """
    The type of the file.
    * `SINGLE` - The file only has one image.
    * `COLLECTION` - The file is a container and contains multiple images that need further
    processing.
    """

    SINGLE = 'SINGLE'
    COLLECTION = 'COLLECTION'

    @classmethod
    def from_path(cls, path: Path) -> FileType:
        if path.is_collection():
            return cls.COLLECTION
        return cls.SINGLE


PlatformPath = type(_Path())


class Path(PlatformPath, _Path, SafelyCopiable):
    f"""
    Extends `Path` from `pathlib` for PIMS.
    
    Work with any path (file/dir/symlink) having a (parent) directory `UPLOAD_DIR`
    starting with `UPLOAD_DIR_PREFIX` in its path. For any path respecting this 
    constraint, all related file representations can be retrieved.
    
    The expected structure is:
    
    1. For a `SINGLE` file type
    /anypath
    |_ /{UPLOAD_DIR_PREFIX}xxx    (directory)
       |_ /my-file-name.abc       (file, symlink or directory)
       |_ /{PROCESSED_DIR}        (directory)
          |_ /{ORIGINAL_STEM}.xyz (file, symlink or directory)
          |_ /{SPATIAL_STEM}.xyz  (file, symlink or directory)
          |_ /{SPECTRAL_STEM}.xyz (file, symlink or directory)
          
    2. For a `COLLECTION` file type
    /anypath
    |_ /{UPLOAD_DIR_PREFIX}xxx    (directory)
       |_ /my-file-name.abc       (file, symlink or directory)
       |_ /{PROCESSED_DIR}        (directory)
          |_ /{ORIGINAL_STEM}.xyz (file, symlink or directory)
          |_ /{EXTRACTED_DIR}     (directory or symlink)
    """

    def __init__(self, *pathsegments):
        self._pathsegments = pathsegments
        super().__init__()

    def _copy__new(self):
        cls = self.__class__
        # https://github.com/python/cpython/blob/main/Lib/pathlib.py#L478
        return cls.__new__(cls, *tuple(self._parts))  # noqa

    @property
    def creation_datetime(self) -> datetime:
        return datetime.fromtimestamp(self.stat().st_ctime)

    @property
    def size(self) -> int:
        """Get file size, in bytes"""
        if self.is_dir():
            return sum([it.size for it in self.iterdir() if os.access(it, os.R_OK)])
        if not self.is_file() and not self.is_dir():
            return 0
        return self.stat().st_size

    @property
    def extension(self) -> str:
        """
        Path extension.

        Examples
        --------
        >>> Path("/a/b/c.ext1.ext2").extension
        ".ext1.ext2"
        """
        return ''.join(self.suffixes)

    @property
    def true_stem(self) -> str:
        """
        Stem is filename without extension (see `pathlib`)
        True stem is filename without all extensions.

        Examples
        --------
        >>> Path("/a/b/c.ext1.ext2").true_stem
        "c"
        """
        return self.stem.split('.')[0]

    def mount_point(self) -> Union[Path, None]:
        for parent in self.parents:
            if parent.is_mount():
                return parent
        return None

    def mount_disk_usage(self):
        return shutil.disk_usage(self)

    def recursive_iterdir(self):
        for p in self.rglob("**/*"):
            yield p

    def is_processed(self) -> bool:
        """
        Whether the path is in a processed directory
        (i.e. the file can be identified by PIMS)
        """
        return PROCESSED_DIR in self.parts

    def is_extracted(self) -> bool:
        """
        Whether the path is in an extracted directory
        (i.e. the file has been extracted from an archive by PIMS)
        """
        return EXTRACTED_DIR in self.parts

    def has_upload_role(self) -> bool:
        """
        Whether the path is an upload (and thus the file is not processed)
        """
        return not self.is_processed() and self.parent.samefile(self.upload_root())

    def has_original_role(self) -> bool:
        """
        Whether the path has the original role (and thus processed)
        """
        return self.is_processed() and self.true_stem == ORIGINAL_STEM

    def has_spatial_role(self) -> bool:
        """
        Whether the path has the spatial role (and thus processed)
        """
        return self.is_processed() and self.true_stem == SPATIAL_STEM

    def has_spectral_role(self) -> bool:
        """
        Whether the path has the spectral role (and thus processed)
        """
        return self.is_processed() and self.true_stem == SPECTRAL_STEM

    def has_histogram_role(self) -> bool:
        """
        Whether the path has the histogram role (and thus processed)
        """
        return self.is_processed() and self.true_stem == HISTOGRAM_STEM

    def upload_root(self) -> Path:
        for parent in self.parents:
            if parent.name.startswith(UPLOAD_DIR_PREFIX):
                return Path(parent)
        raise FileNotFoundError(f"No upload root for {self}")

    def delete_upload_root(self) -> None:
        """
        Delete the all the representations of an image, including the related upload folder.
        """

        upload_root = self.get_upload().resolve().upload_root()
        shutil.rmtree(upload_root)
        return None

    def processed_root(self) -> Path:
        processed = self.upload_root() / Path(PROCESSED_DIR)
        return processed

    def extracted_root(self) -> Path:
        extracted = self.processed_root() / Path(EXTRACTED_DIR)
        return extracted

    def get_upload(self) -> Path:
        upload = next(
            (child for child in self.upload_root().iterdir() if child.has_upload_role()), None
        )
        return upload

    def get_original(self) -> Union[Image, None]:
        if not self.processed_root().exists():
            return None

        original = next(
            (child for child in self.processed_root().iterdir() if child.has_original_role()), None
        )

        from pims.files.image import Image
        return Image(original, factory=FormatFactory(match_on_ext=True)) if original else None

    def get_spatial(self, cache=False) -> Union[Image, None]:
        processed_root = self.processed_root()
        if not processed_root.exists():
            return None

        cache_key = str(processed_root / Path(SPATIAL_STEM))
        cached = IMAGE_CACHE.get(cache_key)
        if cached is not None:
            return cached

        spatial = next(
            (child for child in self.processed_root().iterdir() if child.has_spatial_role()), None
        )
        if not spatial:
            return None
        else:
            from pims.files.image import Image
            image = Image(
                spatial, factory=SpatialReadableFormatFactory(match_on_ext=True)
            )
            if cache:
                IMAGE_CACHE.put(cache_key, image)
            return image

    def get_spectral(self) -> Union[Image, None]:
        if not self.processed_root().exists():
            return None

        spectral = next(
            (child for child in self.processed_root().iterdir() if child.has_spectral_role()), None
        )

        from pims.files.image import Image
        return Image(
            spectral, factory=SpectralReadableFormatFactory(match_on_ext=True)
        ) if spectral else None

    def get_histogram(self) -> Union[Histogram, None]:
        if not self.processed_root().exists():
            return None

        histogram = next(
            (child for child in self.processed_root().iterdir() if child.has_histogram_role()),
            None
        )

        from pims.files.histogram import Histogram
        return Histogram(histogram) if histogram else None

    def get_representations(self) -> List[Path]:
        representations = [
            self.get_upload(), self.get_original(), self.get_spatial(),
            self.get_spectral()
        ]
        return [representation for representation in representations if representation is not None]

    def get_representation(self, role: FileRole) -> Union[Path, None]:
        if role == FileRole.UPLOAD:
            return self.get_upload()
        elif role == FileRole.ORIGINAL:
            return self.get_original()
        elif role == FileRole.SPATIAL:
            return self.get_spatial()
        elif role == FileRole.SPECTRAL:
            return self.get_spectral()
        else:
            return None

    def get_extracted_children(self, stop_recursion_cond: Callable = None):
        if not self.is_collection():
            return []

        def _iterdir(directory):
            for p in directory.glob("*"):
                if p.is_dir():
                    if stop_recursion_cond is not None and stop_recursion_cond(p):
                        yield p
                    else:
                        yield from _iterdir(p)
                else:
                    yield p

        return _iterdir(self.extracted_root())

    def is_collection(self) -> bool:
        if not self.processed_root().exists():
            return False

        # is there a "extracted" directory in upload root children ?
        if not self.is_extracted():
            for child in self.processed_root().iterdir():
                if child.is_extracted():
                    return True
        return False

    def is_single(self) -> bool:
        return not self.is_collection()

    def signature(self) -> bytearray:
        """
        Get file signature (aka magic bytes), enough to identify
        all image formats.
        """
        if not self.is_file():
            return bytearray()
        with self.resolve().open('rb') as fp:
            return bytearray(fp.read(_NUM_SIGNATURE_BYTES))

    @property
    def path(self) -> Path:
        """
        Helps a regular Path to be Pathlike compatible (as expected by format
        checkers). Needed to have same interface as `CachedDataPath`.
        See `CachedDataPath` for technical details.
        """
        return self


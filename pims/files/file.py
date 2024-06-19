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
from typing import Callable, List, TYPE_CHECKING, Union, Optional, Tuple

import numpy as np
from pint import Quantity

from pims.api.exceptions import NoMatchingFormatProblem
from pims.api.utils.models import HistogramType
from pims.cache import cached_property

from pims.cache.redis import PIMSCache, PickleCodec, CACHE_KEY_PREFIX_IMAGE_FORMAT_METADATA, stable_hash
from pims.config import get_settings
from pims.formats import AbstractFormat
from pims.formats.utils.factories import (
    FormatFactory, SpatialReadableFormatFactory,
    SpectralReadableFormatFactory
)
from pims.formats.utils.structures.annotations import ParsedMetadataAnnotation
from pims.formats.utils.structures.metadata import ImageChannel, ImageObjective, ImageMicroscope, ImageAssociated, \
    MetadataStore
from pims.formats.utils.structures.pyramid import Pyramid
from pims.processing.adapters import RawImagePixels
from pims.processing.region import Tile, Region
from pims.utils.copy import SafelyCopiable

if TYPE_CHECKING:
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

    async def _get_cached_representation(self, representation: FileRole) -> Union[Image, None]:
        if representation not in (FileRole.ORIGINAL, FileRole.SPATIAL):
            raise ValueError(f"Cached representation {representation} is not supported.")

        if not PIMSCache.is_enabled() and get_settings().cache_image_format_metadata:
            return self.get_representation(representation)

        processed_root = self.processed_root()
        if not processed_root.exists():
            return None

        stem = ORIGINAL_STEM if representation == FileRole.ORIGINAL else SPATIAL_STEM
        stem_path = str(processed_root / Path(stem))
        cache_key = stable_hash(stem_path.encode())
        cached = await PIMSCache.get_backend().get(cache_key, namespace=CACHE_KEY_PREFIX_IMAGE_FORMAT_METADATA)
        if cached is not None:
            decoded = PickleCodec.decode(cached)
            return Image(f"{stem_path}.{decoded.get_identifier()}", format=decoded)

        image = self.get_representation(representation)
        await PIMSCache.get_backend().set(
            cache_key, PickleCodec.encode(image.format.serialize()), namespace=CACHE_KEY_PREFIX_IMAGE_FORMAT_METADATA
        )
        return image

    def get_original(self) -> Union[Image, None]:
        if not self.processed_root().exists():
            return None

        original = next(
            (child for child in self.processed_root().iterdir() if child.has_original_role()), None
        )

        return Image(original, factory=FormatFactory(match_on_ext=True)) if original else None

    async def get_cached_original(self) -> Union[Image, None]:
        return await self._get_cached_representation(FileRole.ORIGINAL)

    def get_spatial(self) -> Union[Image, None]:
        processed_root = self.processed_root()
        if not processed_root.exists():
            return None

        spatial = next(
            (child for child in self.processed_root().iterdir() if child.has_spatial_role()), None
        )
        if not spatial:
            return None
        else:
            image = Image(
                spatial, factory=SpatialReadableFormatFactory(match_on_ext=True)
            )
            return image

    async def get_cached_spatial(self) -> Union[Image, None]:
        return await self._get_cached_representation(FileRole.SPATIAL)

    def get_spectral(self) -> Union[Image, None]:
        if not self.processed_root().exists():
            return None

        spectral = next(
            (child for child in self.processed_root().iterdir() if child.has_spectral_role()), None
        )

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

    def get_representation(self, role: FileRole) -> Union[Path, Image, None]:
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


class Image(Path):
    """
    An image. Acts as a facade in front of underlying technical details
    about specific image formats.
    """
    def __init__(
        self, *pathsegments,
        factory: FormatFactory = None, format: AbstractFormat = None
    ):
        super().__init__(*pathsegments)

        _format = factory.match(Path(self)) if factory else format
        if _format is None:
            raise NoMatchingFormatProblem(Path(self))
        else:
            if _format.path.absolute() != self.absolute():
                # Paths mismatch: reload format
                _format = _format.from_path(Path(self))
            self._format = _format

    @property
    def format(self) -> AbstractFormat:
        return self._format

    @property
    def media_type(self) -> str:
        return self._format.media_type

    @property
    def width(self) -> int:
        return self._format.main_imd.width

    @property
    def physical_size_x(self) -> Optional[Quantity]:
        return self._format.full_imd.physical_size_x

    @property
    def height(self) -> int:
        return self._format.main_imd.height

    @property
    def physical_size_y(self) -> Optional[Quantity]:
        return self._format.full_imd.physical_size_y

    @property
    def n_pixels(self) -> int:
        return self.width * self.height

    @property
    def depth(self) -> int:
        return self._format.main_imd.depth

    @property
    def physical_size_z(self) -> Optional[Quantity]:
        return self._format.full_imd.physical_size_z

    @property
    def duration(self) -> int:
        return self._format.main_imd.duration

    @property
    def frame_rate(self) -> Optional[Quantity]:
        return self._format.main_imd.frame_rate

    @property
    def n_channels(self) -> int:
        return self._format.main_imd.n_channels

    @property
    def n_concrete_channels(self) -> int:
        return self._format.main_imd.n_concrete_channels

    @property
    def n_distinct_channels(self) -> int:
        return self._format.main_imd.n_distinct_channels

    @property
    def n_samples(self) -> int:
        return self._format.main_imd.n_samples

    @property
    def n_planes(self) -> int:
        return self._format.main_imd.n_planes

    @property
    def pixel_type(self) -> np.dtype:
        return self._format.main_imd.pixel_type

    @property
    def significant_bits(self) -> int:
        return self._format.main_imd.significant_bits

    @property
    def max_value(self) -> int:
        return 2 ** self.significant_bits - 1

    @property
    def value_range(self) -> range:
        return range(0, self.max_value + 1)

    @property
    def acquisition_datetime(self) -> datetime:
        return self._format.full_imd.acquisition_datetime

    @property
    def description(self) -> str:
        return self._format.full_imd.description

    @property
    def channels(self) -> List[ImageChannel]:
        return self._format.main_imd.channels

    @property
    def objective(self) -> ImageObjective:
        return self._format.full_imd.objective

    @property
    def microscope(self) -> ImageMicroscope:
        return self._format.full_imd.microscope

    @property
    def associated_thumb(self) -> ImageAssociated:
        return self._format.full_imd.associated_thumb

    @property
    def associated_label(self) -> ImageAssociated:
        return self._format.full_imd.associated_label

    @property
    def associated_macro(self) -> ImageAssociated:
        return self._format.full_imd.associated_macro

    @property
    def raw_metadata(self) -> MetadataStore:
        return self._format.raw_metadata

    @property
    def annotations(self) -> List[ParsedMetadataAnnotation]:
        return self._format.annotations

    @property
    def pyramid(self) -> Pyramid:
        return self._format.pyramid

    @property
    def normalized_pyramid(self) -> Pyramid:
        return self._format.normalized_pyramid

    @property
    def is_pyramid_normalized(self) -> bool:
        return self._format.is_pyramid_normalized

    @cached_property
    def histogram(self) -> Histogram:
        histogram = self.get_histogram()
        if histogram:
            return histogram
        else:
            return self._format.histogram

    def histogram_type(self) -> HistogramType:
        return self.histogram.type()

    def image_bounds(self):
        return self.histogram.image_bounds()

    def image_histogram(self):
        return self.histogram.image_histogram()

    def channels_bounds(self):
        return self.histogram.channels_bounds()

    def channel_bounds(self, c):
        return self.histogram.channel_bounds(c)

    def channel_histogram(self, c):
        return self.histogram.channel_histogram(c)

    def planes_bounds(self):
        return self.histogram.planes_bounds()

    def plane_bounds(self, c, z, t):
        return self.histogram.plane_bounds(c, z, t)

    def plane_histogram(self, c, z, t):
        return self.histogram.plane_histogram(c, z, t)

    def tile(
        self, tile: Tile, c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None,
        t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get a tile.

        Returned channels are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return only the asked channels.

        Parameters
        ----------
        tile
            A 2D region at a given downsample (linked to a pyramid tier)
        c
            The asked channel index(es).
            If not set, all channels are considered.
        z
            The asked z-slice index. Image formats without Z-stack support
            can safely ignore this parameter. Behavior is undetermined if `z`
            is not set for an image format with Z-stack support.
        t
            The asked timepoint index. Image formats without time support
            can safely ignore this parameter. Behavior is undetermined if `t`
            is not set for an image format with time support.

        Returns
        -------
        RawImagePixels
        """
        try:
            return self._format.reader.read_tile(tile, c=c, z=z, t=t)
        except NotImplementedError as e:
            # Implement tile extraction from window ?
            raise e

    def window(
        self, region: Region, out_width: int, out_height: int,
        c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None,
        t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get an image window whose output dimensions are the nearest possible to
        asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a window at the asked output dimensions. In all cases:
        * `true_out_width >= out_width`
        * `true_out_height >= out_height`

        Returned channels are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return only the asked channels.

        Parameters
        ----------
        region
            A 2D region at a given downsample
        out_width
            The asked output width (best-effort)
        out_height
            The asked output height (best-effort)
        c
            The asked channel index(es).
            If not set, all channels are considered.
        z
            The asked z-slice index. Image formats without Z-stack support
            can safely ignore this parameter. Behavior is undetermined if `z`
            is not set for an image format with Z-stack support.
        t
            The asked timepoint index. Image formats without time support
            can safely ignore this parameter. Behavior is undetermined if `t`
            is not set for an image format with time support.

        Returns
        -------
        RawImagePixels
        """
        try:
            return self._format.reader.read_window(
                region, out_width, out_height, c=c, z=z, t=t
            )
        except NotImplementedError as e:
            # Implement window extraction from tiles ?
            raise e

    def thumbnail(
        self, out_width: int, out_height: int, precomputed: bool = False,
        c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None, t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get an image thumbnail whose dimensions are the nearest possible to
        asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return a thumbnail at the asked output
        dimensions. In all cases:
        * `true_out_width >= out_width`
        * `true_out_height >= out_height`

        Returned channels are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return only the asked channels.

        Parameters
        ----------
        out_width
            The asked output width (best-effort)
        out_height
            The asked output height (best-effort)
        precomputed
            Whether use precomputed thumbnail stored in the file if available.
        c
            The asked channel index(es).
            If not set, all channels are considered.
        z
            The asked z-slice index. Image formats without Z-stack support
            can safely ignore this parameter. Behavior is undetermined if `z`
            is not set for an image format with Z-stack support.
        t
            The asked timepoint index. Image formats without time support
            can safely ignore this parameter. Behavior is undetermined if `t`
            is not set for an image format with time support.

        Returns
        -------
        RawImagePixels
        """
        try:
            return self._format.reader.read_thumb(
                out_width, out_height, precomputed=precomputed, c=c, z=z, t=t
            )
        except NotImplementedError as e:
            # Get thumbnail from window ?
            raise e

    def label(self, out_width: int, out_height: int) -> Optional[RawImagePixels]:
        """
        Get a precomputed image label whose output dimensions are the nearest
        possible to asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a label at the asked output dimensions. In all cases:
        * `true_out_width >= out_width`
        * `true_out_height >= out_height`

        Parameters
        ----------
        out_width
            The asked output width (best-effort)
        out_height
            The asked output height (best-effort)

        Returns
        -------
        RawImagePixels
        """
        if not self.associated_label.exists:
            return None
        try:
            return self._format.reader.read_label(out_width, out_height)
        except NotImplementedError:
            return None

    def macro(self, out_width: int, out_height: int) -> Optional[RawImagePixels]:
        """
        Get a precomputed image macro whose output dimensions are the nearest
        possible to asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a macro at the asked output dimensions. In all cases:
        * `true_out_width >= out_width`
        * `true_out_height >= out_height`

        Parameters
        ----------
        out_width
            The asked output width (best-effort)
        out_height
            The asked output height (best-effort)

        Returns
        -------
        RawImagePixels
        """
        if not self.associated_macro.exists:
            return None
        try:
            return self._format.reader.read_macro(out_width, out_height)
        except NotImplementedError:
            return None

    def check_integrity(
        self, lazy_mode: bool = False, check_metadata: bool = True,
        check_tile: bool = False, check_thumb: bool = False,
        check_window: bool = False, check_associated: bool = False
    ) -> List[Tuple[str, Exception]]:
        """
        Check integrity of the image: ensure that asked checks do not raise
        errors. In lazy mode, stop at first error.

        Returns
        -------
        errors
            A list of problematic attributes with the associated exception.
            Some attributes are inter-dependent, so the same exception can
            appear for several attributes.
        """
        errors = []

        if check_metadata:
            attributes = (
                'width', 'height', 'depth', 'duration', 'n_channels',
                'pixel_type', 'physical_size_x', 'physical_size_y',
                'physical_size_z', 'frame_rate', 'description',
                'acquisition_datetime', 'channels', 'objective', 'microscope',
                'associated_thumb', 'associated_label', 'associated_macro',
                'raw_metadata', 'annotations', 'pyramid'
            )
            for attr in attributes:
                try:
                    getattr(self, attr)
                except Exception as e:
                    errors.append((attr, e))
                    if lazy_mode:
                        return errors

        if check_tile:
            try:
                tier_idx = self.pyramid.max_zoom // 2
                tier = self.pyramid.tiers[tier_idx]
                tx = tier.max_tx // 2
                ty = tier.max_ty // 2
                self.tile(Tile(tier, tx, ty))
            except Exception as e:
                errors.append(('tile', e))
                if lazy_mode:
                    return errors

        if check_thumb:
            try:
                self.thumbnail(128, 128)
            except Exception as e:
                errors.append(('thumbnail', e))
                if lazy_mode:
                    return errors

        if check_window:
            try:
                w = round(0.1 * self.width)
                h = round(0.1 * self.height)
                self.window(
                    Region(self.height - h, self.width - w, w, h), 128, 128
                )
            except Exception as e:
                errors.append(('window', e))
                if lazy_mode:
                    return errors

        if check_associated:
            try:
                self.thumbnail(128, 128, precomputed=True)
            except Exception as e:
                errors.append(('precomputed_thumbnail', e))
                if lazy_mode:
                    return errors

            try:
                self.label(128, 128)
            except Exception as e:
                errors.append(('label', e))
                if lazy_mode:
                    return errors

            try:
                self.macro(128, 128)
            except Exception as e:
                errors.append(('macro', e))
                if lazy_mode:
                    return errors

        return errors

    def __exit__(self, t, v, tb):
        super().__exit__(t, v, tb)
        self.close()


    def close(self):
        if hasattr(self, '_format') and self._format is not None:
            self._format.close()
            self._format._path = None
            del self._format

    def __del__(self):
        self.close()

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

from typing import List, Optional, TYPE_CHECKING, Tuple, Union

import numpy as np
from pint import Quantity

from pims.api.exceptions import NoMatchingFormatProblem
from pims.cache import cached_property
from pims.files.file import Path
from pims.formats.utils.factories import FormatFactory
from pims.processing.adapters import RawImagePixels
from pims.processing.region import Region, Tile

if TYPE_CHECKING:
    from datetime import datetime

    from pims.formats import AbstractFormat
    from pims.api.utils.models import HistogramType
    from pims.files.histogram import Histogram
    from pims.formats.utils.structures.annotations import ParsedMetadataAnnotation
    from pims.formats.utils.structures.metadata import (
        ImageAssociated, ImageChannel, ImageMicroscope,
        ImageObjective, MetadataStore
    )
    from pims.formats.utils.structures.pyramid import Pyramid


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

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

from abc import ABC, abstractmethod
from typing import List, Optional, TYPE_CHECKING, Tuple, Union

import numpy as np

from pims.processing.adapters import RawImagePixels
from pims.processing.region import Region, Tile
from pims.utils.iterables import ensure_list

if TYPE_CHECKING:
    from pims.formats import AbstractFormat


class AbstractReader(ABC):
    """
    Base reader. All format readers must extend this class.
    """
    def __init__(self, format: AbstractFormat):
        self.format = format

    @abstractmethod
    def read_thumb(
        self, out_width: int, out_height: int, precomputed: bool = None,
        c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None, t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get an image thumbnail whose dimensions are the nearest possible to
        asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return a thumbnail at the asked output
        dimensions. The implementation SHOULD try to return the nearest possible
        thumbnail using format capabilities (such as shrink on load features)
        but MUST NOT perform any resize operation after read (in that case, an
        optimized resize operator is used in post-processing). In all cases:
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
            Retrieving precomputed thumbnail should be faster than computing
            the thumbnail from scratch (for multi-giga pixels images), but there
            is no guarantee the precomputed thumb has the same quality.
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
        raise NotImplementedError()

    @abstractmethod
    def read_window(
        self, region: Region, out_width: int, out_height: int,
        c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None, t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get an image window whose output dimensions are the nearest possible to
        asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a window at the asked output dimensions. The implementation
        SHOULD try to return the nearest possible window using format
        capabilities (such as shrink on load features) but MUST NOT perform any
        resize operation after read (in that case, an optimized resize operator
        is used in post-processing). In all cases:
        * `true_out_width >= out_width`
        * `true_out_height >= out_height`

        The implementation is responsible to find the most appropriate pyramid
        tier to get the given region at asked output dimensions.

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
        raise NotImplementedError()

    @abstractmethod
    def read_tile(
        self, tile: Tile,
        c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None, t: Optional[int] = None
    ) -> RawImagePixels:
        """
        Get an image tile. It is a particular case of `read_window` where the
        width and height of the region described by the tile at its downsample
        match the asked output dimensions. As the tile is linked to a pyramid
        tier, the tile downsample matches the downsample of a tier in the image
        pyramid.

        Output dimensions correspond to the tile width and height.

        Returned channels are best-effort, that is, depending on the format
        and the underlying library used to extract pixels from the image format,
        it may or may not be possible to return only the asked channels.

        Note: non tiled formats can fallback on `read_window`.

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
        raise NotImplementedError()

    def read_label(self, out_width: int, out_height: int) -> Optional[RawImagePixels]:
        """
        Get a precomputed image label whose output dimensions are the nearest
        possible to asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a label at the asked output dimensions. The implementation
        SHOULD try to return the nearest possible label using format
        capabilities (such as shrink on load features) but MUST NOT perform any
        resize operation after read (in that case, an optimized resize operator
        is used in post-processing). In all cases:
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
        return None

    def read_macro(self, out_width: int, out_height: int) -> Optional[RawImagePixels]:
        """
        Get a precomputed image macro whose output dimensions are the nearest
        possible to asked output dimensions.

        Output dimensions are best-effort, that is, depending on the format,
        the image pyramid characteristics, and the underlying library used to
        extract pixels from the image format, it may or may not be possible to
        return a macro at the asked output dimensions. The implementation
        SHOULD try to return the nearest possible macro using format
        capabilities (such as shrink on load features) but MUST NOT perform any
        resize operation after read (in that case, an optimized resize operator
        is used in post-processing). In all cases:
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
        return None

    def _concrete_channel_indexes(
        self, channels: Optional[Union[int, List[int]]]
    ) -> Tuple[list, list]:
        if channels is None:
            channels = np.arange(self.format.main_imd.n_channels)
        else:
            channels = np.asarray(ensure_list(channels))

        spp = self.format.main_imd.n_samples

        cc_idxs = channels // spp
        s_idxs = channels % spp
        return cc_idxs.tolist(), s_idxs.tolist()

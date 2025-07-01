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

from math import ceil
from typing import List, Tuple, Union

from pims.api.utils.models import TierIndexType
from pims.processing.region import Region, Tile
from pims.utils.iterables import split_tuple


class PyramidTier:
    """
    A tier in a pyramid.
    """

    def __init__(
        self, width: int, height: int, tile_size: Union[Tuple[int, int], int],
        pyramid: Pyramid,
        data: dict = None
    ):
        self.width = width
        self.height = height
        self.tile_width = split_tuple(tile_size, 0)
        self.tile_height = split_tuple(tile_size, 1)
        self.pyramid = pyramid
        self.data = data if type(data) is dict else dict()

    @property
    def n_pixels(self) -> int:
        return self.width * self.height

    @property
    def factor(self) -> Tuple[float, float]:
        if self.pyramid.base is None:
            return 1.0, 1.0
        else:
            return self.pyramid.base.width / self.width, \
                   self.pyramid.base.height / self.height

    @property
    def width_factor(self) -> float:
        return self.factor[0]

    @property
    def height_factor(self) -> float:
        return self.factor[1]

    @property
    def average_factor(self) -> float:
        return sum(self.factor) / 2.0

    @property
    def level(self) -> int:
        """
        Get tier level, the tier index when counting from pyramid base.
        """
        return self.pyramid.tiers.index(self)

    @property
    def zoom(self) -> int:
        """
        Get tier zoom, the tier index when counting from pyramid top.
        """
        return self.pyramid.level_to_zoom(self.level)

    @property
    def max_tx(self) -> int:
        """
        Maximum tile index along X direction in the tier.
        """
        return ceil(self.width / self.tile_width)

    @property
    def max_ty(self) -> int:
        """
        Maximum tile index along Y direction in the tier.
        """
        return ceil(self.height / self.tile_height)

    @property
    def max_ti(self) -> int:
        """
        Maximum tile index in the tier.
        """
        return self.max_tx * self.max_ty

    def ti2txty(self, ti: int) -> Tuple[int, int]:
        """
        Convert a tile index to a couple (tx, ty)
        """
        return ti % self.max_tx, ti // self.max_tx

    def txty2ti(self, tx: int, ty: int) -> int:
        """
        Convert a couple (tx, ty) to a tile index
        """
        return ty * self.max_tx + tx

    def get_ti_tile(self, ti: int) -> Tile:
        """
        Get the tile at given tile index in this tier
        """
        return self.get_txty_tile(*self.ti2txty(ti))

    def get_txty_tile(self, tx: int, ty: int) -> Tile:
        """
        Get the tile at given (tx, ty) in this tier
        """
        return Tile(self, tx, ty).clip(self.width, self.height)

    def __eq__(self, o: object) -> bool:
        return isinstance(o, PyramidTier) \
               and o.width == self.width \
               and o.height == self.height \
               and o.tile_width == self.tile_width \
               and o.tile_height == self.tile_height


class Pyramid:
    def __init__(self):
        self._tiers = []

    @property
    def n_levels(self) -> int:
        return len(self._tiers)

    @property
    def n_zooms(self) -> int:
        return len(self._tiers)

    @property
    def max_level(self) -> int:
        return self.n_levels - 1

    @property
    def max_zoom(self) -> int:
        return self.n_zooms - 1

    @property
    def tiers(self) -> List[PyramidTier]:
        return self._tiers

    @property
    def base(self) -> PyramidTier:
        """
        Get base tier (always the image at full resolution).
        """
        return self._tiers[0] if self.n_levels > 0 else None

    def zoom_to_level(self, zoom: int) -> int:
        return self.max_zoom - zoom if self.max_zoom > 0 else 0

    def level_to_zoom(self, level: int) -> int:
        return self.max_level - level if self.max_level > 0 else 0

    def insert_tier(
        self, width: int, height: int, tile_size: Union[Tuple[int, int], int],
        **tier_data
    ):
        """
        Insert a new pyramid tier in an existing pyramid.
        """
        tier = PyramidTier(
            width, height, tile_size, pyramid=self, data=tier_data
        )
        idx = 0
        while idx < len(self._tiers) \
                and tier.n_pixels < self._tiers[idx].n_pixels:
            idx += 1
        self._tiers.insert(idx, tier)

    def get_tier_at_level(self, level: int) -> PyramidTier:
        return self._tiers[level]

    def get_tier_at_zoom(self, zoom: int) -> PyramidTier:
        return self.get_tier_at_level(self.zoom_to_level(zoom))

    def get_tier_at(
        self, tier_idx: int, tier_type: TierIndexType
    ) -> PyramidTier:
        if tier_type == TierIndexType.ZOOM:
            return self.get_tier_at_zoom(tier_idx)
        else:
            return self.get_tier_at_level(tier_idx)

    def __len__(self) -> int:
        return len(self._tiers)

    def __iter__(self):
        return iter(self._tiers)

    def most_appropriate_tier_for_downsample_factor(
        self, factor: float
    ) -> PyramidTier:
        if factor < self.base.average_factor:
            return self.base

        for i in range(1, self.n_levels):
            lower_bound = self.tiers[i-1].average_factor
            upper_bound = self.tiers[i].average_factor
            if lower_bound <= factor <= upper_bound:
                if abs(factor - lower_bound) <  abs(factor - upper_bound):
                    return self.tiers[i - 1]
                else:
                    return self.tiers[i]

        return self.tiers[self.n_levels - 1]

    def most_appropriate_tier(
        self, region: Region, out_size: Tuple[int, int]
    ) -> PyramidTier:
        """
        Get the best pyramid tier to get `region` at `out_size`.

        Parameters
        ----------
        region : Region
            Requested region
        out_size : (int, int)
            Output size (width, height)

        Returns
        -------
        PyramidTier
            The most appropriate pyramid tier for this downsampling.
        """
        width_scale = region.true_width / out_size[0]
        height_scale = region.true_height / out_size[1]
        factor = (width_scale + height_scale) / 2.0
        return self.most_appropriate_tier_for_downsample_factor(factor)

    def __eq__(self, o: object) -> bool:
        return isinstance(o, Pyramid) \
               and o.n_levels == self.n_levels \
               and all([a == b for (a, b) in zip(o.tiers, self.tiers)])



def normalized_pyramid(width: int, height: int) -> Pyramid:
    """
    Build a normalized pyramid, with normalized tiles, i.e.
    * each pyramid tier is half the size of the previous one, rounded up.
    * each tile has width of 256 pixels, except for right-most tiles.
    * each tile has height of 256 pixels, except for bottom-most tiles.

    Parameters
    ----------
    width : int
        Pyramid base width
    height : int
        Pyramid base height

    Returns
    -------
    pyramid : Pyramid
        A normalized pyramid.
    """
    pyramid = Pyramid()
    w, h = width, height

    ts = 256
    pyramid.insert_tier(w, h, (ts, ts))
    while w > ts or h > ts:
        w = ceil(w / 2)
        h = ceil(h / 2)
        pyramid.insert_tier(w, h, (ts, ts))

    return pyramid

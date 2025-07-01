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

import math
from typing import TYPE_CHECKING, Tuple, Union

from pims.utils.iterables import split_tuple

if TYPE_CHECKING:
    from pims.formats.utils.structures.pyramid import PyramidTier


class Region:
    """
    A rectangular region, scaled down at a given `downsample`.

    Examples
    --------
    >>> Region(0, 0, 100, 100, 1.0) == Region(0, 0, 50, 50, 2.0)
    True
    """
    def __init__(
        self,
        top: float, left: float, width: float, height: float,
        downsample: Union[Tuple[float, float], float] = 1.0
    ):
        self.top = top
        self.left = left
        self.width = width
        self.height = height

        self.width_downsample = split_tuple(downsample, 0)
        self.height_downsample = split_tuple(downsample, 1)

    @property
    def downsample(self) -> float:
        """Average region downsample"""
        return (self.width_downsample + self.height_downsample) / 2.0

    @property
    def right(self) -> float:
        """Region right (exclusive)"""
        return self.left + self.width

    @property
    def bottom(self) -> float:
        """Region bottom (exclusive)"""
        return self.top + self.height

    @property
    def true_left(self) -> float:
        """Left position at full resolution"""
        return self.left * self.width_downsample

    @property
    def true_top(self) -> float:
        """Top position at full resolution"""
        return self.top * self.height_downsample

    @property
    def true_width(self) -> float:
        """Region width at full resolution"""
        return self.width * self.width_downsample

    @property
    def true_height(self) -> float:
        """Region height at full resolution"""
        return self.height * self.height_downsample

    def scale(self, downsample: Union[Tuple[float, float], float]) -> Region:
        """
        Scale the region in-place at a new downsample.

        Parameters
        ----------
        downsample
            The new downsample factor(s)
        Returns
        -------
        self
            The region at this new downsample factor
        """
        width_downsample = split_tuple(downsample, 0)
        height_downsample = split_tuple(downsample, 1)

        width_scale = self.width_downsample / width_downsample
        height_scale = self.height_downsample / height_downsample

        self.top *= height_scale
        self.left *= width_scale
        self.width *= width_scale
        self.height *= height_scale
        self.width_downsample = width_downsample
        self.height_downsample = height_downsample
        return self

    def discretize(self) -> Region:
        """
        Discretize (transform float coordinates to integers) the region
        in-place.

        Returns
        -------
        self
            The discretized region
        """
        self.top = math.floor(self.top)
        self.left = math.floor(self.left)
        self.width = math.ceil(self.width)
        self.height = math.ceil(self.height)
        return self

    def clip(self, width: float, height: float) -> Region:
        """
        Clip the region in-place so that it fits in a region <0,0,width,height>.
        """
        self.top = max(0, self.top)
        self.left = max(0, self.left)
        self.width = min(self.left + self.width, width) - self.left
        self.height = min(self.top + self.height, height) - self.top
        return self

    def scale_to_tier(self, tier: PyramidTier) -> Region:
        """
        Scale, discretize and clip the region in-place so that it is scaled
        for a given pyramid tier, and fits in it.
        """
        return self.scale((tier.width_factor, tier.height_factor)) \
            .discretize() \
            .clip(tier.width, tier.height)

    def as_dict(self) -> dict:
        return {
            'top': self.top,
            'left': self.left,
            'width': self.width,
            'height': self.height,
            'width_downsample': self.width_downsample,
            'height_downsample': self.height_downsample
        }

    def __eq__(self, other) -> bool:
        if isinstance(other, Region):
            scaled = other.scale(self.downsample)
            return (self.top == scaled.top and self.left == scaled.left and
                    self.width == scaled.width and self.height == scaled.height)

        return False

    def __repr__(self) -> str:
        return f"Region @ downsample ({self.width_downsample}/{self.height_downsample}) " \
               f"(Top: {self.top} / Bottom: {self.bottom} / " \
               f"Left: {self.left} / Right: {self.right} / " \
               f"Width: {self.width} / Height: {self.height}) "


class Tile(Region):
    def __init__(self, tier: PyramidTier, tx: int, ty: int):
        """
        Initialize a tile.

        Parameters
        ----------
        tier
            The pyramid tier from which the tile comes
        tx
            The tile index along the X axis
        ty
            The tile index along the Y axis
        """
        left = tx * tier.tile_width
        top = ty * tier.tile_height
        width = tier.tile_width
        height = tier.tile_height
        super().__init__(
            top, left, width, height,
            downsample=(tier.width_factor, tier.height_factor)
        )
        self.tier = tier
        self.tx = tx
        self.ty = ty

    @property
    def zoom(self) -> int:
        return self.tier.zoom

    @property
    def level(self) -> int:
        return self.tier.level

    @property
    def ti(self) -> int:
        return self.tier.txty2ti(self.tx, self.ty)

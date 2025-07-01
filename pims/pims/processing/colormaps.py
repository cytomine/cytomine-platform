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

from abc import ABC, abstractmethod
from enum import Enum
from typing import Dict, List, Optional, Union

import numpy as np
from matplotlib.cm import get_cmap, register_cmap
from matplotlib.colors import LinearSegmentedColormap as MplLinearSegmentedColormap
from pydantic.color import COLORS_BY_NAME

from pims.utils.color import Color
from pims.utils.dtypes import np_dtype

LookUpTable = np.ndarray  # Shape: (LUT size, LUT n_components)
StackedLookUpTables = np.ndarray  # Shape: (N, LUT size, LUT n_components)


class ColormapType(str, Enum):
    """
    * `SEQUENTIAL` - change in lightness and often saturation of color
    incrementally, often using a single hue should be used for representing
    information that has ordering.
    * `DIVERGING` - change in lightness and possibly saturation of two different
    colors that meet in the middle at an unsaturated color; should be used when
    the image has a critical middle value.
    * `QUALITATIVE` - often are miscellaneous colors; should be used to
    represent information which does not have ordering or relationships
    """
    PERCEPTUAL_UNIFORM = "PERCEPTUAL_UNIFORM"
    SEQUENTIAL = "SEQUENTIAL"
    DIVERGING = "DIVERGING"
    QUALITATIVE = "QUALITATIVE"
    CYCLIC = "CYCLIC"
    MISCELLANEOUS = "MISCELLANEOUS"


class Colormap(ABC):
    def __init__(self, id: str, cmap_type: ColormapType, inverted: bool = False):
        self.id = id
        self.ctype = cmap_type
        self.inverted = inverted

    @property
    def identifier(self) -> str:
        inverted = "!" if self.inverted else ""
        return inverted + self.id.upper()

    @property
    def name(self) -> str:
        inverted = " (Inverted)" if self.inverted else ""
        return self.id.replace('_', ' ').title() + inverted

    @abstractmethod
    def lut(
        self, size: int = 256, bitdepth: int = 8,
        n_components: Optional[int] = None,
        force_black_as_first: bool = False
    ) -> LookUpTable:
        """
        Build a look-up table (LUT) for the colormap.

        Parameters
        ----------
        size
            LUT size (i.e. maximum admissible pixel intensity in the image
            where the look-up table will be applied).
        bitdepth
            LUT bitdepth (i.e. expected image bitdepth after applying LUT).
        n_components
            LUT number of components. Expected to be 1 (grayscale) or 3 (rgb).
            If not set, the number of components defined for the colormap is
            used.
        force_black_as_first
            Force to return black color (0) in the first LUT item whatever
            the colormap.

        Returns
        -------
        lut
            the look-up table of shape (size, n_components)
        """
        pass

    def n_components(self) -> int:
        """
        Number of color components in the colormap.
        1 for grayscale, 3 for rgb.
        """
        return 3

    def as_image(self, width: int, height: int, bitdepth: int = 8) -> np.ndarray:
        """Get an image representation of the colormap."""
        lut = self.lut(size=width, bitdepth=bitdepth)
        return np.tile(lut, (height, 1, 1))

    def __eq__(self, o: object) -> bool:
        return isinstance(o, Colormap) and \
               o.identifier == self.identifier


class MatplotlibColormap(Colormap):
    def __init__(self, id: str, cmap_type: ColormapType, inverted: bool = False):
        super().__init__(id, cmap_type, inverted)

        self._mpl_cmap = dict()
        self._init_cmap(256)

    def _init_cmap(self, size: int):
        # (Matplotlib already precomputes with N=256)
        mpl_size = None
        if size != 256 or self.ctype == ColormapType.QUALITATIVE:
            mpl_size = size
        mpl_name = self.id + ("_r" if self.inverted else "")
        self._mpl_cmap[size] = get_cmap(mpl_name, mpl_size)
        self._mpl_cmap[size]._init()  # noqa

    def lut(
        self, size: int = 256, bitdepth: int = 8,
        n_components: Optional[int] = None,
        force_black_as_first: bool = False
    ) -> LookUpTable:
        if n_components is None or n_components > 3:
            n_components = self.n_components()

        if size not in self._mpl_cmap:
            self._init_cmap(size)

        lut = self._mpl_cmap[size]._lut[:size, :n_components].copy()  # noqa

        if force_black_as_first:
            lut[0, :] = 0

        lut *= (2 ** bitdepth - 1)
        lut = np.rint(lut)
        return lut.astype(np_dtype(bitdepth))


class ColorColormap(Colormap):
    def __init__(self, color: Color, inverted: bool = False):
        super().__init__(str(color), ColormapType.SEQUENTIAL, inverted)
        self._color = color

    @property
    def color(self) -> Color:
        return self._color

    def n_components(self) -> int:
        r, g, b = self._color.as_float_tuple(alpha=False)
        return 1 if r == g == b else 3

    def lut(
        self, size: int = 256, bitdepth: int = 8,
        n_components: Optional[int] = None,
        force_black_as_first: bool = False
    ) -> LookUpTable:
        components = self._color.as_float_tuple(alpha=False)
        if n_components is None or n_components > 3:
            n_components = self.n_components()

        colors = components[:n_components]
        lut = np.zeros((size, n_components))
        x = [0, size - 1]
        xvals = np.arange(size)
        for i, color in enumerate(colors):
            if self.inverted:
                y = [color, 0]
            else:
                y = [0, color]
            lut[:, i] = np.interp(xvals, x, y)

        if force_black_as_first:
            lut[0, :] = 0

        lut = lut * (2 ** bitdepth - 1)
        lut = np.rint(lut)
        return lut.astype(np_dtype(bitdepth))


def default_lut(
    size: int = 256, bitdepth: int = 8, n_components: int = 1,
    force_black_as_first: Optional[bool] = False  # Ignored but here for compat
) -> LookUpTable:
    """Default LUT"""
    return np.rint(np.stack(
        (np.arange(size),) * n_components, axis=-1
    )).astype(np_dtype(bitdepth))


def combine_lut(lut_a: LookUpTable, lut_b: LookUpTable) -> LookUpTable:
    """
    Combine 2 LUTs in a single LUT. Applying combined LUT from LUTs A & B on
    an image produces the same result than applying successively LUT A on an
    image, and then LUT B on the result.

    `lut_a` and `lut_b` must have same size.
    """
    return np.take_along_axis(lut_b, lut_a, axis=0)


def combine_stacked_lut(
    lut_a: StackedLookUpTables, lut_b: StackedLookUpTables
) -> StackedLookUpTables:
    """
    Combine 2 stacked LUTs in a single stacked LUT.
    Applying combined LUT from LUTs A & B on an image produces the same result
    than applying successively LUT A on an image, and then LUT B on the result.

    `lut_a` and `lut_b` must have same size and same number of elements in the
    stack.
    """
    return np.take_along_axis(lut_b, lut_a, axis=1)


def get_lut_from_stacked(
    stack: Optional[StackedLookUpTables], index: int = 0, as_stack: bool = False
) -> Union[None, LookUpTable, StackedLookUpTables]:
    """
    Get a LUT from a stack of LUTs.

    Parameters
    ----------
    stack
        The stack of LUTs. If not set, None is returned.
    index
        The index of the desired LUT in the stack
    as_stack
        Whether to return the LUT as a stack of length 1 or not.

    Returns
    -------
    None if `stack` is None or a LUT if `as_stack` is False or a LUT stack
    if `as_stack` is True.
    """
    if stack is None:
        return None
    lut = stack[index, :, :]
    if as_stack:
        lut = lut[np.newaxis, :, :]
    return lut


mpl_cmaps = dict()

mpl_cmaps[ColormapType.PERCEPTUAL_UNIFORM] = [
    'viridis', 'plasma', 'inferno', 'magma', 'cividis']
mpl_cmaps[ColormapType.SEQUENTIAL] = [
    'Greys', 'Purples', 'Blues', 'Greens', 'Oranges', 'Reds',
    'YlOrBr', 'YlOrRd', 'OrRd', 'PuRd', 'RdPu', 'BuPu',
    'GnBu', 'PuBu', 'YlGnBu', 'PuBuGn', 'BuGn', 'YlGn',
    'binary', 'gist_yarg', 'gist_gray', 'bone',
    'spring', 'summer', 'autumn', 'winter', 'cool', 'Wistia',
    'hot', 'afmhot', 'gist_heat', 'copper']
mpl_cmaps[ColormapType.DIVERGING] = [
    'PiYG', 'PRGn', 'BrBG', 'PuOr', 'RdGy', 'RdBu',
    'RdYlBu', 'RdYlGn', 'Spectral', 'coolwarm', 'bwr', 'seismic']
mpl_cmaps[ColormapType.CYCLIC] = [
    'twilight', 'twilight_shifted', 'hsv']
mpl_cmaps[ColormapType.QUALITATIVE] = [
    'Pastel1', 'Pastel2', 'Paired', 'Accent',
    'Dark2', 'Set1', 'Set2', 'Set3',
    'tab10', 'tab20', 'tab20b', 'tab20c']
mpl_cmaps[ColormapType.MISCELLANEOUS] = [
    'flag', 'prism', 'ocean', 'gist_earth', 'terrain', 'gist_stern',
    'gnuplot', 'gnuplot2', 'CMRmap', 'cubehelix', 'brg',
    'gist_rainbow', 'rainbow', 'jet', 'turbo', 'nipy_spectral',
    'gist_ncar']

# Custom colormaps
_heatmap_data = (
    (0.0, 0.0, 1.0),
    (0.0, 1.0, 1.0),
    (0.0, 1.0, 0.0),
    (1.0, 1.0, 0.0),
    (1.0, 0.0, 0.0)
)
_custom_cmaps = [
    (MplLinearSegmentedColormap.from_list("heatmap", _heatmap_data),
     ColormapType.SEQUENTIAL)
]
for custom_cmap in _custom_cmaps:
    mpl, ctype = custom_cmap
    register_cmap(None, mpl)
    register_cmap(None, mpl.reversed())
    mpl_cmaps[ctype].append(mpl.name)
ColormapsByName = Dict[str, Colormap]

# Non-trivial colormaps
COLORMAPS = {}

for ctype, cmaps in mpl_cmaps.items():
    for cmap in cmaps:
        for inv in (False, True):
            colormap = MatplotlibColormap(cmap, cmap_type=ctype, inverted=inv)
            COLORMAPS[colormap.identifier] = colormap

# Pre-load colormaps for named colors
COLOR_COLORMAPS = {}

for name in COLORS_BY_NAME:
    for inv in (False, True):
        colormap = ColorColormap(Color(name), inverted=inv)
        COLOR_COLORMAPS[colormap.identifier] = colormap

# All pre-loaded colormaps
ALL_COLORMAPS = {**COLORMAPS, **COLOR_COLORMAPS}

# Default colormaps per channel index
DEFAULT_CHANNEL_COLORMAPS = {
    0: ALL_COLORMAPS['RED'],
    1: ALL_COLORMAPS['LIME'],
    2: ALL_COLORMAPS['BLUE'],
    3: ALL_COLORMAPS['CYAN'],
    4: ALL_COLORMAPS['MAGENTA'],
    5: ALL_COLORMAPS['YELLOW']
}

BLACK_COLORMAP = ALL_COLORMAPS['BLACK']

RGB_COLORMAPS = [
    ALL_COLORMAPS['RED'], ALL_COLORMAPS['LIME'], ALL_COLORMAPS['BLUE']
]

RG_COLORMAPS = RGB_COLORMAPS[:2]


def is_rgb_colormapping(colormaps: List[Colormap]) -> bool:
    """Check that given colormaps correspond to a RG(B) colormapping."""
    return ((len(colormaps) == 3 and colormaps == RGB_COLORMAPS)
            or (len(colormaps) == 2 and colormaps == RG_COLORMAPS))

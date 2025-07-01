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

from typing import Iterable, List, Optional, Tuple, Type, Union

from pims.api.exceptions import ColormapNotFoundProblem, FilterNotFoundProblem
from pims.api.utils.models import BitDepthEnum, ColormapEnum, ColormapId, IntensitySelectionEnum
from pims.files.file import Image
from pims.filters import AbstractFilter, FiltersById
from pims.formats.utils.structures.metadata import ImageChannel
from pims.processing.colormaps import BLACK_COLORMAP, ColorColormap, Colormap, ColormapsByName
from pims.utils.color import Color

Intensities = List[Union[int, str]]


def parse_intensity_bounds(
    image: Image, out_channels: List[int], out_zslices: List[int], out_timepoints: List[int],
    min_intensities: Intensities, max_intensities: Intensities, allow_none: bool = False
) -> Tuple[List[int], List[int]]:
    """
    Parse intensity parameters according to a specific image.

    Parameters
    ----------
    image
        Input image used to determine minimum and maximum admissible values per channel.
    out_channels
        Channel indexes expected in the output, used for intensities.
    out_zslices
        Z slices indexes expected in the output, used for AUTO_PLANE and STRETCH_PLANE.
    out_timepoints
        Timepoint indexes expected in the output, used for AUTO_PLANE ans STRETCH_PLANE.
    min_intensities
        List of minimum intensities. See API spec for admissible string constants.
    max_intensities
        List of maximum intensities. See API spec for admissible string constants.
    allow_none
        Whether the NONE string constant is admissible or not.

    Returns
    -------
    parsed_min_intensities
        Parsed min intensities. List size is the number of channels in the image output.
    parsed_max_intensities
        Parsed max intensities. List size is the number of channels in the image output.
    """
    bit_depth = image.significant_bits
    max_allowed_intensity = 2 ** bit_depth - 1
    n_out_channels = len(out_channels)

    if len(min_intensities) == 0:
        min_intensities = [0] * n_out_channels
    elif len(min_intensities) == 1:
        min_intensities = min_intensities * n_out_channels

    if len(max_intensities) == 0:
        max_intensities = [max_allowed_intensity] * n_out_channels
    elif len(max_intensities) == 1:
        max_intensities = max_intensities * n_out_channels

    def parse_intensity(c, bound_value, bound_default, bound_kind):
        bound_kind_idx = 0 if bound_kind == "minimum" else 1

        def stretch_plane():
            bounds = []
            for z in out_zslices:
                for t in out_timepoints:
                    bounds.append(image.plane_bounds(c, z, t)[bound_kind_idx])
            func = min if bound_kind == "minimum" else max
            return func(bounds)

        if type(bound_value) is int:
            if bound_value < 0:
                return 0
            elif bound_value > max_allowed_intensity:
                return max_allowed_intensity
            else:
                return intensity
        else:
            if allow_none and bound_value == "NONE":
                return bound_default
            elif bound_value == IntensitySelectionEnum.AUTO_IMAGE:
                if image.significant_bits <= 8:
                    return bound_default
                else:
                    return image.channel_bounds(c)[bound_kind_idx]
            elif bound_value == IntensitySelectionEnum.STRETCH_IMAGE:
                return image.channel_bounds(c)[bound_kind_idx]
            elif bound_value == IntensitySelectionEnum.AUTO_PLANE:
                if image.significant_bits <= 8:
                    return bound_default
                else:
                    return stretch_plane()
            elif bound_value == IntensitySelectionEnum.STRETCH_PLANE:
                return stretch_plane()
            else:
                return bound_default

    for idx, (channel, intensity) in enumerate(zip(out_channels, min_intensities)):
        min_intensities[idx] = parse_intensity(channel, intensity, 0, "minimum")

    for idx, (channel, intensity) in enumerate(zip(out_channels, max_intensities)):
        max_intensities[idx] = parse_intensity(
            channel, intensity, max_allowed_intensity, "maximum"
        )

    return min_intensities, max_intensities


def parse_bitdepth(in_image: Image, bits: Union[int, BitDepthEnum]) -> int:
    return in_image.significant_bits if bits == BitDepthEnum.AUTO else bits


def parse_filter_ids(
    filter_ids: Iterable[str], existing_filters: FiltersById
) -> List[Type[AbstractFilter]]:
    filters = []
    for filter_id in filter_ids:
        try:
            filters.append(existing_filters[filter_id.upper()])
        except KeyError:
            raise FilterNotFoundProblem(filter_id)
    return filters


def parse_colormap_ids(
    colormap_ids: List[ColormapId], existing_colormaps: ColormapsByName, channel_idxs: List[int],
    img_channels: List[ImageChannel]
) -> List[Union[Colormap, None]]:
    colormaps = []
    if len(colormap_ids) == 0:
        colormap_ids = [ColormapEnum.DEFAULT] * len(channel_idxs)
    elif len(colormap_ids) == 1:
        colormap_ids = colormap_ids * len(channel_idxs)

    for i, colormap_id in zip(channel_idxs, colormap_ids):
        colormaps.append(
            parse_colormap_id(
                colormap_id, existing_colormaps, img_channels[i].color
            )
        )
    return colormaps


def parse_colormap_id(
    colormap_id: ColormapId, existing_colormaps: ColormapsByName, default_color: Optional[Color]
) -> Optional[Colormap]:
    """
    Parse a colormap ID to a valid colormap (or None).

    If the parsed ID is a valid colormap which is not registered in the
    existing colormaps, the valid colormap is added to the set of existing ones
    as a side effect.

    Parameters
    ----------
    colormap_id
    existing_colormaps
        Existing colormaps
    default_color
        The color for a monotonic linear colormap if the colormap ID is
        `ColormapEnum.DEFAULT`.

    Returns
    -------
    colormap
        The parsed colormap. If None, no colormap has to be applied.

    Raises
    ------
    ColormapNotFoundProblem
        If the colormap ID cannot be associated to any colormap.
    """
    if colormap_id == ColormapEnum.NONE:
        return None
    elif colormap_id == ColormapEnum.DEFAULT:
        if default_color is None:
            return None
        colormap_id = str(default_color).upper()
    elif colormap_id == ColormapEnum.DEFAULT_INVERTED:
        if default_color is None:
            return existing_colormaps.get('!WHITE')
        colormap_id = '!' + str(default_color).upper()
    else:
        colormap_id = colormap_id.upper()  # noqa

    colormap = existing_colormaps.get(str(colormap_id))
    if colormap is None:
        inverted = colormap_id[0] == "!"
        color = colormap_id[1:] if inverted else colormap_id

        try:
            parsed_color = Color(color)
        except ValueError:
            raise ColormapNotFoundProblem(colormap_id)

        colormap = ColorColormap(parsed_color, inverted=inverted)
        existing_colormaps[colormap.identifier] = colormap
    return colormap


def parse_gammas(
    out_channels: List[int], gammas: List[float]
):
    """
    Parse gammas parameter.

    Parameters
    ----------
    out_channels
        Channel indexes expected in the output
    gammas
        List of gammas. List length is 0, 1, or len(out_channels)
    Returns
    -------
    gammas:
        List of gammas. List length is the number of channels in the output.
    """
    if len(gammas) == 0:
        return [1] * len(out_channels)
    elif len(gammas) == 1:
        return gammas * len(out_channels)
    else:
        return gammas


def remove_useless_channels(
    channel_idxs: List[int],
    min_intensities: List[int],
    max_intensities: List[int],
    colormaps: List[Colormap],
    gammas: List[float]
) -> Tuple[List[int], List[int], List[int], List[Colormap], List[float]]:
    """
    Remove channels with a black colormap, as they will produce a black image
    and are useless.

    In the case all channels are black, we keep one to guarantee the pipeline is
    working, but a black image will be returned in the end...
    """

    kept_idxs = []
    kept_min_intensities = []
    kept_max_intensities = []
    kept_colormaps = []
    kept_gammas = []
    for idx, min_intensity, max_intensity, colormap, gamma in \
            zip(channel_idxs, min_intensities, max_intensities,
                colormaps, gammas):
        intensity_diff = max_intensity - min_intensity
        if colormap != BLACK_COLORMAP and intensity_diff != 0:
            kept_idxs.append(idx)
            kept_min_intensities.append(min_intensity)
            kept_max_intensities.append(max_intensity)
            kept_colormaps.append(colormap)
            kept_gammas.append(gamma)

    if len(kept_idxs) == 0:
        kept_idxs = [channel_idxs[0]]
        kept_min_intensities = [min_intensities[0]]
        kept_max_intensities = [max_intensities[0]]
        kept_colormaps = [colormaps[0]]
        kept_gammas = [gammas[0]]

    channel_idxs = kept_idxs
    min_intensities = kept_min_intensities
    max_intensities = kept_max_intensities
    colormaps = kept_colormaps
    gammas = kept_gammas

    return channel_idxs, min_intensities, max_intensities, colormaps, gammas

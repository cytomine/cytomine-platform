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

from copy import copy
from typing import List, Optional, Union

from pims.api.exceptions import BadRequestException
from pims.api.utils.models import ChannelReduction, GenericReduction, TierIndexType
from pims.api.utils.output_parameter import Size, check_level_validity, check_zoom_validity
from pims.api.utils.range_parameter import is_range, parse_range
from pims.files.file import Image
from pims.processing.region import Region
from pims.utils.iterables import ensure_list


def parse_region(
    in_image: Image, top: Size, left: Size, width: Size, height: Size, tier_idx: int = 0,
    tier_type: TierIndexType = TierIndexType.LEVEL, silent_oob: bool = False
) -> Region:
    """
    Parse a region

    Parameters
    ----------
    in_image
        Image in which region is extracted
    top
    left
    width
    height
    tier_idx
        Tier index to use as reference
    tier_type
        Type of tier index
    silent_oob
        Whether out of bounds region should raise an error or not.

    Returns
    -------
    region
        The parsed region

    Raises
    ------
    BadRequestException
        If a region coordinate is out of bound and silent_oob is False.
    """
    if tier_type == TierIndexType.ZOOM:
        check_zoom_validity(in_image.pyramid, tier_idx)
        ref_tier = in_image.pyramid.get_tier_at_zoom(tier_idx)
    else:
        check_level_validity(in_image.pyramid, tier_idx)
        ref_tier = in_image.pyramid.get_tier_at_level(tier_idx)

    if type(top) == float:
        top *= ref_tier.height
    if type(left) == float:
        left *= ref_tier.width
    if type(width) == float:
        width *= ref_tier.width
    if type(height) == float:
        height *= ref_tier.height

    downsample = (ref_tier.width_factor, ref_tier.height_factor)
    region = Region(top, left, width, height, downsample)

    if not silent_oob:
        clipped = copy(region).clip(ref_tier.width, ref_tier.height)
        if clipped != region:
            raise BadRequestException(
                detail=f"Some coordinates of region {region} are out of bounds."
            )

    return region


def parse_planes(
    planes_to_parse: List[Union[int, str]], n_planes: int, default: Union[int, List[int]] = 0,
    name: str = 'planes'
) -> List[int]:
    """
    Get a set of planes from a list of plane indexes and ranges.

    Parameters
    ----------
    planes_to_parse
        List of plane indexes and ranges to parse.
    n_planes
        Number of planes. It is the maximum output set size.
    default
        Plane index or list of plane indexes used as default set if `planes_to_parse` is empty.
        Default is returned as a set but default values are expected to be in acceptable range.
    name
        Name of plane dimension (e.g. 'channels', 'z_slices', ...) used for exception messages.

    Returns
    -------
    plane_set
        Ordered list of valid plane indexes (where duplicates have been removed).

    Raises
    ------
    BadRequestException
        If an item of `planes_to_parse´ is invalid.
        If the set of valid planes is empty
    """
    plane_indexes = list()

    if len(planes_to_parse) == 0:
        return sorted(set((ensure_list(default))))

    for plane in planes_to_parse:
        if type(plane) is int or (type(plane) is str and plane.isdigit()):
            plane_indexes.append(int(plane))
        elif is_range(plane):
            plane_indexes += [*parse_range(plane, 0, n_planes)]
        else:
            raise BadRequestException(
                detail=f'{plane} is not a valid index or range for {name}.'
            )
    plane_set = sorted(set([idx for idx in plane_indexes if 0 <= idx < n_planes]))
    if len(plane_set) == 0:
        raise BadRequestException(detail=f"No valid indexes for {name}")
    return plane_set


def get_channel_indexes(image: Image, planes: List[Union[int, str]]) -> List[int]:
    """
    Image channels used to render the response.
    This parameter is interpreted as a set such that duplicates are ignored.
    By default, all channels are considered.
    """
    default = [*range(0, image.n_channels)]
    return parse_planes(planes, image.n_channels, default, 'channels')


def get_zslice_indexes(image: Image, planes: List[int]) -> List[int]:
    """
    Image focal planes used to render the response.
    This parameter is interpreted as a set such that duplicates are ignored.
    By default, the median focal plane is considered.
    """
    default = [round(image.depth / 2)]
    return parse_planes(planes, image.depth, default, 'z_slices')


def get_timepoint_indexes(image: Image, planes: List[int]) -> List[int]:
    """
    Image timepoints used to render the response.
    This parameter is interpreted as a set such that duplicates are ignored.
    By default, the first timepoint considered.
    """
    default = [0]
    return parse_planes(planes, image.duration, default, 'timepoints')


def check_reduction_validity(
    planes: List[int], reduction: Optional[Union[GenericReduction, ChannelReduction]],
    name: str = 'planes'
):
    """
    Verify if a reduction function is given when needed i.e. when
    the set of planes has a size > 1.

    Parameters
    ----------
    planes
        Set of planes
    reduction
        Reduction function to reduce the set of planes.
    name
        Name of plane dimension (e.g. 'channels', 'z_slices', ...) used for exception messages.

    Raises
    ------
    BadRequestException
        If no reduction function is given while needed.
    """
    if len(planes) > 1 and reduction is None:
        raise BadRequestException(detail=f'A reduction is required for {name}')

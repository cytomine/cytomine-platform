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

from typing import Optional, Tuple, Union

from pims.api.exceptions import BadRequestException, TooLargeOutputProblem
from pims.api.utils.header import SafeMode
from pims.api.utils.models import TierIndexType
from pims.files.file import Image
from pims.formats.utils.structures.pyramid import Pyramid
from pims.processing.region import Region
from pims.utils.math import get_rationed_resizing

Size = Union[int, float]


def get_thumb_output_dimensions(
    in_image: Image, height: Optional[Size] = None, width: Optional[Size] = None,
    length: Optional[Size] = None, zoom: Optional[int] = None, level: Optional[int] = None,
    allow_upscaling: bool = True
) -> Tuple[int, int]:
    """
    Get output dimensions according, by order of precedence, either height, either width,
    either the largest image length, either zoom or level and such that ratio is preserved.

    Parameters
    ----------
    in_image
        Input image with the aspect ratio to preserve.
    height
        Output height absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` is not None.
    width
        Output width absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` or `height` is not None.
    length
        Output largest side absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` or `width` or `height` is not None.
    zoom
        Output zoom tier to consider as size.
        The zoom tier is expected to be valid for the input image.
        Ignored if `level` is not None.
    level
        Output level tier to consider as size.
        The level tier is expected to be valid for the input image.
    allow_upscaling
        Whether the output thumb size can be greater than the input image size.
        If upscaling is not allowed, maximum thumb size is the input image size.

    Returns
    -------
    out_width
        Output width preserving aspect ratio.
    out_height
        Output height preserving aspect ratio.

    Raises
    ------
    BadRequestException
        If it is impossible to determine output dimensions.
    """
    if level is not None:
        tier = in_image.pyramid.get_tier_at_level(level)
        out_height, out_width = tier.height, tier.width
    elif zoom is not None:
        tier = in_image.pyramid.get_tier_at_zoom(zoom)
        out_height, out_width = tier.height, tier.width
    elif height is not None:
        out_height, out_width = get_rationed_resizing(height, in_image.height, in_image.width)
    elif width is not None:
        out_width, out_height = get_rationed_resizing(width, in_image.width, in_image.height)
    elif length is not None:
        if in_image.width > in_image.height:
            out_width, out_height = get_rationed_resizing(length, in_image.width, in_image.height)
        else:
            out_height, out_width = get_rationed_resizing(length, in_image.height, in_image.width)
    else:
        raise BadRequestException(
            detail='Impossible to determine output dimensions. '
                   'Height, width and length cannot all be unset.'
        )

    if not allow_upscaling and (out_width > in_image.width or out_height > in_image.height):
        return in_image.width, in_image.height

    return out_width, out_height


def get_window_output_dimensions(
    in_image: Image, region: Region, height: Optional[Size] = None, width: Optional[Size] = None,
    length: Optional[Size] = None, zoom: Optional[int] = None, level: Optional[int] = None
) -> Tuple[int, int]:
    """
    Get output dimensions according, by order of precedence, either height, either width,
    either the largest image length, either zoom or level and such that region ratio is preserved.

    Parameters
    ----------
    in_image
        Input image from which region is extracted.
    region
        Input region with aspect ratio to preserve.
    height
        Output height absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` is not None.
    width
        Output width absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` or `height` is not None.
    length
        Output largest side absolute size (int) or ratio (float).
        Ignored if `level` or `zoom` or `width` or `height` is not None.
    zoom
        Output zoom tier to consider as size.
        The zoom tier is expected to be valid for the input image.
        Ignored if `level` is not None.
    level
        Output level tier to consider as size.
        The level tier is expected to be valid for the input image.

    Returns
    -------
    out_width
        Output width preserving aspect ratio.
    out_height
        Output height preserving aspect ratio.

    Raises
    ------
    BadRequestException
        If it is impossible to determine output dimensions.
    """
    if level is not None:
        tier = in_image.pyramid.get_tier_at_level(level)
        out_height, out_width = round(region.true_height / tier.height_factor), round(
            region.true_width / tier.width_factor
            )
    elif zoom is not None:
        tier = in_image.pyramid.get_tier_at_zoom(zoom)
        out_height, out_width = round(region.true_height / tier.height_factor), round(
            region.true_width / tier.width_factor
            )
    elif height is not None:
        out_height, out_width = get_rationed_resizing(
            height, int(region.true_height), int(region.true_width)
        )
    elif width is not None:
        out_width, out_height = get_rationed_resizing(
            width, int(region.true_width), int(region.true_height)
        )
    elif length is not None:
        if region.true_width > region.true_height:
            out_width, out_height = get_rationed_resizing(
                length, int(region.true_width), int(region.true_height)
            )
        else:
            out_height, out_width = get_rationed_resizing(
                length, int(region.true_height), int(region.true_width)
            )
    else:
        raise BadRequestException(
            detail='Impossible to determine output dimensions. '
                   'Height, width and length cannot all be unset.'
        )

    return out_width, out_height


def safeguard_output_dimensions(
    safe_mode: SafeMode, max_size: int, width: int, height: int
) -> Tuple[int, int]:
    """
    Safeguard image output dimensions according to safe mode and maximum
    admissible size.

    Parameters
    ----------
    safe_mode : SafeMode
        How to handle too large image response. See API specification for details.
    max_size : int
        Maximum admissible size when mode is SAFE_*
    width : int
        Expected output width
    height : int
        Expected output height

    Returns
    -------
    width : int
        Safeguarded output width according to mode.
    height : int
        Safeguarded output height according to mode.

    Raises
    ------
    TooLargeOutputProblem
        If mode is SAFE_REJECT and the expect output size is unsafe.
    """
    if safe_mode == SafeMode.UNSAFE:
        return width, height
    elif safe_mode == SafeMode.SAFE_REJECT and (width > max_size or height > max_size):
        raise TooLargeOutputProblem(width, height, max_size)
    elif safe_mode == SafeMode.SAFE_RESIZE and (width > max_size or height > max_size):
        if width > height:
            return get_rationed_resizing(max_size, width, height)
        else:
            height, width = get_rationed_resizing(max_size, height, width)
            return width, height
    else:
        return width, height


def check_level_validity(pyramid: Pyramid, level: Optional[int]):
    """ Check the level tier exists in the image pyramid.

    Parameters
    ----------
    pyramid : Pyramid
        Image pyramid
    level : int or None
        Level to be checked for existence in the image pyramid

    Raises
    ------
    BadRequestException
        If the given level is not in the image pyramid.
    """

    if level is not None and not 0 <= level <= pyramid.max_level:
        raise BadRequestException(detail=f"Level tier {level} does not exist.")


def check_zoom_validity(pyramid: Pyramid, zoom: Optional[int]):
    """Check the zoom tier exists in the image pyramid.

    Parameters
    ----------
    pyramid : Pyramid
        Image pyramid
    zoom : int or None
        Zoom to be checked for existence in the image pyramid

    Raises
    ------
    BadRequestException
        If the given zoom is not in the image pyramid.
    """

    if zoom is not None and not 0 <= zoom <= pyramid.max_zoom:
        raise BadRequestException(detail=f"Zoom tier {zoom} does not exist.")


def check_tileindex_validity(pyramid: Pyramid, ti: int, tier_idx: int, tier_type: TierIndexType):
    """
    Check the tile index exists in the image pyramid at given tier.

    Parameters
    ----------
    pyramid
        Image pyramid
    ti
        Tile index to check
    tier_idx
        Tier index in the pyramid expected to contain the tile
    tier_type
        Tier type

    Raises
    ------
    BadRequestException
        If the tile index is invalid.
    """
    if tier_type == TierIndexType.ZOOM:
        check_zoom_validity(pyramid, tier_idx)
        ref_tier = pyramid.get_tier_at_zoom(tier_idx)
    else:
        check_level_validity(pyramid, tier_idx)
        ref_tier = pyramid.get_tier_at_level(tier_idx)

    if not 0 <= ti < ref_tier.max_ti:
        raise BadRequestException(f"Tile index {ti} is invalid for tier {ref_tier}.")


def check_tilecoord_validity(
    pyramid: Pyramid, tx: int, ty: int, tier_idx: int, tier_type: TierIndexType
):
    """
    Check the tile index exists in the image pyramid at given tier.

    Parameters
    ----------
    pyramid
        Image pyramid
    tx
        Tile coordinate along X axis to check
    ty
        Tile coordinate along Y axis to check
    tier_idx
        Tier index in the pyramid expected to contain the tile
    tier_type
        Tier type

    Raises
    ------
    BadRequestException
        If the tile index is invalid.
    """
    if tier_type == TierIndexType.ZOOM:
        check_zoom_validity(pyramid, tier_idx)
        ref_tier = pyramid.get_tier_at_zoom(tier_idx)
    else:
        check_level_validity(pyramid, tier_idx)
        ref_tier = pyramid.get_tier_at_level(tier_idx)

    if not 0 <= tx < ref_tier.max_tx:
        raise BadRequestException(
            f"Tile coordinate {tx} along X axis is invalid for tier {ref_tier}."
        )

    if not 0 <= ty < ref_tier.max_ty:
        raise BadRequestException(
            f"Tile coordinate {ty} along Y axis is invalid for tier {ref_tier}."
        )

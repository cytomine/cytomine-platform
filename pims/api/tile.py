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
from typing import Optional

from fastapi import APIRouter, Depends, Path as PathParam, Query
from starlette.requests import Request
from starlette.responses import Response

from pims.api.exceptions import BadRequestException, check_representation_existence
from pims.api.utils.header import ImageRequestHeaders, SafeMode, add_image_size_limit_header
from pims.api.utils.input_parameter import (
    check_reduction_validity, get_channel_indexes, get_timepoint_indexes,
    get_zslice_indexes
)
from pims.api.utils.mimetype import (
    OutputExtension, VISUALISATION_MIMETYPES,
    extension_path_parameter, get_output_format
)
from pims.api.utils.models import (
    ChannelReduction, Colorspace, ImageOpsDisplayQueryParams,
    PlaneSelectionQueryParams, TargetLevel, TargetZoom, TargetZoomTileCoordinates,
    TargetZoomTileIndex, TierIndexType, TileIndex, TileRequest, TileX, TileY
)
from pims.api.utils.output_parameter import (
    check_tilecoord_validity, check_tileindex_validity,
    safeguard_output_dimensions
)
from pims.api.utils.parameter import imagepath_parameter
from pims.api.utils.processing_parameter import (
    parse_colormap_ids, parse_filter_ids,
    parse_gammas, parse_intensity_bounds, remove_useless_channels
)
from pims.cache import cache_image_response
from pims.config import Settings, get_settings
from pims.files.file import Path
from pims.filters import FILTERS
from pims.processing.colormaps import ALL_COLORMAPS
from pims.processing.image_response import TileResponse, WindowResponse
from pims.utils.iterables import check_array_size_parameters, ensure_list

router = APIRouter(prefix=get_settings().api_base_path)
tile_tags = ['Tiles']
norm_tile_tags = ['Normalized tiles']


@router.post('/image/{filepath:path}/tile{extension:path}', tags=tile_tags)
async def show_tile_with_body(
    request: Request, response: Response,
    body: TileRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    Get a 8-bit tile optimized for visualisation, with given channels, focal planes and
    timepoints. If multiple channels are given (slice or selection), they are merged. If
    multiple focal planes or timepoints are given (slice or selection), a reduction function
    must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
     tile is extracted from the median focal plane at first timepoint.
    """
    return await _show_tile(
        request, response,
        path, **body.model_dump(serialize_as_any=True), normalized=False,
        extension=extension, headers=headers, config=config
    )


@router.post('/image/{filepath:path}/normalized-tile{extension:path}', tags=norm_tile_tags)
async def show_tile_with_body(
    request: Request, response: Response,
    body: TileRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    Get a 8-bit normalized tile optimized for visualisation, with given channels, focal planes
    and timepoints. If multiple channels are given (slice or selection), they are merged. If
    multiple focal planes or timepoints are given (slice or selection), a reduction function
    must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
     tile is extracted from the median focal plane at first timepoint.
    """
    return await _show_tile(
        request, response,
        path, **body.model_dump(serialize_as_any=True), normalized=True,
        extension=extension, headers=headers, config=config
    )


@cache_image_response()
async def _show_tile(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    normalized: bool,
    tile: dict,
    channels, z_slices, timepoints,
    min_intensities, max_intensities, filters, gammas, threshold, log,
    extension, headers, config,
    colormaps=None, c_reduction=ChannelReduction.ADD, z_reduction=None, t_reduction=None
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        if not normalized or in_image.is_pyramid_normalized:
            pyramid = in_image.pyramid
            is_window = False
        else:
            pyramid = in_image.normalized_pyramid
            is_window = True

        if 'zoom' in tile:
            reference_tier_index = tile['zoom']
            tier_index_type = TierIndexType.ZOOM
        else:
            reference_tier_index = tile['level']
            tier_index_type = TierIndexType.LEVEL

        if 'ti' in tile:
            check_tileindex_validity(
                pyramid, tile['ti'],
                reference_tier_index, tier_index_type
            )
            tile_region = pyramid.get_tier_at(
                reference_tier_index, tier_index_type
            ).get_ti_tile(tile['ti'])
        else:
            check_tilecoord_validity(
                pyramid, tile['tx'], tile['ty'],
                reference_tier_index, tier_index_type
            )
            tile_region = pyramid.get_tier_at(
                reference_tier_index, tier_index_type
            ).get_txty_tile(tile['tx'], tile['ty'])

        out_format, mimetype = get_output_format(extension, headers.accept, VISUALISATION_MIMETYPES)
        req_size = tile_region.width, tile_region.height
        out_size = safeguard_output_dimensions(headers.safe_mode, config.output_size_limit, *req_size)
        out_width, out_height = out_size

        channels = ensure_list(channels)
        z_slices = ensure_list(z_slices)
        timepoints = ensure_list(timepoints)

        channels = get_channel_indexes(in_image, channels)
        check_reduction_validity(channels, c_reduction, 'channels')
        z_slices = get_zslice_indexes(in_image, z_slices)
        check_reduction_validity(z_slices, z_reduction, 'z_slices')
        timepoints = get_timepoint_indexes(in_image, timepoints)
        check_reduction_validity(timepoints, t_reduction, 'timepoints')

        min_intensities = ensure_list(min_intensities)
        max_intensities = ensure_list(max_intensities)
        colormaps = ensure_list(colormaps)
        filters = ensure_list(filters)
        gammas = ensure_list(gammas)

        array_parameters = ('min_intensities', 'max_intensities', 'colormaps', 'gammas')
        check_array_size_parameters(
            array_parameters, locals(), allowed=[0, 1, len(channels)], nullable=False
        )
        intensities = parse_intensity_bounds(
            in_image, channels, z_slices, timepoints, min_intensities, max_intensities
        )
        min_intensities, max_intensities = intensities
        colormaps = parse_colormap_ids(colormaps, ALL_COLORMAPS, channels, in_image.channels)
        gammas = parse_gammas(channels, gammas)

        channels, min_intensities, max_intensities, colormaps, gammas = remove_useless_channels(
            channels, min_intensities, max_intensities, colormaps, gammas
        )

        array_parameters = ('filters',)
        check_array_size_parameters(
            array_parameters, locals(), allowed=[0, 1], nullable=False
        )
        filters = parse_filter_ids(filters, FILTERS)

        if is_window:
            tile = WindowResponse(
                in_image, channels, z_slices, timepoints,
                tile_region, out_format, out_width, out_height,
                c_reduction, z_reduction, t_reduction,
                gammas, filters, colormaps, min_intensities, max_intensities, log,
                8, threshold, Colorspace.AUTO
            )
        else:
            tile = TileResponse(
                in_image, channels, z_slices, timepoints,
                tile_region, out_format, out_width, out_height,
                c_reduction, z_reduction, t_reduction,
                gammas, filters, colormaps, min_intensities, max_intensities, log,
                threshold
            )

        return tile.http_response(
            mimetype,
            extra_headers=add_image_size_limit_header(dict(), *req_size, *out_size)
        )


def zoom_query_parameter(
    zoom: int = PathParam(...)
):
    return TargetZoom(zoom).model_dump()


def level_query_parameter(
    level: int = PathParam(...)
):
    return TargetLevel(level).model_dump()


def ti_query_parameter(
    ti: int = PathParam(...)
):
    return TileIndex(ti).model_dump()


def tx_query_parameter(
    tx: int = PathParam(...)
):
    return TileX(tx).model_dump()


def ty_query_parameter(
    ty: int = PathParam(...)
):
    return TileY(ty).model_dump()

@router.get(
    '/image/{filepath:path}/tile/zoom/{zoom:int}/ti/{ti:int}{extension:path}', tags=tile_tags
)
async def show_tile_by_zoom(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    zoom: int = Depends(zoom_query_parameter),
    ti: int = Depends(ti_query_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    planes: PlaneSelectionQueryParams = Depends(),
    ops: ImageOpsDisplayQueryParams = Depends(),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings),
):
    """
    Get a 8-bit tile at a given zoom level and tile index, optimized for visualisation,
    with given channels, focal planes and timepoints. If multiple channels are given (slice or
    selection), they are merged. If multiple focal planes or timepoints are given (slice or
    selection), a reduction function must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
    tile is extracted from the median focal plane at first timepoint.
    """
    tile = dict(zoom=zoom, ti=ti)
    return await _show_tile(
        request, response,
        path, False, tile, **planes.dict(), **ops.dict(),
        extension=extension, headers=headers, config=config
    )

@router.get(
    '/image/{filepath:path}/tile/level/{level:int}/ti/{ti:int}{extension:path}', tags=tile_tags
)
async def show_tile_by_level(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    level: int = Depends(level_query_parameter),
    ti: int = Depends(ti_query_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    planes: PlaneSelectionQueryParams = Depends(),
    ops: ImageOpsDisplayQueryParams = Depends(),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings),
):
    """
    Get a 8-bit tile at a given zoom level and tile index, optimized for visualisation,
    with given channels, focal planes and timepoints. If multiple channels are given (slice or
    selection), they are merged. If multiple focal planes or timepoints are given (slice or
    selection), a reduction function must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
     tile is extracted from the median focal plane at first timepoint.
    """
    tile = dict(level=level, ti=ti)
    return await _show_tile(
        request, response,
        path, False, tile, **planes.dict(), **ops.dict(),
        extension=extension, headers=headers, config=config
    )


@router.get(
    '/image/{filepath:path}/normalized-tile/zoom/{zoom:int}/tx/{tx:int}/ty/{ty:int}{extension:path}',
    tags=norm_tile_tags
)
async def show_normalized_tile_by_xyz(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    zoom: int = Depends(zoom_query_parameter),
        tx: int = Depends(tx_query_parameter),
        ty: int = Depends(ty_query_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    planes: PlaneSelectionQueryParams = Depends(),
    ops: ImageOpsDisplayQueryParams = Depends(),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings),
):
    """
    Get a 8-bit normalized tile at a given zoom level and tile index, optimized for
    visualisation, with given channels, focal planes and timepoints. If multiple channels are
    given (slice or selection), they are merged. If multiple focal planes or timepoints are
    given (slice or selection), a reduction function must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
    tile is extracted from the median focal plane at first timepoint.
    """
    tile = dict(zoom=zoom, tx=tx, ty=ty)
    return await _show_tile(
        request, response,
        path, True, tile, **planes.dict(), **ops.dict(),
        extension=extension, headers=headers, config=config
    )

@router.get(
    '/image/{filepath:path}/normalized-tile/zoom/{zoom:int}/ti/{ti:int}{extension:path}',
    tags=norm_tile_tags
)
async def show_normalized_tile_by_zoom(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    zoom: int = Depends(zoom_query_parameter),
    ti: int = Depends(ti_query_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    planes: PlaneSelectionQueryParams = Depends(),
    ops: ImageOpsDisplayQueryParams = Depends(),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings),
):
    """
    Get a 8-bit normalized tile at a given zoom level and tile index, optimized for
    visualisation, with given channels, focal planes and timepoints. If multiple channels are
    given (slice or selection), they are merged. If multiple focal planes or timepoints are
    given (slice or selection), a reduction function must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
    tile is extracted from the median focal plane at first timepoint.
    """
    tile = dict(zoom=zoom, ti=ti)
    return await _show_tile(
        request, response,
        path, True, tile, **planes.dict(), **ops.dict(),
        extension=extension, headers=headers, config=config
    )


@router.get(
    '/image/{filepath:path}/normalized-tile/level/{level:int}/ti/{ti:int}{extension:path}',
    tags=norm_tile_tags
)
async def show_normalized_tile_by_level(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    level: int = Depends(level_query_parameter),
    ti: int = Depends(ti_query_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    planes: PlaneSelectionQueryParams = Depends(),
    ops: ImageOpsDisplayQueryParams = Depends(),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings),
):
    """
    Get a 8-bit normalized tile at a given zoom level and tile index, optimized for
    visualisation, with given channels, focal planes and timepoints. If multiple channels are
    given (slice or selection), they are merged. If multiple focal planes or timepoints are
    given (slice or selection), a reduction function must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
     tile is extracted from the median focal plane at first timepoint.
    """
    tile = dict(level=level, ti=ti)
    return await _show_tile(
        request, response,
        path, True, tile, **planes.dict(), **ops.dict(),
        extension=extension, headers=headers, config=config
    )


@router.get('/image/tile.jpg', tags=norm_tile_tags, deprecated=True)
async def show_tile_v1(
    request: Request, response: Response,
    zoomify: str,
    x: int,
    y: int,
    z: int,
    ops: ImageOpsDisplayQueryParams = Depends(),
    mime_type: Optional[str] = Query(None, alias='mimeType'),  # noqa
    tile_group: Optional[str] = Query(None, alias='tileGroup'),  # noqa
    config: Settings = Depends(get_settings)
):
    """
    Get a tile using IMS V1.x specification.
    """
    zoom = TargetZoom(z)
    tx, ty = TileX(x), TileY(y)
    tile = TargetZoomTileCoordinates(zoom=zoom, tx=tx, ty=ty)
    return await _show_tile(
        request, response,
        imagepath_parameter(zoomify),
        normalized=True,
        tile=tile.dict(),
        channels=None, z_slices=None, timepoints=None,
        **ops.dict(),
        extension=OutputExtension.JPEG,
        headers=ImageRequestHeaders("image/jpeg", SafeMode.SAFE_RESIZE),
        config=config
    )


@router.get('/slice/tile', tags=norm_tile_tags, deprecated=True)
async def show_tile_v2(
    request: Request, response: Response,
    z: int,
    fif: Optional[str] = None,
    zoomify: Optional[str] = None,
    x: Optional[int] = None,
    y: Optional[int] = None,
    tile_index: Optional[int] = Query(None, alias='tileIndex'),
    ops: ImageOpsDisplayQueryParams = Depends(),
    tile_group: Optional[str] = Query(None, alias='tileGroup'),
    mime_type: str = Query(None, alias='mimeType'),  # noqa
    config: Settings = Depends(get_settings)
):
    """
    Get a tile using IMS V2.x specification.
    """
    zoom = TargetZoom(z)
    if all(i is not None for i in (zoomify, tile_group, x, y)):
        tx, ty = TileX(x), TileY(y)
        tile = TargetZoomTileCoordinates(zoom=zoom, tx=tx, ty=ty)
        path = imagepath_parameter(zoomify)
    elif all(i is not None for i in (fif, z, tile_index)):
        ti = TileIndex(tile_index)
        tile = TargetZoomTileIndex(zoom=zoom, ti=ti)
        path = imagepath_parameter(fif)
    else:
        raise BadRequestException(detail="Incoherent set of parameters.")

    return await _show_tile(
        request, response,
        path,
        normalized=True,
        tile=tile.dict(),
        channels=None, z_slices=None, timepoints=None,
        **ops.dict(),
        extension=OutputExtension.JPEG,
        headers=ImageRequestHeaders("image/jpeg", SafeMode.SAFE_RESIZE),
        config=config
    )

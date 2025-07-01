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

from fastapi import APIRouter, Depends, Query
from starlette.requests import Request
from starlette.responses import Response

from pims.api.exceptions import check_representation_existence
from pims.api.utils.header import ImageRequestHeaders, add_image_size_limit_header
from pims.api.utils.input_parameter import (
    check_reduction_validity, get_channel_indexes, get_timepoint_indexes,
    get_zslice_indexes
)
from pims.api.utils.mimetype import (
    OutputExtension, VISUALISATION_MIMETYPES,
    extension_path_parameter, get_output_format
)
from pims.api.utils.models import (
    ChannelReduction, ImageOpsDisplayQueryParams, ImageOutDisplayQueryParams,
    PlaneSelectionQueryParams, ThumbnailRequest
)
from pims.api.utils.output_parameter import (
    get_thumb_output_dimensions,
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
from pims.processing.image_response import ThumbnailResponse
from pims.utils.iterables import check_array_size_parameters, ensure_list

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Thumbnails']


@router.get('/image/{filepath:path}/thumb{extension:path}', tags=api_tags)
async def show_thumb(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    output: ImageOutDisplayQueryParams = Depends(),
    planes: PlaneSelectionQueryParams = Depends(),
    operations: ImageOpsDisplayQueryParams = Depends(),
    use_precomputed: bool = Query(True),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    Get a 8-bit thumbnail optimized for visualisation, with given channels, focal planes and
    timepoints. If multiple channels are given (slice or selection), they are merged. If
    multiple focal planes or timepoints are given (slice or selection), a reduction function
    must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
    thumbnail is extracted from the median focal plane at first timepoint.
    """
    return await _show_thumb(
        request, response,
        path=path, **output.dict(), **planes.dict(), **operations.dict(),
        use_precomputed=use_precomputed, extension=extension,
        headers=headers, config=config
    )


@router.post('/image/{filepath:path}/thumb{extension:path}', tags=api_tags)
async def show_thumb_with_body(
    request: Request, response: Response,
    body: ThumbnailRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    Get a 8-bit thumbnail optimized for visualisation, with given channels, focal planes and
    timepoints. If multiple channels are given (slice or selection), they are merged. If
    multiple focal planes or timepoints are given (slice or selection), a reduction function
    must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
    thumbnail is extracted from the median focal plane at first timepoint.
    """
    return await _show_thumb(
        request, response, path, **body.model_dump(serialize_as_any=True), extension=extension,
        headers=headers, config=config
    )


@cache_image_response()
async def _show_thumb(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    height, width, length,
    channels, z_slices, timepoints,
    min_intensities, max_intensities, filters, gammas, threshold,
    log, use_precomputed,
    extension,
    headers,
    config: Settings,
    colormaps, c_reduction=ChannelReduction.ADD, z_reduction=None, t_reduction=None
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        out_format, mimetype = get_output_format(extension, headers.accept, VISUALISATION_MIMETYPES)
        req_size = get_thumb_output_dimensions(in_image, height, width, length)
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

        return ThumbnailResponse(
            in_image, channels, z_slices, timepoints,
            out_format, out_width, out_height,
            c_reduction, z_reduction, t_reduction,
            gammas, filters, colormaps, min_intensities, max_intensities,
            log, use_precomputed, threshold
        ).http_response(
            mimetype,
            extra_headers=add_image_size_limit_header(dict(), *req_size, *out_size)
        )

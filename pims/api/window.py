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
from typing import List, Union

from fastapi import APIRouter, Depends
from starlette.requests import Request
from starlette.responses import Response

from pims.api.exceptions import check_representation_existence
from pims.api.utils.annotation_parameter import parse_annotations
from pims.api.utils.header import ImageAnnotationRequestHeaders, add_image_size_limit_header
from pims.api.utils.input_parameter import (
    check_reduction_validity, get_channel_indexes, get_timepoint_indexes,
    get_zslice_indexes, parse_region
)
from pims.api.utils.mimetype import (
    OutputExtension, VISUALISATION_MIMETYPES,
    extension_path_parameter, get_output_format
)
from pims.api.utils.models import (
    AnnotationStyleMode, ChannelReduction, TierIndexType,
    WindowRequest
)
from pims.api.utils.output_parameter import (
    check_level_validity, check_tilecoord_validity, check_tileindex_validity, check_zoom_validity,
    get_window_output_dimensions,
    safeguard_output_dimensions
)
from pims.api.utils.parameter import imagepath_parameter
from pims.api.utils.processing_parameter import (
    parse_bitdepth, parse_colormap_ids, parse_filter_ids,
    parse_gammas, parse_intensity_bounds, remove_useless_channels
)
from pims.cache import cache_image_response
from pims.config import Settings, get_settings
from pims.files.file import Path
from pims.filters import FILTERS
from pims.processing.annotations import ParsedAnnotations, annotation_crop_affine_matrix
from pims.processing.colormaps import ALL_COLORMAPS
from pims.processing.image_response import MaskResponse, WindowResponse
from pims.processing.region import Region
from pims.utils.color import RED, WHITE
from pims.utils.iterables import check_array_size_parameters, ensure_list

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Windows']


@router.post('/image/{filepath:path}/window{extension:path}', tags=api_tags)
async def show_window_with_body(
    request: Request, response: Response,
    body: WindowRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageAnnotationRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    Get a window (rectangular crop) extract from an image, with given channels, focal planes and
    timepoints. If multiple channels are given (slice or selection), they are merged. If
    multiple focal planes or timepoints are given (slice or selection), a reduction function
    must be provided.

    **By default**, all image channels are used and when the image is multidimensional, the
     tile is extracted from the median focal plane at first timepoint.
    """
    return await _show_window(
        request, response,
        path, **body.model_dump(serialize_as_any=True),
        extension=extension, headers=headers, config=config
    )


@cache_image_response()
async def _show_window(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    region: Union[Region, dict],
    height, width, length, zoom, level,
    channels, z_slices, timepoints,
    min_intensities, max_intensities, filters, gammas, threshold,
    bits, colorspace,
    annotations: Union[ParsedAnnotations, dict, List[dict]],
    annotation_style: dict,
    extension,
    headers,
    config: Settings,
    colormaps=None, c_reduction=ChannelReduction.ADD, z_reduction=None, t_reduction=None
):
    in_image = await path.get_cached_spatial()
    check_representation_existence(in_image)

    if not isinstance(region, Region):
        tier_index_type = region['tier_index_type']
        reference_tier_index = region['reference_tier_index']
        if reference_tier_index is None:
            if tier_index_type == TierIndexType.LEVEL:
                reference_tier_index = 0
            else:
                reference_tier_index = in_image.pyramid.max_zoom

        if 'top' in region:
            # Parse raw WindowRegion to Region
            region = parse_region(
                in_image, region['top'], region['left'],
                region['width'], region['height'],
                reference_tier_index, tier_index_type,
                silent_oob=False
            )
        elif 'ti' in region:
            # Parse raw WindowTileIndex region to Region
            check_tileindex_validity(
                in_image.pyramid, region['ti'],
                reference_tier_index, tier_index_type
            )
            region = in_image.pyramid.get_tier_at(
                reference_tier_index,
                tier_index_type
            ).get_ti_tile(region['ti'])
        elif ('tx', 'ty') in region:
            # Parse raw WindowTileCoord region to Region
            check_tilecoord_validity(
                in_image.pyramid, region['tx'], region['ty'],
                reference_tier_index, tier_index_type
            )
            region = in_image.pyramid.get_tier_at(
                reference_tier_index,
                tier_index_type
            ).get_txty_tile(region['tx'], region['ty'])

    out_format, mimetype = get_output_format(extension, headers.accept, VISUALISATION_MIMETYPES)
    check_zoom_validity(in_image.pyramid, zoom)
    check_level_validity(in_image.pyramid, level)
    req_size = get_window_output_dimensions(in_image, region, height, width, length, zoom, level)
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

    out_bitdepth = parse_bitdepth(in_image, bits)

    if annotations and annotation_style and not isinstance(annotations, ParsedAnnotations):
        if annotation_style['mode'] == AnnotationStyleMode.DRAWING:
            ignore_fields = ['fill_color']
            default = {'stroke_color': RED, 'stroke_width': 1}
            point_envelope_length = annotation_style['point_envelope_length']
        else:
            ignore_fields = ['stroke_width', 'stroke_color']
            default = {'fill_color': WHITE}
            point_envelope_length = None

        annotations = parse_annotations(
            ensure_list(annotations), ignore_fields,
            default, point_envelope_length,
            origin=headers.annot_origin, im_height=in_image.height
        )

    affine = None
    if annotations:
        affine = annotation_crop_affine_matrix(annotations.region, region, *out_size)

    if annotations and annotation_style and \
            annotation_style['mode'] == AnnotationStyleMode.MASK:
        window = MaskResponse(
            in_image,
            annotations, affine,
            out_width, out_height, out_bitdepth, out_format
        )
    else:
        window = WindowResponse(
            in_image, channels, z_slices, timepoints,
            region, out_format, out_width, out_height,
            c_reduction, z_reduction, t_reduction,
            gammas, filters, colormaps,
            min_intensities, max_intensities, False,
            out_bitdepth, threshold, colorspace,
            annotations, affine, annotation_style
        )

    return window.http_response(
        mimetype,
        extra_headers=add_image_size_limit_header(dict(), *req_size, *out_size)
    )

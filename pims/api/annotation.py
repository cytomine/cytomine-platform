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

from fastapi import APIRouter, Depends
from starlette.requests import Request
from starlette.responses import Response

from pims.api.exceptions import check_representation_existence
from pims.api.utils.annotation_parameter import parse_annotations
from pims.api.utils.header import ImageAnnotationRequestHeaders, add_image_size_limit_header
from pims.api.utils.mimetype import (
    OutputExtension, PROCESSING_MIMETYPES,
    extension_path_parameter, get_output_format
)
from pims.api.utils.models import (
    AnnotationCropRequest, AnnotationDrawingRequest,
    AnnotationMaskRequest, AnnotationStyleMode, ChannelReduction, Colorspace
)
from pims.api.utils.output_parameter import (
    check_level_validity, check_zoom_validity, get_window_output_dimensions,
    safeguard_output_dimensions
)
from pims.api.utils.parameter import imagepath_parameter
from pims.api.window import _show_window
from pims.cache import cache_image_response
from pims.config import Settings, get_settings
from pims.files.file import Path
from pims.processing.annotations import annotation_crop_affine_matrix, get_annotation_region
from pims.processing.image_response import MaskResponse
from pims.utils.color import WHITE
from pims.utils.iterables import ensure_list

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Annotations']


@router.post('/image/{filepath:path}/annotation/mask{extension:path}', tags=api_tags)
async def show_mask(
    request: Request, response: Response,
    body: AnnotationMaskRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageAnnotationRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    The mask is a generated image where geometries are filled by their respective `fill_color`.
    The background is black.

    The input spatial region is given by the rectangular envelope of all geometries multiplied
    by an optional context factor. The target size is given by one of the scaling factors (
    `size`, `width`, `height`, `zoom` or `level`).

    By default, a binary mask with white foreground is returned as the default fill color is
    white for every annotation.

    Annotation `stroke_width` and `stroke_color` are ignored.
    """
    return await _show_mask(
        request, response,
        path, **body.model_dump(serialize_as_any=True),
        extension=extension, headers=headers, config=config
    )


@cache_image_response(
    supported_mimetypes=PROCESSING_MIMETYPES
)
async def _show_mask(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    annotations,
    context_factor,
    height, width, length, zoom, level,
    extension, headers, config
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        annots = parse_annotations(
            ensure_list(annotations),
            ignore_fields=['stroke_width', 'stroke_color'],
            default={'fill_color': WHITE},
            origin=headers.annot_origin, im_height=in_image.height
        )

        region = get_annotation_region(in_image, annots, context_factor)

        out_format, mimetype = get_output_format(extension, headers.accept, PROCESSING_MIMETYPES)
        check_zoom_validity(in_image.pyramid, zoom)
        check_level_validity(in_image.pyramid, level)
        req_size = get_window_output_dimensions(in_image, region, height, width, length, zoom, level)
        out_size = safeguard_output_dimensions(headers.safe_mode, config.output_size_limit, *req_size)
        out_width, out_height = out_size

        affine = annotation_crop_affine_matrix(annots.region, region, out_width, out_height)

        return MaskResponse(
            in_image, annots, affine,
            out_width, out_height, 8, out_format
        ).http_response(
            mimetype,
            extra_headers=add_image_size_limit_header(dict(), *req_size, *out_size)
        )


@router.post('/image/{filepath:path}/annotation/crop{extension:path}', tags=api_tags)
async def show_crop(
    request: Request, response: Response,
    body: AnnotationCropRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageAnnotationRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    The crop is similar to an image window but where the transparency of the background can be
    adjusted.

    The input spatial region is given by the rectangular envelope of all geometries multiplied
    by an optional context factor. The target size is given by one of the scaling factors (
    `size`, `width`, `height`, `zoom` or `level`).

    By default, the background transparency is set to 100 which is also known as *alpha mask*.
    When the background transparency is set to 0, foreground and background cannot be
    distinguished.

    Annotation `fill_color`, `stroke_width` and `stroke_color` are ignored.
    """
    return await _show_crop(
        request, response,
        path, **body.model_dump(serialize_as_any=True),
        extension=extension, headers=headers, config=config
    )


async def _show_crop(
    request: Request, response: Response,
    path: Path,
    annotations,
    context_factor,
    background_transparency,
    height, width, length, zoom, level,
    channels, z_slices, timepoints,
    min_intensities, max_intensities, filters, gammas, threshold,
    bits, colorspace,
    extension, headers, config,
    colormaps=None, c_reduction=ChannelReduction.ADD, z_reduction=None, t_reduction=None,
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        annots = parse_annotations(
            ensure_list(annotations),
            ignore_fields=['stroke_width', 'stroke_color'],
            default={'fill_color': WHITE},
            origin=headers.annot_origin, im_height=in_image.height
        )

        region = get_annotation_region(in_image, annots, context_factor)

        annot_style = dict(
            mode=AnnotationStyleMode.CROP,
            background_transparency=background_transparency
        )

        return await _show_window(
            request, response,
            path, region,
            height, width, length, zoom, level,
            channels, z_slices, timepoints,
            min_intensities, max_intensities,
            filters, gammas, threshold, bits, colorspace,
            annots, annot_style,
            extension, headers, config,
            colormaps, c_reduction, z_reduction, t_reduction
        )


@router.post('/image/{filepath:path}/annotation/drawing{extension:path}', tags=api_tags)
async def show_drawing(
    request: Request, response: Response,
    body: AnnotationDrawingRequest,
    path: Path = Depends(imagepath_parameter),
    extension: OutputExtension = Depends(extension_path_parameter),
    headers: ImageAnnotationRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    """
    **`GET with body` - when a GET with URL encoded query parameters is not possible due to URL
    size limits, a POST with body content must be used.**

    Get an annotation crop (with apparent background) where annotations are drawn according to
    their respective `fill_color`, `stroke_width` and `stroke_color`.

    The input spatial region is given by the rectangular envelope of all geometries multiplied
    by an optional context factor. The target size is given by one of the scaling factors (
    `size`, `width`, `height`, `zoom` or `level`).
    """
    return await _show_drawing(
        request, response,
        path, **body.model_dump(serialize_as_any=True),
        extension=extension, headers=headers, config=config
    )


async def _show_drawing(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    annotations,
    context_factor,
    try_square, point_cross, point_envelope_length,
    height, width, length, zoom, level,
    channels, z_slices, timepoints,
    min_intensities, max_intensities, filters, gammas, threshold, log,
    extension, headers, config,
    colormaps=None, c_reduction=ChannelReduction.ADD, z_reduction=None, t_reduction=None,
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        annots = parse_annotations(
            ensure_list(annotations),
            ignore_fields=['fill_color'],
            default={'stroke_width': 1},
            point_envelope_length=point_envelope_length,
            origin=headers.annot_origin, im_height=in_image.height
        )

        region = get_annotation_region(in_image, annots, context_factor, try_square)

        annot_style = dict(
            mode=AnnotationStyleMode.DRAWING,
            point_cross=point_cross,
            point_envelope_length=point_envelope_length
        )

        return await _show_window(
            request, response,
            path, region,
            height, width, length, zoom, level,
            channels, z_slices, timepoints,
            min_intensities, max_intensities,
            filters, gammas, threshold, 8, Colorspace.AUTO,
            annots, annot_style,
            extension, headers, config,
            colormaps, c_reduction, z_reduction, t_reduction
        )


def show_spectra(filepath, body):
    pass


def show_footprint(filepath, body):
    pass

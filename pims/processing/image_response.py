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
from typing import Dict, List, Optional, Tuple, Type, Union

import numpy as np
from starlette.responses import Response

from pims.api.utils.mimetype import OutputExtension
from pims.api.utils.models import (
    AnnotationStyleMode, AssociatedName, ChannelReduction,
    Colorspace, GenericReduction, PointCross
)
from pims.files.file import Image
from pims.filters import AbstractFilter
from pims.processing.adapters import RawImagePixels
from pims.processing.annotations import ParsedAnnotations
from pims.processing.colormaps import (
    Colormap, StackedLookUpTables, combine_stacked_lut, default_lut, is_rgb_colormapping
)
from pims.processing.histograms.utils import change_colorspace_histogram, rescale_histogram
from pims.processing.masks import (
    draw_condition_mask, rasterize_draw, rasterize_mask, rescale_draw,
    transparency_mask
)
from pims.processing.pixels import ImagePixels
from pims.processing.region import Region, Tile
from pims.utils.dtypes import np_dtype
from pims.utils.math import max_intensity


class ImageResponse(ABC):
    """
    Base class for an image response.
    """

    def __init__(
        self, in_image: Optional[Image], out_format: OutputExtension,
        out_width: int, out_height: int, out_bitdepth: int = 8, **kwargs
    ):
        self.in_image = in_image

        self.out_width = out_width
        self.out_height = out_height
        self.out_format = out_format
        self.out_bitdepth = out_bitdepth
        self.out_format_params = {
            k.replace('out_format_', ''): v
            for k, v in kwargs.items() if k.startswith('out_format_')
        }

    @property
    def best_effort_bitdepth(self) -> int:
        """Depending on output format, asked bitdepth could be downgraded."""
        if self.out_format == OutputExtension.PNG:
            return min(self.out_bitdepth, 16)
        return min(self.out_bitdepth, 8)

    @property
    def max_intensity(self):
        return max_intensity(self.best_effort_bitdepth)

    @abstractmethod
    def process(self) -> ImagePixels:
        """
        Process the image pixels according to in/out parameters.
        """
        pass

    def get_response_buffer(self) -> bytes:
        """
        Get image response compressed using output extension compressor,
        in bytes.
        """
        return self.process().compress(
            self.out_format, self.best_effort_bitdepth,
            **self.out_format_params
        )

    def http_response(
        self, mimetype: str, extra_headers: Optional[Dict[str, str]] = None
    ) -> Response:
        """
        Encapsulate image response into an HTTP response, ready to be sent to
        the client.
        """
        return Response(
            content=self.get_response_buffer(),
            headers=extra_headers,
            media_type=mimetype
        )


class MultidimImageResponse(ImageResponse, ABC):
    """
    Base class for multidimensional image response.
    """

    def __init__(
        self, in_image: Image,
        in_channels: List[int], in_z_slices: List[int], in_timepoints: List[int],
        out_format: OutputExtension, out_width: int, out_height: int,
        out_bitdepth: int, c_reduction: ChannelReduction,
        z_reduction: GenericReduction, t_reduction: GenericReduction, **kwargs
    ):
        super().__init__(
            in_image, out_format, out_width, out_height, out_bitdepth, **kwargs
        )
        self.in_image = in_image
        self.channels = in_channels
        self.z_slices = in_z_slices
        self.timepoints = in_timepoints

        self.c_reduction = c_reduction
        self.z_reduction = z_reduction
        self.t_reduction = t_reduction

    def raw_view_planes(self) -> Tuple[List[int], int, int]:
        # PIMS API currently only allow requests for 1 Z or T plane
        return self.channels, self.z_slices[0], self.timepoints[0]

    @abstractmethod
    def raw_view(self, c: Union[int, List[int]], z: int, t: int) -> RawImagePixels:
        pass


class ProcessedView(MultidimImageResponse, ABC):
    """
    Base class for image responses with processing.
    """

    def __init__(
        self, in_image: Image,
        in_channels: List[int], in_z_slices: List[int], in_timepoints: List[int],
        out_format: OutputExtension, out_width: int, out_height: int,
        out_bitdepth: int, c_reduction: ChannelReduction,
        z_reduction: GenericReduction, t_reduction: GenericReduction,
        gammas: List[float], filters: List[Type[AbstractFilter]],
        colormaps: List[Colormap], min_intensities: List[int],
        max_intensities: List[int], log: bool, threshold: Optional[float],
        colorspace: Colorspace = Colorspace.AUTO, **kwargs
    ):
        super().__init__(
            in_image, in_channels, in_z_slices, in_timepoints,
            out_format, out_width, out_height, out_bitdepth,
            c_reduction, z_reduction, t_reduction, **kwargs
        )

        self.gammas = gammas
        self.filters = filters
        self.colormaps = colormaps
        self.min_intensities = min_intensities
        self.max_intensities = max_intensities
        self.log = log
        self.threshold = threshold
        self.colorspace = colorspace

    @property
    def gamma_processing(self) -> bool:
        """Whether gamma processing is required"""
        return any(gamma != 1.0 for gamma in self.gammas)

    @property
    def threshold_processing(self) -> bool:
        """Whether threshold processing is required"""
        return self.threshold is not None and self.threshold > 0.0

    @property
    def log_processing(self) -> bool:
        """Whether log processing is required"""
        return self.log

    @property
    def intensity_processing(self) -> bool:
        """Whether intensity processing is required"""
        return (any(self.min_intensities)
                or any(i != self.max_intensity for i in self.max_intensities))

    @property
    def math_processing(self) -> bool:
        """Whether math lookup table has to be computed."""
        return (self.intensity_processing or
                self.gamma_processing or
                self.threshold_processing or
                self.log_processing)

    def math_lut(self) -> Optional[StackedLookUpTables]:
        """
        Compute lookup table for math processing operations if any.

        Returns
        -------
        lut
            Stacked LUTs (n_channels, 2**img.bitdepth, 1)
        """
        if not self.math_processing:
            return None

        n_channels = len(self.channels)
        lut = np.zeros((n_channels, self.in_image.max_value + 1, 1))
        if self.intensity_processing:
            for c in range(n_channels):
                mini = self.min_intensities[c]
                maxi = self.max_intensities[c]
                diff = maxi - mini
                lut[c, mini:maxi] = np.linspace((0,), (1,), num=diff)
                lut[c, maxi:] = 1
        else:
            lut[:, :, 0] = np.linspace(
                (0,) * n_channels, (1,) * n_channels,
                num=self.in_image.max_value + 1
            ).T

        if self.gamma_processing:
            gammas = np.array(self.gammas)[:, np.newaxis, np.newaxis]
            lut = np.power(lut, gammas)

        if self.log_processing:
            # Apply logarithmic scale on image.
            # Formula: out = ln(1+ in) * max_per_channel / ln(1 + max_per_channel)
            # Reference: Icy Logarithmic 2D viewer plugin
            # (http://icy.bioimageanalysis.org/plugin/logarithmic-2d-viewer/)
            lut = np.log1p(lut) * 1. / np.log1p(1)

        if self.threshold_processing:
            lut[lut < self.threshold] = 0.0

        lut *= self.max_intensity
        lut = np.rint(lut)
        return lut.astype(np_dtype(self.best_effort_bitdepth))

    @property
    def is_rgb(self):
        if any(self.colormaps):
            return is_rgb_colormapping(self.colormaps)
        return False

    @property
    def colormap_processing(self) -> bool:
        """Whether colormapping processing is required."""
        if any(self.colormaps):
            n = len(self.colormaps)
            return not (2 <= n <= 3
                        and len(self.channels) == n
                        and self.channels == [0, 1, 2]
                        and is_rgb_colormapping(self.colormaps))
        return False

    def colormap_lut(self) -> Optional[StackedLookUpTables]:
        """
        Compute lookup table from colormaps if any.

        Returns
        -------
        lut
            Array of shape (n_channels, 2**img.bitdepth, n_components)
        """
        if not self.colormap_processing:
            return None

        n_components = np.max(
            [colormap.n_components() if colormap else 1
             for colormap in self.colormaps]
        )
        return np.stack(
            [
                colormap.lut(
                    size=self.max_intensity + 1,
                    bitdepth=self.best_effort_bitdepth,
                    n_components=n_components,
                    force_black_as_first=self.threshold_processing
                ) if colormap else default_lut(
                    size=self.max_intensity + 1,
                    bitdepth=self.best_effort_bitdepth,
                    n_components=n_components,
                    force_black_as_first=self.threshold_processing
                ) for colormap in self.colormaps
            ]
        )

    def lut(self) -> Optional[StackedLookUpTables]:
        """
        The lookup table to apply combining all processing operations.
        """
        math_lut = self.math_lut()
        colormap_lut = self.colormap_lut()

        if math_lut is None:
            if colormap_lut is None:
                return None
            else:
                return colormap_lut
        else:
            if colormap_lut is None:
                return math_lut
            else:
                return combine_stacked_lut(math_lut, colormap_lut)

    # Colorspace

    @property
    def new_colorspace(self) -> Colorspace:
        """
        The colorspace for the image response if colorspace processing is
        required.
        """
        if self.colorspace == Colorspace.AUTO:
            if len(self.channels) == 1:
                colorspace = Colorspace.GRAY
            else:
                colorspace = Colorspace.COLOR
            return colorspace
        return self.colorspace

    @property
    def colorspace_processing(self) -> bool:
        """Whether colorspace needs to be changed."""
        if self.colorspace == Colorspace.AUTO:
            return False
        return (self.colormap_processing or
                (self.colorspace == Colorspace.GRAY and
                 len(self.channels) > 1) or
                (self.colorspace == Colorspace.COLOR and
                 len(self.channels) == 1))

    # Filtering

    @property
    def filter_processing(self) -> bool:
        """Whether filters have to be applied."""
        return bool(len(self.filters))

    @property
    def filter_processing_histogram(self) -> bool:
        """If filtering, whether some filters require histograms."""
        return any([f.require_histogram() for f in self.filters])

    @property
    def filter_required_colorspace(self) -> Optional[Colorspace]:
        """
        If filtering and some filters require a specific colorspace, get the
        minimum satisfying colorspace.
        """
        colorspaces = [f.required_colorspace() for f in self.filters]
        if Colorspace.GRAY in colorspaces:
            return Colorspace.GRAY
        if Colorspace.COLOR in colorspaces:
            return Colorspace.COLOR
        return None

    @property
    def filter_colorspace_processing(self):
        """Whether colorspace needs to be changed before applying filters"""
        if self.filter_required_colorspace is None:
            return False
        return (self.filter_required_colorspace == Colorspace.GRAY and
                len(self.channels) > 1) or \
               (self.filter_required_colorspace == Colorspace.COLOR and
                len(self.channels) == 1)

    @property
    def filter_colorspace(self):
        """If needed, the required colorspace before applying filters"""
        if self.filter_required_colorspace is None:
            return self.colorspace
        return self.filter_required_colorspace

    def process(self) -> ImagePixels:
        pixels = ImagePixels(self.raw_view(*self.raw_view_planes()))

        if self.c_reduction != ChannelReduction.ADD:
            lut = self.math_lut()
            if lut is not None:
                pixels.apply_lut_stack(lut, self.c_reduction, self.is_rgb)
            else:
                pixels.channel_reduction(self.c_reduction)

            # TODO: What about colormap ??
            # lut = self.colormap_lut()
            # if lut is not None:
            #     pixels.apply_lut_stack(lut)
        else:
            lut = self.lut()
            if lut is not None:
                pixels.apply_lut_stack(lut, self.c_reduction, self.is_rgb)

        pixels.resize(self.out_width, self.out_height)

        if self.filter_processing:
            if self.filter_colorspace is not None:
                pixels.change_colorspace(self.filter_colorspace)

            filter_params = dict()
            if self.filter_processing_histogram:
                filter_params['histogram'] = self.process_histogram()
            for filter_op in self.filters:
                pixels.apply_filter(filter_op(**filter_params))

        if self.colorspace_processing:
            pixels.change_colorspace(self.new_colorspace)

        if self.threshold_processing:
            dtype = np_dtype(self.out_bitdepth)
            mask = transparency_mask(pixels.np_array(), 100, dtype)  # noqa ?
            pixels.add_transparency(mask)

        return pixels

    def process_histogram(self) -> np.ndarray:
        """
        Process image histogram from in/out parameters, so that if can be
        used by histogram filters on processed images.
        """
        hist = self.in_image.histogram.plane_histogram(*self.raw_view_planes())
        hist = np.atleast_2d(hist)

        # TODO: filters are computed on best_effort bitdepth
        #  while it should do on image bitdepth
        hist = rescale_histogram(hist, self.best_effort_bitdepth)

        if self.filter_colorspace_processing:
            hist = change_colorspace_histogram(hist, self.filter_colorspace)

        return hist.squeeze()


class ThumbnailResponse(ProcessedView):
    def __init__(
        self, in_image: Image, in_channels: List[int], in_z_slices: List[int],
        in_timepoints: List[int], out_format: OutputExtension, out_width: int,
        out_height: int, c_reduction: ChannelReduction, z_reduction: GenericReduction,
        t_reduction: GenericReduction, gammas: List[float],
        filters: List[Type[AbstractFilter]], colormaps: List[Colormap],
        min_intensities: List[int], max_intensities: List[int], log: bool,
        use_precomputed: bool, threshold: Optional[float], **kwargs
    ):
        super().__init__(
            in_image, in_channels, in_z_slices, in_timepoints, out_format,
            out_width, out_height, 8, c_reduction, z_reduction, t_reduction,
            gammas, filters, colormaps, min_intensities, max_intensities, log,
            threshold, **kwargs
        )

        self.use_precomputed = use_precomputed

    def raw_view(self, c: Union[int, List[int]], z: int, t: int) -> RawImagePixels:
        return self.in_image.thumbnail(
            self.out_width, self.out_height, c=c, z=z, t=t,
            precomputed=self.use_precomputed
        )


class ResizedResponse(ProcessedView):
    def __init__(
        self, in_image: Image, in_channels: List[int], in_z_slices: List[int],
        in_timepoints: List[int], out_format: OutputExtension, out_width: int,
        out_height: int, c_reduction: ChannelReduction,
        z_reduction: GenericReduction, t_reduction: GenericReduction,
        gammas: List[float], filters: List[Type[AbstractFilter]],
        colormaps: List[Colormap], min_intensities: List[int],
        max_intensities: List[int], log: bool, out_bitdepth: int,
        threshold: Optional[float], colorspace: Colorspace, **kwargs
    ):
        super().__init__(
            in_image, in_channels, in_z_slices, in_timepoints, out_format,
            out_width, out_height, out_bitdepth, c_reduction, z_reduction,
            t_reduction, gammas, filters, colormaps, min_intensities,
            max_intensities, log, threshold, colorspace, **kwargs
        )

    def raw_view(self, c: Union[int, List[int]], z: int, t: int) -> RawImagePixels:
        return self.in_image.thumbnail(
            self.out_width, self.out_height, c=c, z=z, t=t, precomputed=False
        )


class WindowResponse(ProcessedView):
    def __init__(
        self, in_image: Image, in_channels: List[int], in_z_slices: List[int],
        in_timepoints: List[int], region: Region, out_format: OutputExtension,
        out_width: int, out_height: int, c_reduction: ChannelReduction,
        z_reduction: GenericReduction, t_reduction: GenericReduction,
        gammas: List[float], filters: List[Type[AbstractFilter]],
        colormaps: List[Colormap], min_intensities: List[int],
        max_intensities: List[int], log: bool, out_bitdepth: int,
        threshold: Optional[float], colorspace: Colorspace,
        annotations: Optional[ParsedAnnotations] = None,
        affine_matrix: Optional[np.ndarray] = None,
        annot_params: Optional[dict] = None, **kwargs
    ):
        super().__init__(
            in_image, in_channels, in_z_slices, in_timepoints, out_format,
            out_width, out_height, out_bitdepth, c_reduction, z_reduction,
            t_reduction, gammas, filters, colormaps, min_intensities,
            max_intensities, log, threshold, colorspace, **kwargs
        )

        self.region = region

        annot_params = annot_params if annot_params else dict()
        self.annotation_mode = annot_params.get('mode')
        self.annotations = annotations
        self.affine_matrix = affine_matrix
        self.background_transparency = annot_params.get('background_transparency')
        self.point_style = annot_params.get('point_cross')

    @property
    def colorspace_processing(self) -> bool:
        if (self.colorspace == Colorspace.AUTO
                and self.annotation_mode == AnnotationStyleMode.DRAWING
                and len(self.channels) == 1
                and not self.annotations.is_stroke_grayscale):
            return True
        return super(WindowResponse, self).colorspace_processing

    @property
    def new_colorspace(self) -> Colorspace:
        if (self.colorspace == Colorspace.AUTO
                and self.annotation_mode == AnnotationStyleMode.DRAWING
                and len(self.channels) == 1
                and not self.annotations.is_stroke_grayscale):
            return Colorspace.COLOR
        return super(WindowResponse, self).new_colorspace

    def process(self) -> ImagePixels:
        pixels = super(WindowResponse, self).process()

        if self.annotations and self.affine_matrix is not None:
            if self.annotation_mode == AnnotationStyleMode.CROP:
                mask = rasterize_mask(
                    self.annotations, self.affine_matrix,
                    self.out_width, self.out_height
                )
                mask = transparency_mask(
                    mask, self.background_transparency,
                    np_dtype(self.out_bitdepth)  # noqa
                )
                pixels.add_transparency(mask)
            elif self.annotation_mode == AnnotationStyleMode.DRAWING:
                draw, draw_background = rasterize_draw(
                    self.annotations, self.affine_matrix, self.out_width,
                    self.out_height, self.point_style
                )
                cond = draw_condition_mask(draw, draw_background)

                if self.colorspace_processing:
                    draw = ImagePixels(draw)
                    draw.change_colorspace(self.new_colorspace)
                    draw = draw.np_array()  # TODO

                pixels.draw_on(
                    rescale_draw(draw, np_dtype(self.out_bitdepth)),  # noqa
                    cond
                )
        return pixels

    def raw_view(self, c: Union[int, List[int]], z: int, t: int) -> RawImagePixels:
        return self.in_image.window(
            self.region, self.out_width, self.out_height, c=c, z=z, t=t
        )


class TileResponse(ProcessedView):
    def __init__(
        self, in_image: Image, in_channels: List[int], in_z_slices: List[int],
        in_timepoints: List[int], tile_region: Tile, out_format: OutputExtension,
        out_width: int, out_height: int, c_reduction: ChannelReduction,
        z_reduction: GenericReduction, t_reduction: GenericReduction,
        gammas: List[float], filters: List[Type[AbstractFilter]],
        colormaps: List[Colormap], min_intensities: List[int],
        max_intensities: List[int], log: bool, threshold: Optional[float],
        **kwargs
    ):
        super().__init__(
            in_image, in_channels, in_z_slices, in_timepoints, out_format,
            out_width, out_height, 8, c_reduction, z_reduction, t_reduction,
            gammas, filters, colormaps, min_intensities, max_intensities,
            log, threshold, **kwargs
        )

        # Tile (region)
        self.tile_region = tile_region

    def raw_view(self, c: Union[int, List[int]], z: int, t: int) -> RawImagePixels:
        return self.in_image.tile(self.tile_region, c=c, z=z, t=t)


class AssociatedResponse(ImageResponse):
    def __init__(
        self, in_image: Image, associated_key: AssociatedName, out_width: int,
        out_height: int, out_format: OutputExtension, **kwargs
    ):
        super().__init__(in_image, out_format, out_width, out_height, **kwargs)
        self.associated_key = associated_key

    def associated_image(self) -> RawImagePixels:
        if self.associated_key == AssociatedName.macro:
            associated = self.in_image.macro(self.out_width, self.out_height)
        elif self.associated_key == AssociatedName.label:
            associated = self.in_image.label(self.out_width, self.out_height)
        else:
            associated = self.in_image.thumbnail(
                self.out_width, self.out_height, precomputed=True
            )

        return associated

    def process(self) -> ImagePixels:
        pixels = ImagePixels(self.associated_image())
        pixels.resize(self.out_width, self.out_height)
        return pixels


class MaskResponse(ImageResponse):
    def __init__(
        self, in_image: Image, annotations: ParsedAnnotations,
        affine_matrix: np.ndarray, out_width: int, out_height: int,
        out_bitdepth: int, out_format: OutputExtension, **kwargs
    ):
        super().__init__(
            in_image, out_format, out_width, out_height,
            out_bitdepth, **kwargs
        )

        self.annotations = annotations
        self.affine_matrix = affine_matrix

    def process(self) -> ImagePixels:
        return ImagePixels(
            rasterize_mask(
                self.annotations, self.affine_matrix,
                self.out_width, self.out_height
            )
        )


class DrawingResponse(MaskResponse):
    def __init__(
        self, in_image: Image, annotations: ParsedAnnotations,
        affine_matrix: np.ndarray, point_style: PointCross,
        out_width: int, out_height: int, out_bitdepth: int,
        out_format: OutputExtension, **kwargs
    ):
        super().__init__(
            in_image, annotations, affine_matrix, out_width,
            out_height, out_bitdepth, out_format, **kwargs
        )

        self.point_style = point_style

    def process(self) -> ImagePixels:
        draw, _ = rasterize_draw(
            self.annotations, self.affine_matrix, self.out_width,
            self.out_height, self.point_style
        )
        return ImagePixels(draw)


class ColormapRepresentationResponse(ImageResponse):
    def __init__(
        self, colormap: Colormap, out_width: int, out_height: int,
        out_format: OutputExtension, **kwargs
    ):
        super().__init__(None, out_format, out_width, out_height, **kwargs)
        self.colormap = colormap

    def process(self) -> ImagePixels:
        return ImagePixels(
            self.colormap.as_image(self.out_width, self.out_height)
        )

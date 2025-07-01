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

from enum import Enum
from typing import List, Optional, Union

from fastapi import Query
from pydantic import BaseModel, Field, RootModel

from pims.utils.color import Color
from typing_extensions import Annotated


class BaseDependency:
    def dict(self):
        return vars(self)


class CollectionSize(BaseModel):
    size: int = Field(..., description='The collection size')


class FormatId(RootModel):
    root: str = Field(..., description='Unique format identifier', examples=['VMS'])


class ZoomOrLevel(RootModel):
    root: Annotated[int, Field(ge=0)] = Field(..., examples=[0])


class SingleOrRangeChannelIndex(RootModel):
    """
    A single channel index or a range of indexes. **By default**, all channels are considered.
    """
    root: Union[Annotated[int, Field(ge=0)], str]  # TODO: replace str by range pydantic model


class SingleZSliceIndex(RootModel):
    """
    A single focal plane index. **By default**, the median focal plane is considered.
    """
    root: Annotated[int, Field(ge=0)]


class SingleTimepointIndex(RootModel):
    """
    A single timepoint index. **By default**, the first timepoint considered.
    """
    root: Annotated[int, Field(ge=0)]


class ChannelReduction(str, Enum):
    """
    Reduction function used to merge selected channels.
    """

    ADD = 'ADD'
    MIN = 'MIN'
    MED = 'MED'
    MAX = 'MAX'


class GenericReduction(str, Enum):
    MIN = 'MIN'
    MED = 'MED'
    MAX = 'MAX'


class PlaneSelectionQueryParams(BaseDependency):
    def __init__(
        self,
        channels: Optional[List[Union[str, Annotated[int, Field(ge=0)]]]] = Query(None),
        # TODO: replace str by range pydantic model
        z_slices: Optional[Annotated[int, Field(ge=0)]] = Query(None),
        timepoints: Optional[Annotated[int, Field(ge=0)]] = Query(None),
        c_reduction: ChannelReduction = Query(ChannelReduction.ADD)
    ):
        self.channels = channels
        self.z_slices = z_slices
        self.timepoints = timepoints
        self.c_reduction = c_reduction


class IntensitySelectionEnum(str, Enum):
    AUTO_IMAGE = 'AUTO_IMAGE'
    AUTO_PLANE = 'AUTO_PLANE'
    STRETCH_IMAGE = 'STRETCH_IMAGE'
    STRETCH_PLANE = 'STRETCH_PLANE'


class IntensitySelection(RootModel):
    root: Union[
        IntensitySelectionEnum,
        Annotated[int, Field(ge=0)],
        List[Union[IntensitySelectionEnum, Annotated[int, Field(ge=0)]]],
    ]


class Gamma(RootModel):
    root: Annotated[float, Field(ge=0.0, le=10.0)]


class GammaList(RootModel):
    """
    Gamma performs a non-linear histogram adjustment.
    Pixel intensities in the original image are raised
    to the power of the gamma value.

    If `gamma < 1`, faint objects become more intense
    while bright objects do not.

    If `gamma > 1`, medium-intensity objects become fainter
    while bright objects do not.
    """
    root: Union[Gamma, List[Gamma]]


class Threshold(RootModel):
    """
    Threshold an image so that pixel intensities in the
    original image below the threshold (scaled to pixel type)
    are set to 0. If the target content type is an image
    format supporting transparency, pixel intensities below
    the threshold are transparent.
    """
    root: Optional[Annotated[float, Field(ge=0.0, le=1.0)]] = None


class FilterId(RootModel):
    """
    A unique case-insensitive identifier for an image filter
    """
    root: str = Field(
        ..., examples=['OTSU']
    )


class FilterIdList(RootModel):
    """
    An image filter is used to change the appearance of an image
    and helps at understanding the source image.

    Valid filter names can be found with the endpoint `/filters`.
    """
    root: Union[FilterId, List[FilterId]]


class FilterType(str, Enum):
    """
    * `LOCAL` - The image filter uses the neighborhood of every pixels.
    * `GLOBAL` - The image filter uses histogram-derived techniques.
    * `PIXEL` - The image filter works pixel per pixel.

    """
    LOCAL = 'LOCAL'
    GLOBAL = 'GLOBAL'
    PIXEL = 'PIXEL'


class ColormapEnum(str, Enum):
    """
    * `NONE` - No colormap is applied, all channels are grayscale.
    * `DEFAULT` - The colormap(s) determined by image metadata and/or format.
    """
    NONE = 'NONE'
    DEFAULT = 'DEFAULT'
    DEFAULT_INVERTED = '!DEFAULT'


class ColormapId(RootModel):
    """
    A unique case-insensitive identifier for a colormap.
    Pre-defined colormap names can be found with the endpoint `/colormaps`.
    CSS named and hexadecimal colors are valid colormap names (monotonic linear colormap).
    Colormaps can be inverted by prepending the colormap name with `!`.
    """
    root: Union[str, ColormapEnum] = Field(
        ..., examples=['JET', '!#f00', 'red']
    )


class ColormapIdList(RootModel):
    """
    A colormap is a function that maps the colors of the original image (source)
    to the colors of the response image. The usage of colormap produces
    a false-color representation of the original image and helps at
    understanding the image.

    Valid colormap names can be found with the endpoint `/colormaps`.
    """
    root: Union[ColormapId, List[ColormapId]]


class ExistingColormapId(RootModel):
    """
    A unique case-insensitive identifier for a colormap.
    Pre-defined colormap names can be found with the endpoint `/colormaps`.
    CSS named and hexadecimal colors are valid colormap names (monotonic linear colormap).
    Colormaps can be inverted by prepending the colormap name with `!`.
    """
    root: str = Field(
        ..., examples=['JET', '!#f00', 'red']
    )


class ImageIn(BaseModel):
    channels: Optional[Union[SingleOrRangeChannelIndex, List[SingleOrRangeChannelIndex]]] = None
    z_slices: Optional[SingleZSliceIndex] = None
    timepoints: Optional[SingleTimepointIndex] = None
    c_reduction: Optional[ChannelReduction] = ChannelReduction.ADD
    min_intensities: Optional[IntensitySelection] = None
    max_intensities: Optional[IntensitySelection] = None
    colormaps: Optional[ColormapIdList] = ColormapEnum.DEFAULT
    filters: Optional[FilterIdList] = None
    gammas: GammaList = 1.0
    threshold: Threshold = None
    # TODO[pydantic]: The following keys were removed: `fields`.
    # Check https://docs.pydantic.dev/dev-v2/migration/#changes-to-config for more information.
    # model_config = ConfigDict(__mi_doc="Intensity in the original image used as minimum intensity (black) to create the response." \
    #                "As a consequence, original image intensities lower than this value will be black in the response." \
    #                "\n\n" \
    #                "Maximum allowed value depends on image pixel type and is equal to `2 * pow(pixel type)`.\n" \
    #                "Minimum intensity is closely related to the concepts of brightness and contrast." \
    #                "\n\n" \
    #                "Brightness is the visual perception of reflected light while contrast is the separation of the " \
    #                "lightest and darkest parts of an image. A minimum intensity increase leads to:\n" \
    #                "* a brightness decrease, which refers to an image's decreased luminance.\n" \
    #                "* a contrast increase, which darken shadows and lighten highlights.\n\n" \
    #                "Enumeration supported values:\n" \
    #                "* `AUTO_IMAGE` - If image pixel type uses 8 bits, `min_intensity=0`. " \
    #                "Otherwise, the behavior is `STRETCH_IMAGE`.\n" \
    #                "* `AUTO_PLANE` - If image pixel type uses 8 bits, `min_intensity=0`. " \
    #                "Otherwise, the behavior is `STRETCH_PLANE`.\n" \
    #                "* `STRETCH_IMAGE` - Set `min_intensity` to lowest intensity in the original image, " \
    #                "for each channel.\n" \
    #                "* `STRETCH_PLANE` - Set `min_intensity` to lowest intensity in the set of planes, for each channel.",
    #     __ma_doc = "Intensity in the original image used as maximum intensity (white) to create the response." \
    #                "As a consequence, original image intensities greater than this value will be white in the response." \
    #                "\n\n" \
    #                "Maximum allowed value depends on image pixel type and is equal to `2 * pow(pixel type)`.\n" \
    #                "Maximum intensity is closely related to the concepts of brightness and contrast." \
    #                "\n\n" \
    #                "Brightness is the visual perception of reflected light while contrast is the separation of the " \
    #                "lightest and darkest parts of an image. A maximum intensity increase leads to:\n" \
    #                "* a brightness decrease, which refers to an image's decreased luminance.\n" \
    #                "* a contrast decrease, which darken highlights and lighten shadows.\n\n" \
    #                "Enumeration supported values:\n" \
    #                "* `AUTO_IMAGE` - If image pixel type uses 8 bits, `max_intensity=255`. " \
    #                "Otherwise, the behavior is `STRETCH_IMAGE`.\n" \
    #                "* `AUTO_PLANE` - If image pixel type uses 8 bits, `max_intensity=255`. " \
    #                "Otherwise, the behavior is `STRETCH_PLANE`.\n" \
    #                "* `STRETCH_IMAGE` - Set `min_intensity` to greatest intensity in the original image, " \
    #                "for each channel.\n" \
    #                "* `STRETCH_PLANE` - Set `min_intensity` to greatest intensity in the set of planes, " \
    #                "for each channel.",
    #     #fields={
    #     # "min_intensities": {
    #     #     "description": __mi_doc
    #     # },
    #     # "max_intensities": {
    #     #     "description": __ma_doc
    #     # }
    #     # }
    # )


class ImageOut(BaseModel):
    height: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = None
    width: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = None
    length: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = None
    # TODO[pydantic]: The following keys were removed: `fields`.
    # Check https://docs.pydantic.dev/dev-v2/migration/#changes-to-config for more information.
    # model_config = ConfigDict(fields={
    #     "height": {
    #         "description": "Height of the thumbnail. Width is adjusted to preserve the "
    #                        "aspect ratio. "
    #                        "**Takes precedence over `width` and `length`.**"
    #     },
    #     "width": {
    #         "description": "Width of the thumbnail. Height is adjusted to preserve the "
    #                        "aspect ratio. "
    #                        "**Takes precedence over `length`.**"
    #     },
    #     "length": {
    #         "description": "Length of the largest side of the thumbnail. "
    #                        "The other dimension is adjusted to preserve the aspect ratio. "
    #                        "**Ignored if other size-related parameter such as `width` or "
    #                        "`height` is present.** "
    #     }
    # })


class ImageInDisplay(ImageIn):
    min_intensities: Optional[IntensitySelection] = IntensitySelectionEnum.AUTO_IMAGE
    max_intensities: Optional[IntensitySelection] = IntensitySelectionEnum.AUTO_IMAGE
    log: bool = Field(
        False,
        description='Apply a logarithmic scale on image data to ease observation '
                    'of high dynamic range images such as 16-bit images.',
    )


class ImageOpsDisplayQueryParams(BaseDependency):
    def __init__(
        self,
        gammas: Optional[List[Annotated[float, Field(ge=0.0, le=10.0)]]] = Query([1.0]),
        threshold: Optional[Annotated[float, Field(ge=0.0, le=1.0)]] = Query(None),
        min_intensities: Optional[List[Union[IntensitySelectionEnum, Annotated[int, Field(ge=0)]]]] = Query(
            [
                IntensitySelectionEnum.AUTO_IMAGE]
        ),
        max_intensities: Optional[List[Union[IntensitySelectionEnum, Annotated[int, Field(ge=0)]]]] = Query(
            [
                IntensitySelectionEnum.AUTO_IMAGE]
        ),
        colormaps: Optional[List[Union[str, ColormapEnum]]] = Query([ColormapEnum.DEFAULT]),
        filters: Optional[List[str]] = Query(None),
        log: bool = Query(False),
    ):
        self.gammas = gammas
        self.threshold = threshold
        self.min_intensities = min_intensities
        self.max_intensities = max_intensities
        self.colormaps = colormaps
        self.filters = filters
        self.log = log


class ImageOutDisplay(ImageOut):
    length: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = 256


class ImageOutDisplayQueryParams(BaseDependency):
    def __init__(
        self,
        height: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = Query(None),
        width: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = Query(None),
        length: Optional[Union[Annotated[int, Field(gt=1)], Annotated[float, Field(gt=0.0, le=1.0)]]] = Query(256)
    ):
        self.length = length
        self.width = width
        self.height = height


class BitDepthEnum(str, Enum):
    AUTO = 'AUTO'


class BitDepth(RootModel):
    """
    The target bit depth. It is a best-effort parameter as supported bit depths
    depend on target content type.
    * `AUTO` - Try to set target bit depth equal to the source bit depth.
    """
    root: Union[BitDepthEnum, int] = Field(BitDepthEnum.AUTO)


class Colorspace(str, Enum):
    """
    The target colorspace. It is a best-effort parameter as supported
    colorspace depend on target content type.

    * `GRAY` - Return the target in grayscale, using luminance if it was
    initially a color image.
    * `COLOR` - Return the target in RGB colors.
    * `AUTO` - Automatically returns the target in the best colorspace.

    If the source is grayscale and image manipulation do not use colors,
    it returns the target in grayscale. Otherwise, target is in RGB colors.
    """
    GRAY = 'GRAY'
    COLOR = 'COLOR'
    AUTO = 'AUTO'


class ImageInProcessing(ImageIn):
    bits: BitDepth = BitDepthEnum.AUTO
    colorspace: Colorspace = Colorspace.AUTO


class ImageOpsProcessingQueryParams(BaseDependency):
    def __init__(
        self,
        gammas: Optional[List[Annotated[float, Field(ge=0.0, le=10.0)]]] = Query([1.0]),
        threshold: Optional[Annotated[float, Field(ge=0.0, le=1.0)]] = Query(None),
        min_intensities: Optional[List[Union[IntensitySelectionEnum, Annotated[int, Field(ge=0)]]]] = Query(None),
        max_intensities: Optional[List[Union[IntensitySelectionEnum, Annotated[int, Field(ge=0)]]]] = Query(None),
        colormaps: Optional[List[Union[str, ColormapEnum]]] = Query([ColormapEnum.DEFAULT]),
        filters: Optional[List[str]] = Query(None),
        bits: Optional[Union[BitDepthEnum, int]] = Query(BitDepthEnum.AUTO),
        colorspace: Optional[Colorspace] = Query(Colorspace.AUTO)
    ):
        self.gammas = gammas
        self.threshold = threshold
        self.min_intensities = min_intensities
        self.max_intensities = max_intensities
        self.colormaps = colormaps
        self.filters = filters
        self.bits = bits
        self.colorspace = colorspace


class ImageOutProcessing(ImageOut):
    zoom: Optional[Annotated[int, Field(ge=0)]] = None
    level: Optional[Annotated[int, Field(ge=0)]] = None
    # TODO[pydantic]: The following keys were removed: `fields`.
    # Check https://docs.pydantic.dev/dev-v2/migration/#changes-to-config for more information.
    # model_config = ConfigDict(fields={
    #     "zoom": {
    #         "description": "The zoom level to consider as thumbnail. Zoom 0 has the worst "
    #                        "resolution (smallest image, top of the image pyramid). Maximum "
    #                        "admissible zoom level depends on image. "
    #                        "**Takes precedence over `height`, `width` and `length`.**"
    #     },
    #     "level": {
    #         "description": "The tier level. Level 0 has the best resolution (largest image, "
    #                        "basis of the image pyramid). Maximum admissible tier level "
    #                        "depends on image. "
    #                        "**Takes precedence over `zoom`, `height`, `width` and `length`.**"
    #     }
    # })


class ImageOutProcessingQueryParams(BaseDependency):
    def __init__(
        self,
        zoom: Optional[Annotated[int, Field(ge=0)]] = Query(None),
        level: Optional[Annotated[int, Field(ge=0)]] = Query(None)
    ):
        self.zoom = zoom
        self.level = level


class ResizedRequest(ImageInProcessing, ImageOutProcessing):
    pass


class ThumbnailRequest(ImageInDisplay, ImageOutDisplay):
    use_precomputed: bool = Field(
        True,
        description="Whether the precomputed thumbnail (associated file) "
                    "has to be used if it is available, or not.",
    )


class TierIndexType(str, Enum):
    """
    How to interpret `reference_tier_index`, either as a zoom index,
    either as a level index.
    """
    LEVEL = 'LEVEL'
    ZOOM = 'ZOOM'


class ReferenceTierIndex(BaseModel):
    reference_tier_index: Optional[int] = Field(
        None,
        description="The index (zoom or level) denoting the tier "
                    "to use as reference coordinate system "
                    "for the input region. By default, best resolution tier "
                    "(maximum zoom/lowest level) is used.",
    )
    tier_index_type: TierIndexType = Field(
        TierIndexType.LEVEL,
        description='How to interpret `reference_tier_index`, '
                    'either as a zoom index, either as a level index.',
    )


class RegionCoordinate(RootModel):
    """
    A coordinate in pixels in the reference tier.
    """
    root: Annotated[int, Field(ge=0)] = Field(..., examples=[256])


class RegionLength(RootModel):
    """
    A length in pixels in the reference tier.
    """
    root: Annotated[int, Field(ge=1)] = Field(..., examples=[256])


class WindowRegion(ReferenceTierIndex):
    """
    The input region in the reference tier.
    """

    top: RegionCoordinate
    left: RegionCoordinate
    width: RegionLength
    height: RegionLength


class TileIndex(RootModel):
    """
    The tile index is the position of the tile in the given image pyramid tier.
    It is computed as `n * n_x_tiles + m` where
    * `n_x_tiles` is the number of tiles along the horizontal axis at given tier.
    * `m` is the tile position along the horizontal axis at given tier (0 is left).
    * `n` is the tile position along the vertical axis at given tier (0 is top).
    """
    root: Annotated[int, Field(ge=0)] = Field(...)


class TileX(RootModel):
    """
    The tile position along the horizontal axis at given tier (0 is left).
    """
    root: Annotated[int, Field(ge=0)] = Field(...)


class TileY(RootModel):
    """
    The tile position along the vertical axis at given tier (0 is top).
    """
    root: Annotated[int, Field(ge=0)] = Field(...)


class WindowTileCoord(ReferenceTierIndex):
    tx: TileX
    ty: TileY


class WindowTileIndex(ReferenceTierIndex):
    ti: TileIndex


class Annotation(BaseModel):
    geometry: str = Field(
        ...,
        description='A geometry described in Well-known text (WKT)',
        examples=['POINT(10 10)'],
    )
    fill_color: Optional[Color] = Field(
        Color("white"),
        description='A color to fill the annotation',
        examples=['#FF00FF']
    )
    stroke_color: Optional[Color] = Field(
        None,
        description='A color for the annotation stroke'
    )
    stroke_width: Optional[Annotated[int, Field(ge=0, le=10)]] = Field(
        0,
        description='A width for the annotation stroke'
    )


class Annotations(RootModel):
    root: Union[List[Annotation], Annotation]


class PointCross(str, Enum):
    """
    Point geometries have no area. Possible representation as a drawing are:
    * `CROSS` - A regular cross, hiding point location
    * `CROSSHAIR` - A cross but whose intersection is removed to show the point location
    * `CIRCLE` - A circle around the point location
    This parameter has no effect on non-point geometries.
    """
    CROSS = 'CROSS'
    CROSSHAIR = 'CROSSHAIR'
    CIRCLE = 'CIRCLE'


class PointEnvelopeLength(RootModel):
    """
    Point geometries have no area. An envelope must be specified to 
    extract some image data.
    This envelope is a square of length given by this parameter, 
    whose the center is the given point.
    
    This parameter has no effect on non-point geometries.
    """
    root: int = 100


class AnnotationBgTransparency(RootModel):
    """
    The background transparency. 100 means transparent background. 
    When transparency is used, the target content type must be 
    an image format supporting transparency.
    """
    root: Annotated[int, Field(ge=0, le=100)] = 0


class AnnotationStyleMode(str, Enum):
    CROP = 'CROP'
    MASK = 'MASK'
    DRAWING = 'DRAWING'


class AnnotationStyle(BaseModel):
    mode: AnnotationStyleMode
    point_envelope_length: Optional[PointEnvelopeLength] = None
    point_cross: Optional[PointCross] = PointCross.CROSS
    background_transparency: Optional[AnnotationBgTransparency] = None


class WindowRequest(ImageInProcessing, ImageOutProcessing):
    region: Union[WindowRegion, WindowTileIndex, WindowTileCoord]
    annotations: Optional[Annotations] = None
    annotation_style: Optional[AnnotationStyle] = None


class AnnotationContextFactor(RootModel):
    """
    The context factor is the number by which the spatial region 
    is given by the rectangular envelope of all geometries is multiplied.
    """
    root: float = 1


class AnnotationTrySquare(RootModel):
    """
    Try to adapt other parameters (such as input spatial region,
    context factor, target size) to return a square image.
    """
    root: bool = False


class AnnotationCropRequest(ImageInProcessing, ImageOutProcessing):
    annotations: Annotations
    context_factor: Optional[AnnotationContextFactor] = 1.0
    background_transparency: Optional[AnnotationBgTransparency] = 0


class AnnotationMaskRequest(ImageOutProcessing):
    annotations: Annotations
    context_factor: Optional[AnnotationContextFactor] = 1.0


class AnnotationDrawingRequest(ImageInDisplay, ImageOutProcessing):
    annotations: Annotations
    context_factor: Optional[AnnotationContextFactor] = 1.0
    try_square: Optional[AnnotationTrySquare] = False
    point_envelope_length: Optional[PointEnvelopeLength] = 100
    point_cross: Optional[PointCross] = PointCross.CROSS


class TargetZoom(RootModel):
    """
    The zoom level to consider for the target.
    Zoom 0 has the worst resolution (smallest image, top of the image pyramid).
    Maximum admissible zoom level depends on image.
    """
    root: Annotated[int, Field(ge=0)]


class TargetLevel(RootModel):
    """
    The tier level to consider for the target.
    Level 0 has the best resolution (largest image, basis of the image pyramid).
    Maximum admissible tier level depends on image.
    """
    root: Annotated[int, Field(ge=0)]


class TargetZoomTileIndex(BaseModel):
    zoom: TargetZoom
    ti: TileIndex


class TargetLevelTileIndex(BaseModel):
    level: TargetLevel
    ti: TileIndex


class TargetZoomTileCoordinates(BaseModel):
    zoom: TargetZoom
    tx: TileX
    ty: TileY


class TargetLevelTileCoordinates(BaseModel):
    level: TargetLevel
    tx: TileX
    ty: TileY


class TileRequest(ImageInDisplay):
    tile: Union[TargetZoomTileIndex, TargetZoomTileCoordinates,
                TargetLevelTileIndex, TargetLevelTileCoordinates]


class AssociatedName(str, Enum):
    """
    The type of associated image.

    `macro` - A macro image (generally, in slide scanners, a low resolution picture of the entire slide)
    `label` - A label image (generally a barcode)
    `thumb` - A pre-computed thumbnail
    """

    macro = 'macro'
    label = 'label'
    thumb = 'thumb'


class HistogramType(str, Enum):
    FAST = "FAST"
    COMPLETE = "COMPLETE"

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
from datetime import datetime
from enum import Enum
from typing import Any, List, Optional, Union

from fastapi import APIRouter, Depends, Path as PathParam
from pydantic import BaseModel, Field, RootModel
from starlette.requests import Request
from starlette.responses import Response
from typing_extensions import Annotated

from pims.api.exceptions import NoAppropriateRepresentationProblem, check_representation_existence
from pims.api.utils.header import ImageRequestHeaders, add_image_size_limit_header
from pims.api.utils.mimetype import OutputExtension, VISUALISATION_MIMETYPES, get_output_format
from pims.api.utils.models import (
    AssociatedName, CollectionSize, FormatId,
    ImageOutDisplayQueryParams, ZoomOrLevel
)
from pims.api.utils.output_parameter import (
    get_thumb_output_dimensions,
    safeguard_output_dimensions
)
from pims.api.utils.parameter import filepath_parameter, imagepath_parameter
from pims.api.utils.response import FastJsonResponse, convert_quantity, response_list
from pims.cache import cache_image_response, cache_response
from pims.config import Settings, get_settings
from pims.files.file import FileRole, FileType, Path
from pims.formats.utils.structures.metadata import MetadataType
from pims.processing.image_response import AssociatedResponse
from pims.utils.dtypes import dtype_to_bits

router = APIRouter(prefix=get_settings().api_base_path)
api_tags = ['Metadata']


class SingleFileInfo(BaseModel):
    """
    Information about a file
    """

    file_type: FileType
    filepath: str = Field(
        ...,
        description='The file path (filename with path, relative to the server root)',
        examples=['/a/b/c/thefile.png'],
    )
    stem: str = Field(
        ...,
        description='The file stem (filename without extension)',
        examples=['thefile'],
    )
    extension: str = Field(
        ..., description='The file extension', examples=['.png']
    )
    created_at: datetime = Field(..., description='The file creation date')
    size: int = Field(..., description='The file size, in bytes.')
    is_symbolic: bool = Field(
        False, description='Whether the file is a symbolic link or not'
    )
    role: FileRole


class CollectionFileInfo(SingleFileInfo):
    children: List[Union['CollectionFileInfo', SingleFileInfo]] = Field(
        ..., description='Information about children files'
    )


CollectionFileInfo.model_rebuild()


class FileInfo(RootModel):
    root: Union[CollectionFileInfo, SingleFileInfo]

    @classmethod
    def from_path(cls, path):
        info = {
            "file_type": FileType.from_path(path),
            "filepath": path.public_filepath,
            "stem": path.true_stem,
            "extension": path.extension,
            "created_at": path.creation_datetime,
            "size": path.size,
            "is_symbolic": path.is_symlink(),
            "role": FileRole.from_path(path)
        }
        if path.is_collection():
            children = []
            for p in path.get_extracted_children():
                if p.is_symlink():
                    children.append(FileInfo.from_path(p.resolve()))
            return CollectionFileInfo(children=children, **info)
        else:
            return SingleFileInfo(**info)


class PixelType(str, Enum):
    """
    The type used to store each pixel in the image.
    """

    int8 = 'int8'
    int16 = 'int16'
    int32 = 'int32'
    uint8 = 'uint8'
    uint16 = 'uint16'
    uint32 = 'uint32'


class ImageInfo(BaseModel):
    """
    Information about an image

    """

    original_format: FormatId = Field(
        ..., description='The original image format identifier.'
    )
    width: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The (multidimensional) image width. It is the number of pixels along X axis.',
    )
    height: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The (multidimensional) image height. It is the number of pixels along Y axis.',
    )
    depth: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The multidimensional image depth. It is the number of focal planes.',
    )
    duration: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The multidimensional image duration. It is the number of frames.',
    )
    physical_size_x: Optional[float] = Field(
        None,
        description='The physical size of a pixel along the X axis, expressed in micrometers (µm).'
    )
    physical_size_y: Optional[float] = Field(
        None,
        description='The physical size of a pixel along the Y axis, expressed in micrometers (µm).'
    )
    physical_size_z: Optional[float] = Field(
        None,
        description='The physical size of a pixel (voxel) along the Z axis, expressed in '
                    'micrometers (µm).',
    )
    frame_rate: Optional[float] = Field(
        None,
        description='The frequency at which consecutive timepoints are taken (T axis), expressed '
                    'in Hz.',
    )
    n_channels: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The number of channels in the image.'
                    'Grayscale images have 1 channel. RGB images have 3 channels.'
                    'It is the product of `n_samples` and `n_concrete_channels`.',
    )
    n_concrete_channels: int = Field(
        ...,
        description='The number of concrete channel planes in the image.'
                    'A RGB image has 3 channels, but they are usually interleaved in a single '
                    'plane. In such a case, there is only 1 concrete channel with 3 samples.',
    )
    n_samples: int = Field(
        ...,
        description='The number of samples per concrete channel. There is usually 1 sample per '
                    'concrete channel, except when a RGB image have interleaved RGB values.'
    )
    n_planes: int = Field(
        ...,
        description='The number of intrinsic planes in the image.'
                    'It is computed as `n_concrete_channels * depth * duration`.',
    )
    are_rgb_planes: bool = Field(
        ...,
        description='Whether concrete channels (and thus planes) are RGB, meaning that channels '
                    'have a meaning merged `n_samples` by `n_samples`.'
    )
    n_distinct_channels: int = Field(
        ...,
        description='The number of suggested distinct channels for visualisation.'
                    'RGB or fluorescence images have 1 single distinct channels (all channels '
                    'are merged). Hyperspectral images can have several distinct channels.'
    )
    acquired_at: Optional[datetime] = Field(
        None, description='The acquisition date of the image.'
    )
    description: Optional[str] = Field(None, description='The image description.')
    pixel_type: PixelType = Field(
        ..., description='The type used to store each pixel in the image.'
    )
    significant_bits: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The number of bits within the type storing each pixel that are significant.',
    )
    bits: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The number of bits used by the type storing each pixel.'
    )

    @classmethod
    def from_image(cls, image):
        return cls(
            **{
                "original_format": image.format.get_identifier(),
                "width": image.width,
                "height": image.height,
                "depth": image.depth,
                "duration": image.duration,
                "n_channels": image.n_channels,
                "n_concrete_channels": image.n_concrete_channels,
                "n_samples": image.n_samples,
                "n_planes": image.n_planes,
                "are_rgb_planes": bool(image.n_samples > 1),
                "n_distinct_channels": image.n_distinct_channels,
                "physical_size_x": convert_quantity(image.physical_size_x, "micrometers"),
                "physical_size_y": convert_quantity(image.physical_size_y, "micrometers"),
                "physical_size_z": convert_quantity(image.physical_size_z, "micrometers"),
                "frame_rate": convert_quantity(image.frame_rate, "Hz"),
                "acquired_at": image.acquisition_datetime,
                "description": image.description,
                "pixel_type": PixelType[str(image.pixel_type)],
                "significant_bits": image.significant_bits,
                "bits": dtype_to_bits(image.pixel_type)
            }
        )


class TierInfo(BaseModel):
    zoom: ZoomOrLevel = Field(..., description='The zoom at this tier')
    level: ZoomOrLevel = Field(..., description='The level at this tier')
    width: Annotated[int, Field(ge=1)] = Field(..., description='The tier width')
    height: Annotated[int, Field(ge=1)] = Field(..., description='The tier height')
    tile_width: Annotated[int, Field(ge=1)] = Field(
        ..., description='The width of a tile', examples=[256]
    )
    tile_height: Annotated[int, Field(ge=1)] = Field(
        ..., description='The height of a tile', examples=[256]
    )
    downsampling_factor: float = Field(
        ...,
        description='The factor by which the tier downsamples the basis of the pyramid.',
        examples=[2.0],
    )
    n_tiles: int = Field(..., description='The number of tiles at this tier')
    n_tx: int = Field(
        ..., description='The number of tiles along horizontal axis'
    )
    n_ty: int = Field(
        ..., description='The number of tiles along vertical axis'
    )

    @classmethod
    def from_tier(cls, tier):
        return cls(
            **{
                "width": tier.width,
                "height": tier.height,
                "level": tier.level,
                "zoom": tier.zoom,
                "tile_width": tier.tile_width,
                "tile_height": tier.tile_height,
                "downsampling_factor": tier.average_factor,
                "n_tiles": tier.max_ti,
                "n_tx": tier.max_tx,
                "n_ty": tier.max_ty
            }
        )


class PyramidInfo(BaseModel):
    """
    Information about an image pyramid.
    """

    n_tiers: Annotated[int, Field(ge=1)] = Field(
        ..., description='The number of tiers in the pyramid.'
    )
    tiers: List[TierInfo]

    @classmethod
    def from_pyramid(cls, pyramid):
        return cls(
            n_tiers=pyramid.n_levels,
            tiers=[TierInfo.from_tier(tier) for tier in pyramid]
        )


class SimpleRepresentationInfo(BaseModel):
    """
    Information about an image representation.

    """
    role: FileRole
    file: FileInfo


class FullRepresentationInfo(SimpleRepresentationInfo):
    pyramid: PyramidInfo


class RepresentationInfo(RootModel):
    root: Union[FullRepresentationInfo, SimpleRepresentationInfo]

    @classmethod
    def from_path(cls, path):
        if path.has_spatial_role() or path.has_spectral_role():
            return FullRepresentationInfo(
                role=FileRole.from_path(path),
                file=FileInfo.from_path(path),
                pyramid=PyramidInfo.from_pyramid(path.pyramid)
            )
        else:
            return SimpleRepresentationInfo(
                role=FileRole.from_path(path),
                file=FileInfo.from_path(path)
            )


async def _get_representation_info_list(path: Path) -> List[Union[FullRepresentationInfo, SimpleRepresentationInfo]]:
    data = []
    for representation in FileRole.representations():
        try:
            with await path.get_representation(representation, from_cache=True) as rpr:
                    data.append(RepresentationInfo.from_path(rpr))
        except AttributeError:
            pass
    return data


class Microscope(BaseModel):
    model: Optional[str] = Field(None, description='The microscope model.')

    @classmethod
    def from_image(cls, image):
        return cls(model=image.microscope.model)


class Objective(BaseModel):
    nominal_magnification: Optional[float] = Field(
        None, description='Magnification of the lens specified by the manufacturer.'
    )
    calibrated_magnification: Optional[float] = Field(
        None, description='Magnification of the lens measured by a calibration process.'
    )

    @classmethod
    def from_image(cls, image):
        return cls(
            nominal_magnification=image.objective.nominal_magnification,
            calibrated_magnification=image.objective.calibrated_magnification
        )


class InstrumentInfo(BaseModel):
    """
    Information about an instrument in an image file.
    """

    microscope: Microscope
    objective: Objective

    @classmethod
    def from_image(cls, image):
        return cls(
            microscope=Microscope.from_image(image),
            objective=Objective.from_image(image)
        )


class ChannelsInfoItem(BaseModel):
    index: Annotated[int, Field(ge=0)] = Field(..., description='Channel index.')
    suggested_name: Optional[str] = Field(
        None,
        description='Suggested name for the channel inferred from other properties.',
    )
    emission_wavelength: Optional[float] = Field(
        None, description='Wavelength of emission for a particular channel.'
    )
    excitation_wavelength: Optional[float] = Field(
        None, description='Wavelength of excitation for a particular channel.'
    )
    color: str = Field(
        None,
        description='Color for the channel (possibly inferred from other properties).'
    )

    @classmethod
    def from_channel(cls, c):
        return cls(
            **{
                "index": c.index,
                "emission_wavelength": convert_quantity(c.emission_wavelength, "nanometers"),
                "excitation_wavelength": convert_quantity(c.excitation_wavelength, "nanometers"),
                "suggested_name": c.suggested_name,
                "color": c.color.as_hex() if c.color is not None else None
            }
        )


class ChannelsInfo(RootModel):
    """
    Information about channels in an image file.
    """

    root: List[ChannelsInfoItem] = Field(
        ..., description='Information about channels in an image file.'
    )

    @classmethod
    def from_image(cls, image):
        return [ChannelsInfoItem.from_channel(c) for c in image.channels]


class AssociatedInfoItem(BaseModel):
    """
    Associated images are metadata image stored in the original image file.
    """

    width: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The associated image width. It is the number of pixels along X axis.',
    )
    height: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The associated image height. It is the number of pixels along Y axis.',
    )
    n_channels: Annotated[int, Field(ge=1)] = Field(
        ...,
        description='The number of channels in the associated image.'
                    'Grayscale images have 1 channel. RGB images have 3 channels.',
    )
    name: AssociatedName = Field(
        ...,
        description='The type of associated image.'
                    ''
                    '`macro` - A macro image (generally, in slide scanners, a low resolution picture of the entire slide)'
                    '`label` - A label image (generally a barcode)'
                    '`thumb` - A pre-computed thumbnail',
    )

    @classmethod
    def from_associated(cls, associated):
        return cls(
            **{
                "name": AssociatedName[associated._kind],
                "width": associated.width,
                "height": associated.height,
                "n_channels": associated.n_channels
            }
        )


class AssociatedInfo(RootModel):
    """
    Information about all associated in an image file.
    """

    root: List[AssociatedInfoItem] = Field(
        ..., description='Information about associated in an image file.'
    )

    @classmethod
    def from_image(cls, image):
        return [
            AssociatedInfoItem.from_associated(associated) for associated
            in (image.associated_thumb, image.associated_label, image.associated_macro)
            if associated.exists
        ]


class MetadataTypeEnum(str, Enum):
    """
    The metadata value type
    """

    STRING = 'STRING'
    INTEGER = 'INTEGER'
    DECIMAL = 'DECIMAL'
    BOOLEAN = 'BOOLEAN'
    JSON = 'JSON'
    BASE64 = 'BASE64'
    DATE = 'DATE'
    DATETIME = 'DATETIME'
    TIME = 'TIME'
    LIST = 'LIST'
    UNKNOWN = 'UNKNOWN'


class Metadata(BaseModel):
    """
    A metadata is a key-value pair stored in an image file.

    """

    key: str = Field(..., description='The metadata key')
    value: Any = Field(..., description='The metadata value')
    type: MetadataTypeEnum = Field('STRING', description='The metadata value type')
    namespace: Optional[str] = Field(
        None, description='The metadata namespace to avoid key name conflicts'
    )

    @classmethod
    def from_metadata(cls, metadata):
        return cls(
            **{
                "namespace": metadata.namespace,
                "key": metadata.key,
                "value": metadata.value if metadata.metadata_type != MetadataType.UNKNOWN else str(
                    metadata.value
                ),
                "type": MetadataTypeEnum[metadata.metadata_type.name]
            }
        )


class ImageFullInfo(BaseModel):
    image: ImageInfo
    channels: ChannelsInfo
    instrument: InstrumentInfo
    associated: AssociatedInfo
    representations: List[RepresentationInfo]


@router.get(
    '/file/{filepath:path}/info',
    response_model=FileInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
async def show_file(
    path: Path = Depends(filepath_parameter),
):
    """
    Get file info
    """
    return FileInfo.from_path(path)


@router.get(
    '/image/{filepath:path}/info',
    response_model=ImageFullInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_info(
    request: Request, response: Response , # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get all image info
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        data = dict()
        data["image"] = ImageInfo.from_image(original)
        data["instrument"] = InstrumentInfo.from_image(original)
        data["associated"] = AssociatedInfo.from_image(original)
        data["channels"] = ChannelsInfo.from_image(original)
        data["representations"] = await _get_representation_info_list(original)
        return FastJsonResponse(data)


# IMAGE

@router.get(
    '/image/{filepath:path}/info/image',
    response_model=ImageInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_image(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get standard image info
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(ImageInfo.from_image(original))


# CHANNELS

class ChannelsInfoCollection(CollectionSize):
    items: ChannelsInfo = Field(None, description='Array of channels', title='Channel')


@router.get(
    '/image/{filepath:path}/info/channels',
    response_model=ChannelsInfoCollection,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_channels(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image channel info
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(response_list(ChannelsInfo.from_image(original)))


# PYRAMID

@router.get(
    '/image/{filepath:path}/info/normalized-pyramid',
    response_model=PyramidInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_normalized_pyramid(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image normalized pyramid
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(PyramidInfo.from_pyramid(original.normalized_pyramid))


# INSTRUMENT

@router.get(
    '/image/{filepath:path}/info/instrument',
    response_model=InstrumentInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_instrument(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image instrument info
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(InstrumentInfo.from_image(original))


# ASSOCIATED

class AssociatedInfoCollection(CollectionSize):
    items: AssociatedInfo


@router.get(
    '/image/{filepath:path}/info/associated',
    response_model=AssociatedInfoCollection,
    tags=api_tags + ['Associated'],
    response_class=FastJsonResponse
)
@cache_response()
async def show_associated(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get associated file info
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(response_list(AssociatedInfo.from_image(original)))


@router.get(
    '/image/{filepath:path}/associated/{associated_key}',
    tags=api_tags + ['Associated']
)
async def show_associated_image(
    request: Request, response: Response,
    path: Path = Depends(imagepath_parameter),
    output: ImageOutDisplayQueryParams = Depends(),
    associated_key: AssociatedName = PathParam(...),
    headers: ImageRequestHeaders = Depends(),
    config: Settings = Depends(get_settings)
):
    return await _show_associated_image(
        request, response, path, **output.dict(),
        associated_key=associated_key, headers=headers,
        config=config
    )


@cache_image_response()
async def _show_associated_image(
    request: Request, response: Response,  # required for @cache  # noqa
    path: Path,
    height, width, length,
    associated_key,
    headers,
    config: Settings
):
    with await path.get_cached_spatial() as in_image:
        check_representation_existence(in_image)

        associated = getattr(in_image, f'associated_{associated_key.value}')
        if not associated or not associated.exists:
            raise NoAppropriateRepresentationProblem(path, associated_key)

        out_format, mimetype = get_output_format(
            OutputExtension.NONE, headers.accept, VISUALISATION_MIMETYPES
        )
        req_size = get_thumb_output_dimensions(associated, height, width, length)
        out_size = safeguard_output_dimensions(headers.safe_mode, config.output_size_limit, *req_size)
        out_width, out_height = out_size

        return AssociatedResponse(
            in_image, associated_key, out_width, out_height, out_format
        ).http_response(
            mimetype,
            extra_headers=add_image_size_limit_header(dict(), *req_size, *out_size)
        )


# METADATA

class MetadataCollection(CollectionSize):
    items: List[Metadata]


@router.get(
    '/image/{filepath:path}/metadata',
    response_model=MetadataCollection,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_metadata(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image metadata
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)

        store = original.raw_metadata
        return FastJsonResponse(response_list([Metadata.from_metadata(md) for md in store.values()]))

# ANNOTATIONS

class MetadataAnnotation(BaseModel):
    """
    A metadata annotation is an annotation stored in an image file.

    """
    geometry: str = Field(
        ...,
        description='A geometry described in Well-known text (WKT)',
        examples=['POINT(10 10)'],
    )
    terms: List[str] = Field(
        ...,
        description='A list of terms (labels) associated to the annotation',
        examples=['ROI']
    )
    properties: dict = Field(
        ...,
        description='A set of key-value pairs associated to the annotation'
    )
    channels: List[int] = Field(
        ...,
        description='Channel indexes associated to the annotation'
    )
    z_slices: List[int] = Field(
        ...,
        description='Z-slice indexes associated to the annotation'
    )
    timepoints: List[int] = Field(
        ...,
        description='Timepoint indexes associated to the annotation'
    )

    @classmethod
    def from_metadata_annotation(cls, annot):
        return cls(
            **{
                "geometry": annot.wkt,
                "terms": annot.terms,
                "properties": annot.properties,
                "channels": annot.channels,
                "z_slices": annot.z_slices,
                "timepoints": annot.timepoints
            }
        )


class MetadataAnnotationCollection(CollectionSize):
    items: List[MetadataAnnotation]


@router.get(
    '/image/{filepath:path}/metadata/annotations',
    response_model=MetadataAnnotationCollection,
    tags=api_tags,
    response_class=FastJsonResponse
)
@cache_response()
async def show_metadata_annotations(
    request: Request, response: Response,  # noqa
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image annotation metadata
    """
    with await path.get_cached_original() as original:
        check_representation_existence(original)
        return FastJsonResponse(response_list(
            [MetadataAnnotation.from_metadata_annotation(a)
             for a in original.annotations]
        ))


# REPRESENTATIONS

class RepresentationInfoCollection(CollectionSize):
    items: List[RepresentationInfo]


@router.get(
    '/image/{filepath:path}/info/representations',
    response_model=RepresentationInfoCollection,
    tags=api_tags,
    response_class=FastJsonResponse
)
async def list_representations(
    path: Path = Depends(imagepath_parameter)
):
    """
    Get all image representation info
    """
    return response_list(await _get_representation_info_list(path))


@router.get(
    '/image/{filepath:path}/info/representations/{representation}',
    response_model=RepresentationInfo,
    tags=api_tags,
    response_class=FastJsonResponse
)
async def show_representation(
    representation: FileRole,
    path: Path = Depends(imagepath_parameter)
):
    """
    Get image representation info
    """
    try:
        with await path.get_representation(representation, from_cache=True) as rpr:
            return RepresentationInfo.from_path(rpr)
    except AttributeError:
        raise NoAppropriateRepresentationProblem(path, representation)

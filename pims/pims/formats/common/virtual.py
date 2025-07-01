#  * Copyright (c) 2020-2022. Authors: see NOTICE file.
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
import logging
from functools import cached_property
from typing import List, Optional, Union
import pathlib
import numpy as np
import orjson
from pyvips import Image as VIPSImage

from pims.api.exceptions import MetadataParsingProblem
from pims.config import get_settings
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.engines.vips import (
    VipsSpatialConvertor
)
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.parser import AbstractParser
from pims.formats.utils.reader import AbstractReader
from pims.formats.utils.structures.metadata import ImageChannel, ImageMetadata, MetadataStore
from pims.formats.utils.structures.planes import PlanesInfo
from pims.formats.utils.structures.pyramid import Pyramid
from pims.processing.adapters import RawImagePixels, convert_to
from pims.processing.region import Region, Tile
from pims.utils.dtypes import dtype_to_bits
from pims.utils.iterables import ensure_list
from pims.utils.types import parse_datetime
from pims.utils.vips import bandjoin, fix_rgb_interpretation

log = logging.getLogger("pims.formats")


VIRTUAL_STACK_SLUG_SCHEMA = "virtual/stack"


def _json_load(path):
    if pathlib.Path(path).is_dir():
        return {}
    else:
        with open(path, "rb") as f:
            return orjson.loads(f.read())


def cached_json(format: Union[AbstractFormat, CachedDataPath]) -> dict:
    return format.get_cached(
        '_json', _json_load, str(format.path.resolve())
    )


class VirtualStackChecker(SignatureChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        try:
            json_data = cached_json(pathlike)
            return json_data.get("schema") == VIRTUAL_STACK_SLUG_SCHEMA
        except ValueError:
            return False


class VirtualStackParser(AbstractParser):
    def parse_main_metadata(self) -> ImageMetadata:
        image = cached_json(self.format)
        metadata = image.get("image")
        if metadata is None:
            raise MetadataParsingProblem(self.format.path)

        imd = ImageMetadata()
        imd.width = metadata.get("width")
        imd.height = metadata.get("height")
        imd.depth = metadata.get("depth")
        imd.duration = metadata.get("duration")
        imd.n_concrete_channels = metadata.get(
            "n_concrete_channels",
            metadata.get("n_intrinsic_channels", metadata.get("n_channels"))
        )
        imd.n_samples = metadata.get(
            "n_samples",
            metadata.get("n_channels_per_read", 1)
        )

        imd.pixel_type = np.dtype(metadata.get("pixel_type"))
        imd.significant_bits = dtype_to_bits(imd.pixel_type)

        for i, channel in enumerate(image.get("channels")):
            imd.set_channel(
                ImageChannel(index=i, color=channel.get("color"))
            )

        return imd

    def parse_known_metadata(self) -> ImageMetadata:
        imd = super().parse_known_metadata()

        image = cached_json(self.format)
        metadata = image.get("image")
        if metadata is None:
            raise MetadataParsingProblem(self.format.path)

        imd.description = metadata.get("description")
        imd.acquisition_datetime = parse_datetime(
            metadata.get("acquired_at")
        )

        imd.physical_size_x = metadata.get("physical_size_x")
        imd.physical_size_y = metadata.get("physical_size_y")
        imd.physical_size_z = metadata.get("physical_size_z")
        imd.frame_rate = metadata.get("frame_rate")

        for parsed_channel, channel in zip(imd.channels, image.get("channels")):
            parsed_channel.suggested_name = channel.get("suggested_name")
            parsed_channel.emission_wavelength = channel.get("emission_wavelength")
            parsed_channel.excitation_wavelength = channel.get("excitation_wavelength")

        imd.is_complete = True
        return imd

    def parse_raw_metadata(self) -> MetadataStore:
        store = super().parse_raw_metadata()
        return store

    def parse_pyramid(self) -> Pyramid:
        return super().parse_pyramid()  # TODO (?)

    def parse_planes(self) -> PlanesInfo:
        imd = self.format.main_imd
        pi = PlanesInfo(
            imd.n_concrete_channels, imd.depth, imd.duration,
            ['location'], ['U255']
        )

        image = cached_json(self.format)
        planes = image.get("planes")
        for c in range(imd.n_concrete_channels):
            for z in range(imd.depth):
                for t in range(imd.duration):
                    key = f"C{c}_Z{z}_T{t}"
                    pi.set(c, z, t, location=planes[key]["location"])

        return pi


class VirtualStackReader(AbstractReader):
    @staticmethod
    def _get_underlying_format(filepath):
        from pims.formats.utils.factories import SpatialReadableFormatFactory
        from pims.files.file import Path
        FILE_ROOT_PATH = get_settings().root
        return SpatialReadableFormatFactory(match_on_ext=True).match(
            Path(FILE_ROOT_PATH, filepath).get_spatial()
        )

    def read_thumb(self, out_width: int, out_height: int, precomputed: bool = None,
                   c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None,
                   t: Optional[int] = None) -> RawImagePixels:
        bands = list()
        if c is None:
            channels = list(range(self.format.main_imd.n_channels))
        else:
            channels = ensure_list(c)

        for c in channels:
            bands.append(
                convert_to(
                    self._get_underlying_format(
                        self.format.planes_info.get(c, z, t, "location")
                    ).reader.read_thumb(out_width, out_height, precomputed),
                    VIPSImage
                )
            )

        im = bandjoin(bands)
        if c == [0, 1, 2]:
            im = fix_rgb_interpretation(im)
        return im

    def read_window(self, region: Region, out_width: int, out_height: int,
                    c: Optional[Union[int, List[int]]] = None, z: Optional[int] = None,
                    t: Optional[int] = None) -> RawImagePixels:
        bands = list()
        if c is None:
            channels = list(range(self.format.main_imd.n_channels))
        else:
            channels = ensure_list(c)

        for c in channels:
            bands.append(
                convert_to(
                    self._get_underlying_format(
                        self.format.planes_info.get(c, z, t, "location")
                    ).reader.read_window(region, out_width, out_height),
                    VIPSImage
                )
            )

        im = bandjoin(bands)
        if c == [0, 1, 2]:
            im = fix_rgb_interpretation(im)
        return im

    def read_tile(self, tile: Tile, c: Optional[Union[int, List[int]]] = None,
                  z: Optional[int] = None, t: Optional[int] = None) -> RawImagePixels:
        return self.read_window(
            tile, int(tile.width), int(tile.height), c, z, t
        )


class VirtualStackFormat(AbstractFormat):
    """
    VirtualStack Format.

    A stack of 2D images, organized to mimic up to a 5D (XYCZT) image.
    The stack is described by a JSON file with the following structure:

    ```
    {
        "schema": "virtual/stack",
        "image": {
            "width": (int),
            "height": (int),
            "depth": (int),
            "duration": (int),
            "physical_size_x": (float-nullable),
            "physical_size_y": (float-nullable),
            "physical_size_z": (float-nullable),
            "frame_rate": (float-nullable),
            "n_concrete_channels": (int),
            "n_distinct_channels": (int),
            "acquired_at": (str-datetime-nullable),
            "description": (str-nullable),
            "pixel_type": (str-pixel-type)("uint8", "uint16"),
            "significant_bits": (int),
            "n_samples": (int)
        },
        "channels": [
            {
                "index": (int),
                "suggested_name": (str-nullable),
                "emission_wavelength": (float-nullable),
                "excitation_wavelength": (float-nullable),
                "color": (str-nullable)
            }
        ],
        "instrument": {
            "microscope": {
              "model": (str-nullable)
            },
            "objective": {
              "nominal_magnification": (float-nullable),
              "calibrated_magnification": (float-nullable)
            }
        },
        "planes": {
            "C0_Z0_T0": {
              "location": (str-pims-filepath-without-root)
            },
             "C0_Z0_T1": {
              "location": (str-pims-filepath-without-root)
            },
            ...
        }
    ```
    """
    checker_class = VirtualStackChecker
    parser_class = VirtualStackParser
    reader_class = VirtualStackReader
    histogram_reader_class = DefaultHistogramReader
    convertor_class = VipsSpatialConvertor

    def __init__(self, *args, **kwargs):
        super(VirtualStackFormat, self).__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

    @classmethod
    def is_importable(cls) -> bool:
        # We do not allow to import virtual stacks as we do for regular images !
        # As virtual stacks make references to other uploads, virtual stacks could
        # be used to get privileges on other uploads !!
        # The virtual stack file (JSON) is expected to be created by other means
        # than the regular import, and by a service who has enough rights and is
        # security-aware of access rights on images.
        return False

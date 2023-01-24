import os
from datetime import datetime
from functools import cached_property
from pathlib import Path
from typing import List, Optional

import shapely
from pydicom.multival import MultiValue
from wsidicom.graphical_annotations import Point as WsiPoint
from wsidicom.graphical_annotations import Polygon as WsiPolygon
from wsidicom.wsidicom import WsiDicom

from pims.formats.utils.abstract import AbstractChecker, AbstractParser, AbstractReader, AbstractFormat, CachedDataPath
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.formats.utils.structures.annotations import ParsedMetadataAnnotation
from pims.formats.utils.structures.metadata import ImageMetadata, ImageChannel
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils import UNIT_REGISTRY
from pims.utils.dtypes import np_dtype
from pims.utils.types import parse_float


def dictify(ds):
    output = dict()
    for elem in ds:
        if elem.VR != 'SQ':
            output[elem.name] = elem.value
        else:
            output[elem.name] = [dictify(item) for item in elem]
    return output


def recurse_if_SQ(ds):
    list_ds = []

    for data_element in ds:
        if data_element.VR != 'SQ':
            list_ds.append(data_element)

        else:
            for elmt in data_element:
                list_recursive = recurse_if_SQ(elmt)
                list_ds.extend(list_recursive)
    return list_ds


def cached_wsi_dicom_file(format: AbstractFormat) -> WsiDicom:
    return format.get_cached('_wsi_dicom', WsiDicom.open, str(format.path))


def get_root_file(path: Path) -> Optional[Path]:
    """Try to get WSI DICOM directory (as it is a multi-file format)."""
    if path.is_dir():
        if sum(1 for _ in Path(path).glob('*')):
            for child in path.iterdir():
                if child.is_dir():
                    return path
    return None


class WSIDicomChecker(AbstractChecker):
    OFFSET = 128

    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        from pims.files.file import Path
        path = pathlike.path
        if os.path.isdir(path):
            # list_subdir = [f.path for f in os.scandir(path) if os.path.isdir(f)]
            # if len(list_subdir) == 1:
            for child in os.listdir(path):
                # verification on the format signature for each .dcm file
                complete_path = Path(os.path.join(path, child))
                cached_child = CachedDataPath(complete_path)
                buf = cached_child.get_cached('signature', cached_child.path.signature)
                if not (len(buf) > cls.OFFSET + 4 and
                        buf[cls.OFFSET] == 0x44 and
                        buf[cls.OFFSET + 1] == 0x49 and
                        buf[cls.OFFSET + 2] == 0x43 and
                        buf[cls.OFFSET + 3] == 0x4D):
                    return False
            return True
            # return False
        return False


class WSIDicomParser(AbstractParser):

    def parse_main_metadata(self):
        wsidicom_object = cached_wsi_dicom_file(self.format)
        levels = wsidicom_object.levels
        imd = ImageMetadata()

        imd.width = levels.base_level.size.width
        imd.height = levels.base_level.size.height
        metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])
        if 'Bits Stored' in metadata:
            imd.significant_bits = metadata["Bits Stored"]
        else:
            imd.significant_bits = 8

        imd.duration = 1
        if "Samples per Pixel" in metadata:
            imd.n_samples = metadata["Samples per Pixel"]
        imd.depth = 1
        imd.n_concrete_channels = 1
        imd.pixel_type = np_dtype(imd.significant_bits)
        if "Manufacturer's Model Name" in metadata:
            imd.microscope.model = metadata["Manufacturer's Model Name"]

        if 'Objective Lens Power' in metadata['Optical Path Sequence'][0]:
            imd.objective.nominal_magnification = parse_float(
                metadata['Optical Path Sequence'][0]['Objective Lens Power'])

        if imd.n_channels == 3:
            imd.set_channel(ImageChannel(index=0, suggested_name='R'))
            imd.set_channel(ImageChannel(index=1, suggested_name='G'))
            imd.set_channel(ImageChannel(index=2, suggested_name='B'))
        else:
            imd.set_channel(ImageChannel(index=0, suggested_name='L'))
        imd.n_channels_per_read = imd.n_channels

        if wsidicom_object.labels:
            label_img = wsidicom_object.read_label()
            imd.associated_label.width = label_img.width
            imd.associated_label.height = label_img.height
            imd.associated_label.n_channels = 3

        if wsidicom_object.overviews:
            overview = wsidicom_object.read_overview()
            imd.associated_macro.width = overview.width
            imd.associated_macro.height = overview.height
            imd.associated_macro.n_channels = 3

        return imd

    def parse_known_metadata(self):
        wsidicom_object = cached_wsi_dicom_file(self.format)
        levels = wsidicom_object.levels

        metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])
        imd = super().parse_known_metadata()
        imd.physical_size_x = wsidicom_object.levels.groups[0].mpp.width * UNIT_REGISTRY("micrometers")
        imd.physical_size_y = wsidicom_object.levels.groups[0].mpp.height * UNIT_REGISTRY("micrometers")

        imd.physical_size_z = self.parse_physical_size(
            metadata['Shared Functional Groups Sequence'][0]['Pixel Measures Sequence'][0]['Spacing Between Slices'])
        if 'Acquisition DateTime' in metadata:
            imd.acquisition_datetime = self.parse_acquisition_date(metadata['Acquisition DateTime'])
        return imd

    def parse_raw_metadata(self):
        wsidicom_object = cached_wsi_dicom_file(self.format)
        levels = wsidicom_object.levels
        store = super().parse_raw_metadata()
        ds = wsidicom_object.levels.groups[0].datasets[0]
        data_elmts = recurse_if_SQ(ds)

        for data_element in data_elmts:
            name = data_element.name
            if data_element.is_private:
                tag = data_element.tag
                name = f"{tag.group:04x}_{tag.element:04x}"  # noqa
            name = name.replace(' ', '')

            value = data_element.value
            if type(value) is MultiValue:
                value = list(value)
            store.set(name, value, namespace="DICOM")

        return store

    def parse_pyramid(self):
        pyramid = Pyramid()

        wsidicom_object = cached_wsi_dicom_file(self.format)
        levels = wsidicom_object.levels

        for level in wsidicom_object.levels.levels:
            level_info = wsidicom_object.levels.get_level(level)
            level_size = level_info.size
            tile_size = level_info.tile_size
            pyramid.insert_tier(level_size.width, level_size.height, (tile_size.width, tile_size.height))

        return pyramid

    def parse_annotations(self) -> List[ParsedMetadataAnnotation]:
        wsidicom_object = cached_wsi_dicom_file(self.format)
        channels = list(range(self.format.main_imd.n_channels))
        parsed_annots = []
        pixel_spacing = wsidicom_object.levels.groups[0].pixel_spacing.width

        ds_annot = wsidicom_object.annotations
        for annot in ds_annot:
            annotation_groups = annot.groups
            for annotation_group in annotation_groups:
                for annotation in annotation_group:
                    coords = annotation.geometry.to_coords()  # list of tuples
                    coords_pixels = []
                    for xy in coords:  # tuple
                        new_xy = tuple(int(value / pixel_spacing) for value in xy)
                        coords_pixels.append(new_xy)
                    if isinstance(annotation.geometry, WsiPolygon):
                        annotation_geom = shapely.geometry.Polygon(coords_pixels)
                    elif isinstance(annotation.geometry, WsiPoint):
                        annotation_geom = shapely.geometry.Point(coords_pixels)
                    else:
                        pass
                    parsed = ParsedMetadataAnnotation(annotation_geom, channels, 0, 0)
                    parsed_annots.append(parsed)
        return parsed_annots

    @staticmethod
    def parse_acquisition_date(date: str):
        """
        Datetime examples: 20211216163400 -> 16/12/21, 16h34
        """
        try:
            if date:
                str_date = datetime.strptime(date.split('.')[0], "%Y%m%d%H%M%S")
                return f'{str_date}'

            else:
                return None
        except (ValueError, TypeError):
            return None

    @staticmethod
    def parse_physical_size(physical_size: str):
        if physical_size is not None and parse_float(physical_size) is not None:
            return parse_float(physical_size) * UNIT_REGISTRY("millimeter")
        return None


class WSIDicomReader(AbstractReader):

    def read_thumb(self, out_width, out_height, precomputed=True, c=None, z=None, t=None):
        img = cached_wsi_dicom_file(self.format)

        return img.read_thumbnail((out_width, out_height))

    def read_window(self, region, out_width, out_height, c=None, z=None, t=None):
        img = cached_wsi_dicom_file(self.format)

        tier = self.format.pyramid.most_appropriate_tier(region, (out_width, out_height))
        region = region.scale_to_tier(tier)
        level = tier.level
        norm_level = img.levels.levels[level]
        return img.read_region((region.left, region.top), norm_level, (region.width, region.height))

    def read_tile(self, tile, c=None, z=None, t=None):
        return self.read_window(tile, tile.width, tile.height, c, z, t)

    def read_macro(self, out_width, out_height):
        img = cached_wsi_dicom_file(self.format)
        return img.read_overview()

    def read_label(self, out_width, out_height):
        img = cached_wsi_dicom_file(self.format)
        return img.read_label()


class WSIDicomFormat(AbstractFormat):
    checker_class = WSIDicomChecker
    parser_class = WSIDicomParser
    reader_class = WSIDicomReader
    histogram_reader_class = DefaultHistogramReader

    def __init__(self, path, *args, **kwargs):
        super().__init__(path, *args, **kwargs)

        root = get_root_file(path)
        if root:
            self._path = root
            self.clear_cache()

        self._enabled = True

    @classmethod
    def get_name(cls):
        return "WSI Dicom"

    @classmethod
    def get_remarks(cls):
        return "A set of .dcm files packed in archive directory."

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

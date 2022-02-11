from functools import cached_property

import sys, os, io
import numpy as np

from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.abstract import AbstractChecker, AbstractParser, AbstractReader, AbstractFormat, CachedDataPath
from pims.formats.utils.structures.metadata import ImageMetadata, ImageChannel
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.utils.types import parse_float, parse_int, parse_datetime
from pyvips import Image as VipsImage
from pims.processing.region import Region
from pims.formats.utils.structures.pyramid import Pyramid, PyramidTier
from pims.utils import UNIT_REGISTRY
from PIL import Image
import numpy as np
from pims.utils.dtypes import np_dtype
from wsidicom.geometry import Point, PointMm, Region, RegionMm, Size, SizeMm
from wsidicom.wsidicom import WsiDicom, WsiDicomGroup, WsiDicomLevel, WsiDicomLevels
from wsidicom.instance import WsiDataset
from datetime import datetime
from zipfile import ZipFile
import json


class WSIDicomChecker(AbstractChecker):
    OFFSET = 128
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        from pims.files.file import Path
        path = pathlike.path
        if os.path.isdir(path):
            list_subdir = [f.path for f in os.scandir(path) if os.path.isdir(f)]
            if len(list_subdir) == 1:
                for child in os.listdir(list_subdir[0]):
                    # verification on the format signature for each .dcm file
                    complete_path = Path(os.path.join(path, list_subdir[0], child))
                    cached_child = CachedDataPath(complete_path)
                    buf = cached_child.get_cached('signature', cached_child.path.signature)
                    if not (len(buf) > cls.OFFSET + 4 and
                            buf[cls.OFFSET] == 0x44 and
                            buf[cls.OFFSET + 1] == 0x49 and
                            buf[cls.OFFSET + 2] == 0x43 and
                            buf[cls.OFFSET + 3] == 0x4D):
                        return False
                return True
            return False
        return False
    
def dictify(ds):
    output = dict()
    for elem in ds:
        if elem.VR != 'SQ':
            output[elem.name] = elem.value
        else:
            output[elem.name] = [dictify(item) for item in elem]
    return output
        
class WSIDicomParser(AbstractParser):
        
    def parse_main_metadata(self):
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        wsidicom_object = WsiDicom.open(str(list_subdir[0]))
        levels = wsidicom_object.levels
        imd = ImageMetadata()
        
        imd.width = levels.base_level.size.width
        imd.height = levels.base_level.size.height
        metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])
        imd.significant_bits = metadata["Bits Stored"]
        
        imd.duration = 1
        imd.n_channels = metadata["Samples per Pixel"] # same thing for all WsiDatasets? 
        #print(imd.n_channels)
        imd.depth = 1
        imd.n_intrinsic_channels = 1
        imd.pixel_type = np_dtype(imd.significant_bits)
        imd.microscope.model = metadata["Manufacturer's Model Name"]
        
        if 'Objective Lens Power' in metadata['Optical Path Sequence'][0]:
            imd.objective.nominal_magnification = parse_float(metadata['Optical Path Sequence'][0]['Objective Lens Power'])
            
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
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        wsidicom_object = WsiDicom.open(str(list_subdir[0]))
        levels = wsidicom_object.levels
                
        metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])     
        #print(metadata)
        imd = super().parse_known_metadata()
        imd.physical_size_x = wsidicom_object.levels.groups[0].mpp.width * UNIT_REGISTRY("micrometers")
        imd.physical_size_y = wsidicom_object.levels.groups[0].mpp.height * UNIT_REGISTRY("micrometers")
        
        #if 'Spacing Between Slices' in metadata:
        imd.physical_size_z = self.parse_physical_size(metadata['Shared Functional Groups Sequence'][0]['Pixel Measures Sequence'][0]['Spacing Between Slices'])
        #print(metadata['Shared Functional Groups Sequence'][0]['Pixel Measures Sequence'][0]['Spacing Between Slices'])
        if 'Acquisition DateTime' in metadata:
            imd.acquisition_datetime = self.parse_acquisition_date(metadata['Acquisition DateTime'])
        return imd
        
    def parse_raw_metadata(self):
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        wsidicom_object = WsiDicom.open(str(list_subdir[0]))
        levels = wsidicom_object.levels
                
        metadata = dictify(wsidicom_object.levels.groups[0].datasets[0])
        store = super().parse_raw_metadata()
        
        if 'Device Serial Number' in metadata:
            store.set('Device Serial Number', metadata['Device Serial Number'])
            
        if 'Software Versions' in metadata:
            store.set('Software Versions', metadata['Software Versions'])
        return store
    
    def parse_pyramid(self):
        pyramid = Pyramid()
        
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        wsidicom_object = WsiDicom.open(str(list_subdir[0]))
        levels = wsidicom_object.levels
                
        for level in wsidicom_object.levels.levels:
            level_info = wsidicom_object.levels.get_level(level)
            level_size = level_info.size
            tile_size = level_info.tile_size
            pyramid.insert_tier(level_size.width, level_size.height, (tile_size.width, tile_size.height))

        return pyramid
        
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
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        img = WsiDicom.open(str(list_subdir[0]))
               
        return img.read_thumbnail((out_width, out_height))
    
    def read_window(self, region, out_width, out_height, c=None, z=None, t=None):
        
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        img = WsiDicom.open(str(list_subdir[0]))

        tier = self.format.pyramid.most_appropriate_tier(region, (out_width, out_height))
        region = region.scale_to_tier(tier)
        level = tier.level
        norm_level = img.levels.levels[level]
        return img.read_region((region.left, region.top), norm_level, (region.width, region.height))
    
    def read_tile(self, tile, c=None, z=None, t=None):
        return self.read_window(tile, tile.width, tile.height, c, z, t)
        
    def read_macro(self, out_width, out_height):
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        img = WsiDicom.open(str(list_subdir[0]))
        return img.read_overview()
        
    def read_label(self, out_width, out_height):
        list_subdir = [f.path for f in os.scandir(self.format.path) if f.is_dir()]
        img = WsiDicom.open(str(list_subdir[0]))
        return img.read_label()
        
        
class WSIDicomFormat(AbstractFormat):
    checker_class = WSIDicomChecker
    parser_class = WSIDicomParser
    reader_class = WSIDicomReader
    histogram_reader_class = DefaultHistogramReader 

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "WSI Dicom"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

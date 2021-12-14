from functools import cached_property
from pathlib import Path

import sys, os, io
import numpy as np

from pims.formats.utils.checker import SignatureChecker
from pims.formats.utils.abstract import AbstractChecker, AbstractParser, AbstractReader, AbstractFormat, CachedDataPath
from pims.formats.utils.structures.metadata import ImageMetadata, ImageChannel
from pims.formats.utils.histogram import DefaultHistogramReader
from pims.utils.types import parse_float, parse_int
from pyvips import Image as VipsImage
from pims.processing.region import Region
from pims.formats.utils.structures.pyramid import Pyramid
from pims.utils import UNIT_REGISTRY
from PIL import Image
import numpy as np

from wsidicom.geometry import Point, PointMm, Region, RegionMm, Size, SizeMm
from wsidicom.wsidicom import WsiDicom, WsiDicomGroup, WsiDicomLevel, WsiDicomLevels
from wsidicom.instance import WsiDataset

from zipfile import ZipFile




def get_root_file(path):
    if path.is_dir(): 
        
        for subdir in path.iterdir(): 
            if subdir.is_dir():
                for child in subdir.iterdir():
                    if not child.suffix == '.dcm':
                        return None
                return subdir
            return None
    return None


class DicomChecker(AbstractChecker):
    @classmethod
    def match(cls, pathlike): 
        root = get_root_file(pathlike.path)
        if root:
            return root.is_dir()
        return False

        
class DicomParser(AbstractParser):
        
    def parse_main_metadata(self):
        for subdir in self.format.path.iterdir():
            if self.format.path.is_dir() and (subdir.is_dir()):
                wsidicom_object = WsiDicom.open(str(subdir))
                levels = wsidicom_object.levels
        imd = ImageMetadata()

        imd.width = levels.base_level.size.width
        imd.height = levels.base_level.size.height
        imd.significant_bits = 8
        
        imd.duration = 1
        imd.n_channels = wsidicom_object.levels.groups[0].datasets[0].samples_per_pixel # same thing for all WsiDatasets? 
        imd.depth = 1
        
        imd.pixel_type = np.dtype('uint8')
        
        if imd.n_channels == 3:
            imd.set_channel(ImageChannel(index=0, suggested_name='R'))
            imd.set_channel(ImageChannel(index=1, suggested_name='G'))
            imd.set_channel(ImageChannel(index=2, suggested_name='B'))
        else:
            imd.set_channel(ImageChannel(index=0, suggested_name='L'))
        imd.n_channels_per_read = imd.n_channels
        
        return imd
        
    def parse_known_metadata(self):
        for subdir in self.format.path.iterdir():
            if self.format.path.is_dir() and (subdir.is_dir()):
                wsidicom_object = WsiDicom.open(str(subdir))
                levels = wsidicom_object.levels
        imd = super().parse_known_metadata()
        imd.physical_size_x = wsidicom_object.levels.groups[0].mpp.width * UNIT_REGISTRY("micrometers")
        imd.physical_size_y = wsidicom_object.levels.groups[0].mpp.height * UNIT_REGISTRY("micrometers")
        return imd
        
    def parse_raw_metadata(self):
        store = super().parse_raw_metadata()
        return store
    
    def parse_pyramid(self):
        pyramid = Pyramid()
        
        for subdir in self.format.path.iterdir():
            files = os.listdir(subdir)
            
            if self.format.path.is_dir() and (subdir.is_dir()): 
                wsidicom_object = WsiDicom.open(str(subdir))
                levels = wsidicom_object.levels
        print(wsidicom_object.levels.highest_level)
        print(wsidicom_object.levels.levels)
        for level in wsidicom_object.levels.levels:
            level_info = wsidicom_object.levels.get_level(level)
            level_size = level_info.size
            nb_tiles = level_info.default_instance.tiled_size # /!\ nb de tuiles, pas la taille des tuiles
            tile_size = (round(level_size.width/parse_int(nb_tiles.width)), round(level_size.height/parse_int(nb_tiles.height)))
            pyramid.insert_tier(level_size.width, level_size.height, tile_size)

        return pyramid
    
class DicomReader(AbstractReader):
        
    def read_thumb(self, out_width, out_height, precomputed=True, c=None, z=None, t=None):
        for subdir in self.format.path.iterdir():
            if self.format.path.is_dir() and (subdir.is_dir()):
                img = WsiDicom.open(str(subdir))
               
        return img.read_thumbnail((out_width, out_height))
    
    def read_window(self, region, out_width, out_height, c=None, z=None, t=None):
        """
        out_size = (out_width, out_height)
        tier = self.format.pyramid.most_appropriate_tier(region, out_size)
        region = region.scale_to_tier(tier)
        """
        
        for subdir in self.format.path.iterdir():
            if self.format.path.is_dir() and (subdir.is_dir()):
                img = WsiDicom.open(str(subdir))
        level = round(np.log2(region.downsample))
        return img.read_region((region.left, region.top), level, (region.width, region.height))
    
    
    def read_tile(self, tile, c=None, z=None, t=None):
        return self.read_window(tile, tile.width, tile.height, c, z, t)

class DicomFormat(AbstractFormat):
    checker_class = DicomChecker
    parser_class = DicomParser
    reader_class = DicomReader
    histogram_reader_class = DefaultHistogramReader 

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._enabled = True

    @classmethod
    def get_name(cls):
        return "DICOM WSI"

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

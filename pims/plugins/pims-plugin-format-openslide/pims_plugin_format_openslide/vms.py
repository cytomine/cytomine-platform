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
from pathlib import Path
from typing import Optional

from pims.cache import cached_property
from pims.formats import AbstractFormat
from pims.formats.utils.abstract import CachedDataPath
from pims.formats.utils.checker import AbstractChecker
from pims.formats.utils.histogram import DefaultHistogramReader
from pims_plugin_format_openslide.utils.engine import OpenslideVipsParser, OpenslideVipsReader


def get_root_file(path: Path) -> Optional[Path]:
    """Try to get VMS main file (as it is a multi-file format)."""
    if path.is_dir():
        for child in path.iterdir():
            if child.suffix == '.vms':
                return child
    return None


class VMSChecker(AbstractChecker):
    @classmethod
    def match(cls, pathlike: CachedDataPath) -> bool:
        root = get_root_file(pathlike.path)
        if root:
            with open(root, 'r') as vms:
                return vms.readline().strip() == '[Virtual Microscope Specimen]'
        return False


class VMSFormat(AbstractFormat):
    """
    Hamamatsu VMS.

    References
        https://openslide.org/formats/hamamatsu/
        https://docs.openmicroscopy.org/bio-formats/6.5.1/formats/hamamatsu-vms.html

    """
    checker_class = VMSChecker
    parser_class = OpenslideVipsParser
    reader_class = OpenslideVipsReader
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
        return "Hamamatsu VMS"

    @classmethod
    def get_remarks(cls):
        return "One .vms file, one .opt optimization file and several .jpg with same name, " \
               "packed in an archive. "

    @classmethod
    def is_spatial(cls):
        return True

    @cached_property
    def need_conversion(self):
        return False

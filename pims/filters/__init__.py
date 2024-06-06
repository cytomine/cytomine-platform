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

import logging
import re
from abc import ABC, abstractmethod
from importlib import import_module
from inspect import isabstract, isclass
from pkgutil import iter_modules
from typing import Callable, Dict, List, Tuple, Type, Union

from pims.processing.adapters import RawImagePixels, RawImagePixelsType, imglib_adapters

FILTER_PLUGIN_PREFIX = 'pims_filter_'
NON_PLUGINS_MODULES = ["pims.filters.utils"]
_CAMEL_TO_SPACE_PATTERN = re.compile(r'((?<=[a-z])[A-Z]|(?<!\A)[A-Z](?=[a-z]))')

logger = logging.getLogger("pims.app")
logger.info("Filters initialization...")


class AbstractFilter(ABC):
    """
    Base class for a filter.
    Filters are expected to be called like functions.
    """
    _impl: Dict[RawImagePixelsType, Callable]

    def __init__(self, histogram=None):
        self._impl = {}

        if self.require_histogram() and histogram is None:
            raise ValueError("Histogram parameter is not set, while the filter requires it.")
        self.histogram = histogram

    @property
    def implementations(self) -> List[RawImagePixelsType]:
        return list(self._impl.keys())

    @property
    def implementation_adapters(
        self
    ) -> Dict[Tuple[RawImagePixelsType, RawImagePixelsType], Callable]:
        return imglib_adapters

    def __call__(
        self, obj: RawImagePixels, *args, **kwargs
    ) -> Union[RawImagePixels, bytes]:
        """
        Apply image operation on given obj. Return type is a convertible
        image type (but not necessarily the type of `obj`).
        """

        if type(obj) not in self.implementations:
            obj = self.implementation_adapters.get(
                (type(obj), self.implementations[0])
            )(obj)

        processed = self._impl[type(obj)](obj, *args, **kwargs)
        return processed

    @classmethod
    def init(cls):
        """
        Initialize the filter, such that all third-party libs are ready.
        """
        pass

    @classmethod
    def identifier(cls):
        return cls.__name__.replace('Filter', '')

    @classmethod
    def get_identifier(cls, uppercase=True):
        """
        Get the filter identifier. It must be unique across all filters.

        Parameters
        ----------
        uppercase: bool
            If the filter must be returned in uppercase.
            In practice, comparisons are always done using the uppercase identifier

        Returns
        -------
        identifier: str
            The filter identifier
        """
        identifier = cls.identifier()
        if uppercase:
            return identifier.upper()
        return identifier

    @classmethod
    def aliases(cls):
        return []

    @classmethod
    def get_aliases(cls, uppercase=True):
        if uppercase:
            return [alias.upper() for alias in cls.aliases()]
        return cls.aliases()

    @classmethod
    def get_name(cls):
        return re.sub(_CAMEL_TO_SPACE_PATTERN, r' \1', cls.get_identifier(False))

    @classmethod
    def get_description(cls):
        return str()

    @classmethod
    @abstractmethod
    def get_type(cls):
        pass

    @classmethod
    @abstractmethod
    def require_histogram(cls):
        pass

    @classmethod
    @abstractmethod
    def required_colorspace(cls):
        pass

    @classmethod
    def get_plugin_name(cls):
        return '.'.join(cls.__module__.split('.')[:-1])


def _discover_filter_plugins():
    plugins = [name for _, name, _ in iter_modules(__path__, prefix="pims.filters.")
               if name not in NON_PLUGINS_MODULES]
    plugins += [name for _, name, _ in iter_modules()
                if name.startswith(FILTER_PLUGIN_PREFIX)]

    logger.info(
        f"Filter plugins: found {len(plugins)} plugin(s) "
        f"({', '.join(plugins)})", )
    return plugins


def _find_filters_in_module(module_name):
    """
    Find all Filter classes in a module.

    Parameters
    ----------
    module_name: str
        The module to analyze

    Returns
    -------
    filters: list
        The filter classes
    """
    filters = list()

    mod = import_module(module_name)
    is_package = hasattr(mod, '__path__')
    if is_package:
        for _, name, _ in iter_modules(mod.__path__):
            filters += _find_filters_in_module(f"{mod.__name__}.{name}")
    else:
        try:
            for var in vars(mod).values():
                if isclass(var) and issubclass(var, AbstractFilter) and \
                        not isabstract(var) and 'Abstract' not in var.__name__:
                    imgfilter = var
                    filters.append(var)
                    imgfilter.init()
                    logger.info(
                        f" * {imgfilter.get_identifier()} "
                        f"- {imgfilter.get_name()} imported."
                    )
        except ImportError as e:
            logger.error(f"{module_name} submodule cannot be checked for filters !", exc_info=e)
    return filters


def _get_all_filters():
    """
    Find all Filter classes in modules specified in FILTER_PLUGINS.

    Returns
    -------
    filters: list
        The filter classes
    """
    filters = list()
    for module_name in FILTER_PLUGINS:
        logger.info(f"Importing filters from {module_name} plugin...")
        filters.extend(_find_filters_in_module(module_name))

    return filters


FiltersById = Dict[str, Type[AbstractFilter]]


FILTER_PLUGINS = _discover_filter_plugins()
FILTERS = {f.get_identifier(): f for f in _get_all_filters()}

# Add aliases
_aliases = dict()
for f in FILTERS.values():
    _aliases.update({alias: f for alias in f.get_aliases()})
FILTERS.update(_aliases)

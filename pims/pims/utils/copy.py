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
import copy
from itertools import chain


class SafelyCopiable:
    UNSAFE_DEEPCOPY_ATTRS = ('_cache',)

    def _copy__new(self):
        cls = self.__class__
        return cls.__new__(cls)

    def __copy__(self):
        # Create a new instance
        result = self._copy__new()

        # Copy all attributes
        result.__dict__.update(self.__dict__)

        # Get all __slots__ of the derived class
        slots = chain.from_iterable(
            getattr(s, '__slots__', [])
            for s in self.__class__.__mro__
        )

        # Copy all slot attributes
        for var in slots:
            setattr(result, var, copy.copy(getattr(self, var)))

        # Return updated instance
        return result

    def _deepcopy_new(self):
        return self._copy__new()

    def __deepcopy__(self, memo):
        # Create a new instance
        result = self._deepcopy_new()

        # Don't copy self reference
        memo[id(self)] = result

        # Don't copy the cache - if it exists
        for unsafe_attr in self.UNSAFE_DEEPCOPY_ATTRS:
            if hasattr(self, unsafe_attr):
                unsafe_var = getattr(self, unsafe_attr)
                memo[id(unsafe_var)] = unsafe_var.__new__(type(unsafe_var))

        # Deep copy all other attributes
        for k, v in self.__dict__.items():
            setattr(result, k, copy.deepcopy(v, memo))  # noqa

        # Get all __slots__ of the derived class
        slots = chain.from_iterable(
            getattr(s, '__slots__', [])
            for s in self.__class__.__mro__
        )

        # Deep copy all other attributes
        for var in slots:
            try:
                setattr(
                    result, var,
                    copy.deepcopy(getattr(self, var), memo)  # noqa
                )
            except AttributeError as e:
                pass

        # Return updated instance
        return result

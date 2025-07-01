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

from tifffile import OmeXml as _OmeXml

from pims import __title__, __version__


class OmeXml(_OmeXml):
    def __init__(self, **metadata):
        if 'Creator' not in metadata.keys():
            metadata['Creator'] = f'{__title__} v{__version__}'
        super().__init__(**metadata)

    def addimage(self, imd, shape, storedshape, axes=None, **metadata):

        if imd.acquisition_datetime is not None:
            metadata['AcquisitionDate']: imd.acquisition_datetime.isoformat()

        if imd.description is not None:
            metadata['Description']: imd.description

        if imd.physical_size_x is not None:
            metadata['PhysicalSizeX'] = imd.physical_size_x.magnitude
            metadata['PhysicalSizeXUnit'] = 'µm'

        if imd.physical_size_y is not None:
            metadata['PhysicalSizeY'] = imd.physical_size_y.magnitude
            metadata['PhysicalSizeYUnit'] = 'µm'

        if imd.physical_size_z is not None:
            metadata['PhysicalSizeZ'] = imd.physical_size_z.magnitude
            metadata['PhysicalSizeZUnit'] = 'µm'

        if imd.frame_rate is not None and imd.frame_rate.magnitude > 0:
            metadata['TimeIncrement'] = 1 / imd.frame_rate.magnitude
            metadata['TimeIncrementUnit'] = 's'

        metadata['Channels'] = list()
        for cmd in imd.channels:
            channel = dict()

            if cmd.suggested_name is not None:
                channel['Name'] = cmd.suggested_name

            if cmd.color is not None:
                channel['Color'] = cmd.color.as_int()

            if cmd.emission_wavelength is not None:
                channel['EmissionWavelength'] = cmd.emission_wavelength
                channel['EmissionWavelengthUnit'] = 'nm'

            if cmd.excitation_wavelength is not None:
                channel['ExcitationWavelength'] = cmd.excitation_wavelength
                channel['ExcitationWavelengthUnit'] = 'nm'

            metadata['Channels'].append(channel)

        dtype = imd.pixel_type
        return super().addimage(dtype=dtype, shape=shape, storedshape=storedshape, axes=axes, **metadata)


omexml_type = {
    'int8': 'int8',
    'int16': 'int16',
    'int32': 'int32',
    'uint8': 'uint8',
    'uint16': 'uint16',
    'uint32': 'uint32',
    'float': 'float32',
    'double': 'float64',
    'complex': 'complex64',
    'double-complex': 'complex128',
    'bit': 'bool'
}
/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

let vendors = {
  hamamatsu: {
    imgPath: require('@/assets/brands/hamamatsu.jpg'),
    name: 'Hamamatsu Photonics'
  },
  '3dh': {
    imgPath: require('@/assets/brands/3dh.png'),
    name: '3DHISTECH Ltd.'
  },
  aperio: {
    imgPath: require('@/assets/brands/aperio.jpg'),
    name: 'Aperio'
  },
  leica: {
    imgPath: require('@/assets/brands/leica.png'),
    name: 'Leica Biosystems'
  },
  roche: {
    imgPath: require('@/assets/brands/roche.gif'),
    name: 'La Roche Ltd.'
  },
  philips: {
    imgPath: require('@/assets/brands/philips.svg'),
    name: 'Philips'
  }
};

export default function vendorFromFormat(format) {
  switch (format) {
    case 'NDPI':
    case 'VMS':
      return vendors['hamamatsu'];
    case 'MRXS':
      return vendors['3dh'];
    case 'SVS':
      return vendors['aperio'];
    case 'SCN':
      return vendors['leica'];
    case 'VENTANA':
    case 'BIF':
      return vendors['roche'];
    case 'PHILIPS':
      return vendors['philips'];
    default:
      return null;
  }
}

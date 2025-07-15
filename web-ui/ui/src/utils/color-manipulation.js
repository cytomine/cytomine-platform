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

/* eslint no-undef: 0 */

/* Sources:
 * https://www.dfstudios.co.uk/articles/programming/image-programming-algorithms/
 * https://stackoverflow.com/a/8510751
 * http://alienryderflex.com/saturation.html
 */

function computeContrastFactorFunc(contrast) {
  return 259 * (contrast + 255) / 255 / (259 - contrast);
}

function computeHueMatrixFunc(hue) {
  let angle = hue * Math.PI / 180;
  let cosA = Math.cos(angle);
  let sinA = Math.sin(angle);
  let sqrtThird = Math.sqrt(1 / 3);
  let hueMatrix = [
    [
      cosA + (1 - cosA) / 3,
      1 / 3 * (1 - cosA) - sqrtThird * sinA,
      1 / 3 * (1 - cosA) + sqrtThird * sinA
    ],
    [
      1 / 3 * (1 - cosA) + sqrtThird * sinA,
      cosA + 1 / 3 * (1 - cosA),
      1 / 3 * (1 - cosA) - sqrtThird * sinA
    ],
    [
      1 / 3 * (1 - cosA) - sqrtThird * sinA,
      1 / 3 * (1 - cosA) + sqrtThird * sinA,
      cosA + 1 / 3 * (1 - cosA)
    ]
  ];
  return hueMatrix;
}

function truncatePixelValueFunc(val) {
  return val < 0 ? 0 : val > 255 ? 255 : val;
}

function changeBrightnessContrastFunc(pixel) {
  for (let i = 0; i < 3; i++) {
    let newVal = truncatePixelValue(contrastFactor * (pixel[i] - 128) + 128) + brightness;
    pixel[i] = truncatePixelValue(newVal);
  }
}

function changeSaturationFunc(pixel) {
  let r = pixel[0];
  let g = pixel[1];
  let b = pixel[2];

  let saturationConstant = Math.sqrt(r * r * Pr + g * g * Pg + b * b * Pb);

  for (let i = 0; i < 3; i++) {
    let newVal = saturationConstant + (pixel[i] - saturationConstant) * (1 + saturation / 100);
    pixel[i] = truncatePixelValue(newVal);
  }
}

function changeHueFunc(pixel) {
  let r = pixel[0];
  let g = pixel[1];
  let b = pixel[2];

  for (let i = 0; i < 3; i++) {
    let newVal = hueMatrix[i][0] * r + hueMatrix[i][1] * g + hueMatrix[i][2] * b;
    pixel[i] = truncatePixelValue(newVal);
  }
}

export let constLib = {
  Pr: 0.299,
  Pg: 0.587,
  Pb: 0.114,

  truncatePixelValue: truncatePixelValueFunc,
  changeBrightnessContrast: changeBrightnessContrastFunc,
  changeSaturation: changeSaturationFunc,
  changeHue: changeHueFunc,

  computeContrastFactor: computeContrastFactorFunc,
  computeHueMatrix: computeHueMatrixFunc,
  contrastFactor: -1,
  hueMatrix: -1,
};

export function operation(pixels) {
  let pixel = pixels[0];

  if (brightness !== 0 || contrast !== 0) {
    if (contrastFactor === -1) {
      contrastFactor = computeContrastFactor(contrast);
    }
    changeBrightnessContrast(pixel);
  }

  if (saturation !== 0) {
    changeSaturation(pixel);
  }

  if (hue !== 0) {
    if (hueMatrix === -1) {
      hueMatrix = computeHueMatrix(hue);
    }
    changeHue(pixel);
  }

  return pixel;
}

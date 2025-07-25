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

import {Style, Stroke, Fill, Circle, Text} from 'ol/style';
import {MultiPoint} from 'ol/geom';
import {asArray as hexToRgb} from 'ol/color';
import constants from '@/utils/constants';

// -----

export function isCluster(feature) {
  let annot = feature.get('annot');
  if (!annot) {
    return;
  }
  return annot.count != null;
}

// -----

export function createStrokeStyle(color, opacity = 0.5) {
  let colorArray = hexToRgb(color);
  let colorWithOpacity = colorArray.slice();
  colorWithOpacity[3] = opacity;

  let stroke = new Stroke({color: colorWithOpacity, width: 2});

  return new Style({
    stroke,
    image: new Circle({radius:5, stroke: stroke})
  });
}

export function createLineStrokeStyle(color, opacity = 0.5) {
  let colorArray = hexToRgb(color);
  let colorWithOpacity = colorArray.slice();
  colorWithOpacity[3] = opacity;

  return new Style({stroke: new Stroke({color: colorWithOpacity, width: 5})});
}

// -----

function colorWithOpacity(color, opacity) {
  let colorWithOpacity = color.slice();
  colorWithOpacity[3] = opacity;
  return colorWithOpacity;
}

function createStroke(opacity = 0.5) {
  let color = (constants.ANNOTATION_STROKE_COLOR) ? constants.ANNOTATION_STROKE_COLOR : black;
  return new Stroke({color: colorWithOpacity(color, opacity), width: 2});
}

export function createColorStyle(color, opacity = 0.5) {
  let colorArray = hexToRgb(color);
  let colorWithOpacity = colorArray.slice();
  colorWithOpacity[3] = opacity;

  let fill = new Fill({color: colorWithOpacity});

  let circleStyle = new Circle({
    radius: 5,
    fill: new Fill({color: colorArray}),
    stroke: createStroke(1),
  });
  circleStyle.setOpacity(opacity);

  return new Style({
    fill,
    stroke: createStroke(opacity),
    image: circleStyle
  });
}

// -----

export function createColorLineStyle(color, opacity = 0.5) {
  let colorArray = hexToRgb(color);
  let colorWithOpacity = colorArray.slice();
  colorWithOpacity[3] = opacity;

  return new Style({stroke: new Stroke({color: colorWithOpacity, width: 3})});
}

// -----

let textFill = new Fill({color: '#fff'});
let textStroke = new Stroke({color: '#000', width: 3});

export function createTextStyle(text, fontSize = '22px', fill = textFill, stroke = textStroke) {
  return new Style({
    text: new Text({
      text,
      font: `${fontSize} Arial, sans-serif`,
      overflow: true,
      fill,
      stroke
    })
  });
}

// -----

let width = 2;

let blue = [0, 153, 255, 1];
let green = [51, 160, 79, 1];
let lightGreen = [17, 214, 76, 1];
let red = [200, 40, 40, 1];
let lightRed = [255, 56, 56, 1];
let white = [255, 255, 255, 1];
let black = [0, 0, 0, 1];

let blueStroke = new Stroke({color: blue, width: width});
let greenStroke = new Stroke({color: green, width: width + 1});
let lightGreenStroke = new Stroke({color: lightGreen, width: width});
let redStroke = new Stroke({color: red, width: width + 1});
let lightRedStroke = new Stroke({color: lightRed, width: width});
let whiteStroke = new Stroke({color: white, width: width + 2});

let selectStroke = blueStroke;
if (constants.ANNOTATION_STROKE_SELECT_COLOR) {
  let color = colorWithOpacity(constants.ANNOTATION_STROKE_SELECT_COLOR, 1);
  selectStroke = new Stroke({color, width});
}

export let trackedSelectStyles = [
  new Style({stroke: whiteStroke}),
  new Style({image: new Circle({radius: 6, stroke: whiteStroke})})
];

export let selectStyles = [
  new Style({stroke: whiteStroke}),
  new Style({stroke: selectStroke}),
  new Style({image: new Circle({radius: 6, stroke: selectStroke})})
];

export let verticesStyle = new Style({
  image: new Circle({radius: width + 1, fill: new Fill({color: blue})}),
  geometry: function (feature) {
    // return the coordinates of the first ring of the polygon
    let coordinates = feature.getGeometry().getCoordinates()[0];
    return new MultiPoint(coordinates);
  }
});

export let reviewedStyles = [
  new Style({stroke: greenStroke}),
  new Style({image: new Circle({radius: 6, stroke: greenStroke})})
];

export let reviewedSelectStyles = [
  new Style({stroke: whiteStroke}),
  new Style({stroke: lightGreenStroke}),
  new Style({image: new Circle({radius: 6, stroke: lightGreenStroke})})
];

export let rejectedStyles = [
  new Style({stroke: redStroke}),
  new Style({image: new Circle({radius: 6, stroke: redStroke})})
];

export let rejectedSelectStyles = [
  new Style({stroke: whiteStroke}),
  new Style({stroke: lightRedStroke}),
  new Style({image: new Circle({radius: 6, stroke: lightRedStroke})})
];

// -----

export function changeOpacity(style, opacity) {
  let stroke = style.getStroke();
  if (stroke) {
    stroke.getColor()[3] = opacity;
  }
  let fill = style.getFill();
  if (fill) {
    fill.getColor()[3] = opacity;
  }
  let image = style.getImage();
  if (image) {
    image.setOpacity(opacity);
  }
}

// -----

function createDefaultColor(name, hexaCode) {
  return {
    name,
    fill: new Fill({color: '#' + hexaCode}),
    hexaCode
  };
}

export const defaultColors = Object.freeze([
  createDefaultColor('black', '000000'),
  createDefaultColor('white', 'ffffff'),
  createDefaultColor('red', 'ff0000'),
  createDefaultColor('orange', 'ff6600'),
  createDefaultColor('yellow', 'ffff00'),
  createDefaultColor('green', '008000'),
  createDefaultColor('blue', '0000ff'),
  createDefaultColor('purple', '800080')
]);

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

export function isRectangle(geometry) {
  if (geometry.getLinearRingCount() !== 1 || geometry.getCoordinates()[0].length !== 5) {
    return false;
  }

  const coordinates = geometry.getCoordinates(false)[0];
  let prevX = coordinates[0][0];
  let prevY = coordinates[0][1];
  for (let i = 1; i <= 4; i++) {
    let x = coordinates[i][0];
    let y = coordinates[i][1];
    let xChanged = (x !== prevX);
    let yChanged = (y !== prevY);
    if (xChanged === yChanged) {
      return false;
    }
    prevX = x;
    prevY = y;
  }

  return true;
}

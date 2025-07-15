/*
 * Copyright (c) 2009-2023. Authors: see NOTICE file.
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

export function stripIDfromKey(key) {
  if (!key.startsWith('MSMDAD')) {
    return key;
  }

  const keywords = [
    'BiologicalBeing',
    'Block',
    'Case',
    'Dataset',
    'Image',
    'Observation',
    'Slide',
    'Specimen',
    'Study'
  ];
  const toRemove = [
    'blocks',
    'cases',
    'images',
    'observations',
    'slides',
    'specimens'
  ];

  let words = key.split('.').filter(word => !toRemove.includes(word));
  let striped = words.map(word => {
    if (keywords.some(keyword => word.startsWith(keyword))) {
      word = word.substr(0, word.lastIndexOf('_'));
    }

    return word;
  });

  return striped.join('.');
}

export function filterAutoCompletion(key, suggestions) {
  let subKeys = key.split('.');
  let subKey = subKeys[subKeys.length - 2];

  return suggestions.filter(suggestion => suggestion.startsWith(subKey));
}

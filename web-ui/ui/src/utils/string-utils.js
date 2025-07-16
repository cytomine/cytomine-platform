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

import _ from 'lodash';

/**
 * Return a Regexp based on the provided search string, and using * as a wildcard character.
 * @param {String} str The string entered by the user
 * @return {RegExp}
 */
export function getWildcardRegexp(str) {
  let escapedString = _.escapeRegExp(str); // escape all RegExp special characters (only * should have a special behaviour)
  return new RegExp('(' + escapedString.split('\\*').join('.*') + ')', 'i');
}

/**
 * Convert the string into a date time
 * @param {String} str The string value
 * @return {Date} The converted date time
 */
export function convertToDate(str) {
  let dateString = '';

  if (str.length === 8) {
    dateString = str.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');
  } else {
    dateString = str.replace(/(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})/, '$1-$2-$3 $4:$5:$6');
  }

  return new Date(dateString);
}

/**
 * Check if the string is a valid date
 * @param {String} str The string value
 * @return {boolean}
 */
export function isDate(str) {
  return (new Date(str)).toString() !== 'Invalid Date';
}

/**
 * Check if a string is a valid number.
 * @param {String} str The string entered by the user
 * @return {boolean}
 */
export function isNumeric(str) {
  return !isNaN(str);
}

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

// Helpers for vuex (inspired by https://github.com/davestewart/vuex-pathify)

import _ from 'lodash';

/**
 * @typedef {Object} Options
 * @property {String|Array} rootModuleProp  The name of the component property containing the path to the root module to
 *                                          use; this root path will be prefixed to the provided path
 * @property {Number} debounced The debounce delay
 */

/**
 * Getter for a computed property allowing to easily retrieve the value of a store property
 * This function must be called from a Vue.js component
 * @example
 * computed: {
 *   myProp: get('myModule/myProp')
 * }
 *
 * @example
 * computed: {
 *   myProp: sync('myProp', {rootModuleProp: 'modulePath'}),
 *   modulePath() {
 *     return 'myModule';
 *   }
 * }
 *
 * @param {String|Array<String>} path Store path to the property
 * @param {Options} [options] Options refining the way to get the property
 * @returns A computed getter for the targetted property
 */
export function get(path, options = {}) {
  path = arrayPath(path);
  return {
    get() {
      return getValue(this.$store, fullPath(path, this, options));
    },
  };
}

/**
 * Provides getter and setter for a computed property that should be synchronized with a store property
 * This function must be called from a Vue.js component
 * @example
 * computed: {
 *   myProp: sync('myModule/myProp')
 * }
 *
 * @param {String|Array<String>} path Store path to the property
 * @param {Options} [options] Options refining the way to sync the property
 * @returns Computed getter and setter for the targetted property
 */
export function sync(path, options = {}) {
  path = arrayPath(path);
  return {
    ...get(path, options),
    set: debounce(function (value) {
      this.$store.commit(getMutationName(fullPath(path, this, options)), value);
    }, options)
  };
}

/**
 * Provides getter and setter for a computed property that should be synchronized with a simple filter
 * stored in Vuex
 * This function must be called from a Vue.js component and the targetted module should be constructed as shown
 * in the example
 * @example
 * // Vue.js component
 * computed: {
 *   myFilter: sync('myModule', 'myFilter'),
 * }
 * // myModule structure
 * namespaced: true,
 * state: {
 *   filters: {
 *     myFilter: null // null value for inactive filter
 *   }
 * },
 * mutations: {
 *   setFilter(state, {filterName, propValue}) {
 *     state.filters[filterName] = propValue;
 *   },
 * }
 *
 *
 * @param {String|Array<String>} modulePath Path to the store module containing the filters
 * @param {String} filterName The name of the filter property
 * @param {Options} [options] Options refining the way to sync the filter
 * @returns Computed getter and setter for the targetted filter
 */
export function syncFilter(modulePath, filterName, options = {}) {
  modulePath = arrayPath(modulePath);

  return {
    get() {
      return getValue(this.$store, fullPath(modulePath, this, options)).filters[filterName];
    },
    set(propValue) {
      let path = fullPath(modulePath, this, options);
      this.$store.commit(path.join('/') + '/setFilter', {filterName, propValue});
    }
  };
}

/**
 * Provides getter and setter for a computed property that should be synchronized with a bounds filter (min, max bounds)
 * stored in Vuex
 * This function must be called from a Vue.js component, that also defines a property giving the max upper bound, and the
 * targetted module should be constructed as shown in the example
 * @example
 * // Vue.js component
 * computed: {
 *   myFilter: sync('myModule', 'myFilter', 'maxValue'),
 *   maxValue() {return 15;}
 * }
 * // myModule structure
 * namespaced: true,
 * state: {
 *   filters: {
 *     myFilter: null // null value for inactive filter (computed property will be equal to [0, maxValue])
 *   }
 * },
 * mutations: {
 *   setFilter(state, {filterName, propValue}) {
 *     state.filters[filterName] = propValue;
 *   },
 * }
 *
 *
 * @param {String|Array<String>} modulePath Path to the store module containing the filters
 * @param {String} filterName The name of the filter property
 * @param {String} maxProp The name of the component property containing the max allowed value
 * @param {Options} [options] Options refining the way to sync the filter
 * @returns Computed getter and setter for the targetted filter
 */
export function syncBoundsFilter(modulePath, filterName, maxProp, options = {}) {
  modulePath = arrayPath(modulePath);

  return {
    get() {
      let value = getValue(this.$store, fullPath(modulePath, this, options)).filters[filterName];
      return value ? value : [0, this[maxProp]];
    },
    set: debounce(function (bounds) {
      let path = fullPath(modulePath, this, options);
      let propValue = bounds[0] !== 0 || bounds[1] !== this[maxProp] ? bounds : null;
      this.$store.commit(path.join('/') + '/setFilter', {filterName, propValue});
    }, options)
  };
}

/**
 * Provides getter and setter for a computed property that should be synchronized with a multiselect filter stored in Vuex
 * This function must be called from a Vue.js component, that also defines a property giving the available options, and the
 * targetted module should be constructed as shown in the example
 * @example
 * // Vue.js component
 * computed: {
 *   myFilter: sync('myModule', 'myFilter', 'availableOptions'),
 *   availableOptions() {return [1, 2, 3];}
 * }
 * // myModule structure
 * namespaced: true,
 * state: {
 *   filters: {
 *     myFilter: null // null value for inactive filter (computed property will be equal to copy of available options)
 *   }
 * },
 * mutations: {
 *   setFilter(state, {filterName, propValue}) {
 *     state.filters[filterName] = propValue;
 *   },
 * }
 *
 *
 * @param {String|Array<String>} path Path to the store module containing the filters
 * @param {String} filterName The name of the filter property
 * @param {String} optionsProp The name of the component property containing the available options
 * @param {Options} [options] Options refining the way to sync the filter
 * @returns Computed getter and setter for the targetted filter
 */
export function syncMultiselectFilter(modulePath, filterName, optionsProp, options = {}) {
  modulePath = arrayPath(modulePath);

  return {
    get() {
      let value = getValue(this.$store, fullPath(modulePath, this, options)).filters[filterName];
      return value ? value : this[optionsProp].slice();
    },
    set: debounce(function (selectedOptions) {
      let path = fullPath(modulePath, this, options);
      let propValue = (selectedOptions.length === this[optionsProp].length) ? null : selectedOptions;
      this.$store.commit(path.join('/') + '/setFilter', {filterName, propValue});
    }, options)
  };
}

// wrapper for lodash debounce that debounces the function iff the debounce delay specified in options is > 0
function debounce(fct, options) {
  if (options.debounce) {
    return _.debounce(fct, options.debounce);
  } else {
    return fct;
  }
}

function arrayPath(path) {
  if (!path) {
    return [];
  }
  if (Array.isArray(path)) {
    return path;
  }
  if (typeof path === 'string') {
    return path.split('/');
  }
  throw new Error('Path must be a string or an array');
}

function fullPath(path, component, options) {
  if (options.rootModuleProp) {
    path = arrayPath(component[options.rootModuleProp]).concat(path);
  }
  return path;
}

function getValue(store, path) {
  let obj = store.state;
  for (let i = 0; i < path.length; i++) {
    obj = obj[path[i]];
  }
  return obj;
}

function getMutationName(path) {
  let variableName = path[path.length - 1];
  let modulePath = path.slice(0, path.length - 1);
  return modulePath.join('/') + '/set' + variableName.charAt(0).toUpperCase() + variableName.slice(1);
}

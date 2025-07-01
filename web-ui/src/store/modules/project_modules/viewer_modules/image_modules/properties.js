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

import {defaultColors} from '@/utils/style-utils.js';
import {PropertyCollection} from 'cytomine-client';
import constants from '@/utils/constants';

export default {
  state() {
    return {
      idImage: 0,
      propertiesKeys: null,
      selectedPropertyKey: null,
      selectedPropertyColor: defaultColors[0],
      selectedPropertyValues: {},
    };
  },

  mutations: {
    setIdImage(state, id) {
      state.idImage = id;
    },

    setPropertiesKeys(state, keys) {
      state.propertiesKeys = keys;
    },

    setSelectedPropertyKey(state, value) {
      state.selectedPropertyKey = value;
    },

    setSelectedPropertyColor(state, value) {
      state.selectedPropertyColor = value;
    },

    setSelectedPropertyValues(state, properties) {
      state.selectedPropertyValues = properties;
    },

    addSelectedPropertyValues(state, newValues) {
      state.selectedPropertyValues = {...state.selectedPropertyValues, ...newValues};
    }
  },

  actions: {
    async initialize({commit, rootState}, {image}) {
      commit('setIdImage', image.id);

      let [propertiesKeys, projectProperties] = await Promise.all([
        fetchPropertiesKeys(image.id),
        PropertyCollection.fetchAll({object: rootState.currentProject.project})
      ]);

      let selectedPropertyKey = null;
      let defaultPropertyProp = projectProperties.array.find(prop => prop.key === constants.DEFAULT_PROPERTY_KEY);
      if (defaultPropertyProp && propertiesKeys.includes(defaultPropertyProp.value)) {
        selectedPropertyKey = defaultPropertyProp.value;
      }

      commit('setPropertiesKeys', propertiesKeys);
      commit('setSelectedPropertyKey', selectedPropertyKey);
    },

    async setSelectedPropertyKey({state, commit, getters}, value) {
      let selectedLayers = getters.selectedLayers;

      let properties = {};
      if (value && selectedLayers) {
        for (let layer of selectedLayers) {
          let layerValues = await fetchLayerPropertiesValues(layer.id, state.idImage, value);
          properties = {...properties, ...layerValues};
        }
      }

      commit('setSelectedPropertyValues', properties);
      commit('setSelectedPropertyKey', value);
    },

    async addLayer({state, commit}, layer) {
      let key = state.selectedPropertyKey;
      let propValues = {};
      if (key) {
        propValues = await fetchLayerPropertiesValues(layer.id, state.idImage, key);
      }
      commit('addSelectedPropertyValues', propValues);
    },

    async setImageInstance({commit, dispatch}, {image}) {
      commit('setIdImage', image.id);
      await dispatch('refreshProperties');
    },

    async refreshProperties({state, commit, dispatch}) {
      let keys = await fetchPropertiesKeys(state.idImage);
      commit('setPropertiesKeys', keys);

      let currentKey = state.selectedPropertyKey;
      let newKey = keys.includes(currentKey) ? currentKey : null;
      dispatch('setSelectedPropertyKey', newKey);
    },

    async refreshData({dispatch}) {
      await dispatch('refreshProperties');
    }
  }
};

async function fetchLayerPropertiesValues(idLayer, idImage, key) {
  if (idLayer === -1) {
    return []; // currently not possible to retrieve properties of review layer (TODO in core)
  }

  let propertiesValues = await PropertyCollection.fetchPropertiesValuesAndPositions(
    idLayer,
    idImage,
    key
  );

  // if several properties with target key for an annotation, concatenate their values
  let properties = {};
  propertiesValues.forEach(propVal => {
    if (!properties[propVal.idAnnotation]) {
      properties[propVal.idAnnotation] = propVal.value;
    } else {
      properties[propVal.idAnnotation] += '; ' + propVal.value;
    }
  });

  return properties;
}

async function fetchPropertiesKeys(idImage) {
  let data = await PropertyCollection.fetchKeysAnnotationProperties(null, idImage);
  return data;
}

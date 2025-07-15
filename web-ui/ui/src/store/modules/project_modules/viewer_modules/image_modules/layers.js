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

export default {
  state() {
    return {
      selectedLayers: null
    };
  },

  mutations: {
    addLayer(state, layer) {
      if (!state.selectedLayers) {
        state.selectedLayers = [];
      }
      state.selectedLayers.push({...layer});
    },

    removeLayer(state, indexLayer) {
      state.selectedLayers.splice(indexLayer, 1);
    },

    toggleLayerVisibility(state, indexLayer) {
      let layer = state.selectedLayers[indexLayer];
      layer.visible = !layer.visible;
    },

    toggleLayerDrawOn(state, indexLayer) {
      let layer = state.selectedLayers[indexLayer];
      layer.drawOn = !layer.drawOn;
    },

    filterSelectedLayers(state, idLayers) {
      if (!state.selectedLayers) {
        return;
      }
      state.selectedLayers = state.selectedLayers.filter(layer => idLayers.includes(layer.id));
    }
  },

  actions: {
    async addLayer({commit}, layer) {
      commit('addLayer', layer);
    },

    toggleLayerVisibility({state, commit}, indexLayer) {
      commit('toggleLayerVisibility', indexLayer);
      let layer = state.selectedLayers[indexLayer];
      if (!layer.visible) {
        commit('removeLayerFromSelectedFeatures', {layer});
      }
    },

    removeLayer({state, commit}, indexLayer) {
      let layer = state.selectedLayers[indexLayer];
      commit('removeLayer', indexLayer);
      commit('removeLayerFromSelectedFeatures', {layer});
    }
  },

  getters: {
    selectedLayers: state => state.selectedLayers // expose in getter function because properties module need access to this value
  }
};

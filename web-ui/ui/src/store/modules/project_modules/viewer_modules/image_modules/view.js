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
      zoom: null, // will be initialized to appropriate value (depending on container size) in CytomineImage
      center: [0, 0],
      rotation: 0,
      digitalZoom: true,
      overviewCollapsed: false,
      highlighted: false,
      scaleLineCollapsed: false,
    };
  },

  mutations: {
    setCenter(state, center) {
      state.center = center;
    },

    setZoom(state, zoom) {
      state.zoom = zoom;
    },

    setRotation(state, rotation) {
      state.rotation = rotation;
    },

    setDigitalZoom(state, digitalZoom) {
      state.digitalZoom = digitalZoom;
    },

    setOverviewCollapsed(state, value) {
      state.overviewCollapsed = value;
    },

    setScaleLineCollapsed(state, value) {
      state.scaleLineCollapsed = value;
    },

    setHighlighted(state, value) {
      state.highlighted = value;
    }
  },

  actions: {
    async initialize({commit}, {image}) {
      commit('setCenter', [image.width / 2, image.height / 2]);
    },
  }
};

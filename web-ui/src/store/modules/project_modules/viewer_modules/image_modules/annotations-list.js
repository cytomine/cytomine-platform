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

import Vue from 'vue';

export default {
  state() {
    return {
      open: false,
      displayType: 'TERM', // TERM, TRACK
      currentPages: {}, // mapping of type {idProp: currentPage}
      selectedTermsIds: [],
      selectedTracksIds: []
    };
  },

  mutations: {
    setShowAnnotationsList(state, value) {
      state.open = value;
    },
    setDisplayType(state, value) {
      state.displayType = value;
    },
    setCurrentPage(state, {prop, page}) {
      Vue.set(state.currentPages, prop, page);
    },
    setSelectedTermsIds(state, termsIds) {
      state.selectedTermsIds = termsIds;
    },
    setSelectedTracksIds(state, tracksIds) {
      state.selectedTracksIds = tracksIds;
    }
  }
};

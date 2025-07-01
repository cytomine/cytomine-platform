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
      activeTool: 'select',

      termsNewAnnots: [],
      tracksNewAnnots: [],

      activeEditTool: null,
      ongoingEdit: false
    };
  },

  mutations: {
    activateTool(state, tool) {
      state.activeTool = tool;
    },

    activateEditTool(state, tool) {
      state.activeEditTool = tool;
    },

    setOngoingEdit(state, value) {
      state.ongoingEdit = value;
    },

    setTermsNewAnnots(state, terms) {
      state.termsNewAnnots = terms;
    },

    filterTermsNewAnnots(state, terms) { // keep only the terms that still exist
      let idTerms = terms.map(term => term.id);
      state.termsNewAnnots = state.termsNewAnnots.filter(id => idTerms.includes(id));
    },

    setTracksNewAnnots(state, tracks) {
      state.tracksNewAnnots = tracks;
    },

    filterTracksNewAnnots(state, tracks) {
      let idTracks = tracks.map(track => track.id);
      state.tracksNewAnnots = state.tracksNewAnnots.filter(id => idTracks.includes(id));
    }
  },

  actions: {
    activateTool({state, commit}, tool) {
      let previousTool = state.activeTool;
      if (previousTool === 'select' && tool !== 'select') {
        commit('clearSelectedFeatures');
        commit('activateEditTool', {tool: null});
      }
      commit('activateTool', tool);
    },

    refreshData({commit, getters, rootGetters}) {
      commit('filterTermsNewAnnots', rootGetters['currentProject/terms'] || []);
      commit('filterTracksNewAnnots', getters.tracks || []);
    },
  }
};

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

import {TrackCollection} from 'cytomine-client';

export default {
  state() {
    return {
      idImage: 0,
      tracks: [],
    };
  },

  mutations: {
    setIdImage(state, id) {
      state.idImage = id;
    },

    setTracks(state, keys) {
      state.tracks = keys;
    },
  },

  actions: {
    async initialize({commit}, {image}) {
      commit('setIdImage', image.id);

      let [tracks] = await Promise.all([
        fetchTracks(image.id)
      ]);

      commit('setTracks', tracks);
      commit('setWrappedTracks', tracks);
    },

    async setImageInstance({commit, dispatch}, {image}) {
      commit('setIdImage', image.id);
      await dispatch('refreshTracks');
    },

    async refreshTracks({state, commit}) {
      let tracks = await fetchTracks(state.idImage);
      commit('filterTracksNewAnnots', tracks || []);
      commit('setTracks', tracks);
      commit('setWrappedTracks', tracks);
    },

    async refreshData({dispatch}) {
      await dispatch('refreshTracks');
    }
  },

  getters: {
    tracks: state => state.tracks
  }
};

async function fetchTracks(idImage) {
  let data = (await TrackCollection.fetchAll({filterKey: 'imageinstance', filterValue: idImage})).array;
  return data;
}

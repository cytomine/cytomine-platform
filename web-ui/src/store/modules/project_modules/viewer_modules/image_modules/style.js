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

import {createColorStyle, createColorLineStyle, changeOpacity, createStrokeStyle, createLineStrokeStyle} from '@/utils/style-utils.js';

let initialTermsOpacity = 1;
let initialTracksOpacity = 1;
let initialLayersOpacity = 0.5;

export default {
  state() {
    return {
      terms: null,

      displayNoTerm: true,
      noTermOpacity: initialTermsOpacity,
      noTermStyle: createColorStyle('#fff', initialLayersOpacity * initialTermsOpacity),

      defaultStyle: createColorStyle('#fff', initialLayersOpacity),
      multipleTermsStyle: createColorStyle('#fff', initialLayersOpacity),

      layersOpacity: initialLayersOpacity,

      wrappedTracks: null,
      multipleTracksStyle: createStrokeStyle('#fff', initialLayersOpacity)
    };
  },

  mutations: {
    addTerm(state, term) {
      state.terms.push(formatTerm(term, state.layersOpacity));
    },

    setTerms(state, terms) {
      state.terms = terms;
    },

    setWrappedTracks(state, tracks) {
      state.wrappedTracks = formatTracks(tracks, state.layersOpacity, state.wrappedTracks || []);
    },

    toggleTermVisibility(state, indexTerm) {
      let term = state.terms[indexTerm];
      term.visible = !term.visible;
    },

    setDisplayNoTerm(state, value) {
      state.displayNoTerm = value;
    },

    setTermOpacity(state, {indexTerm, opacity}) {
      let term = state.terms[indexTerm];
      term.opacity = opacity;
      changeOpacity(term.olStyle, state.layersOpacity * opacity);
      changeOpacity(term.olLineStyle, state.layersOpacity * opacity);
    },

    setNoTermOpacity(state, opacity) {
      state.noTermOpacity = opacity;
      changeOpacity(state.noTermStyle, state.layersOpacity * opacity);
    },

    resetTermOpacities(state) {
      state.terms.forEach(term => {
        term.opacity = initialTermsOpacity;
        changeOpacity(term.olStyle, state.layersOpacity * initialTermsOpacity);
        changeOpacity(term.olLineStyle, state.layersOpacity * initialTermsOpacity);
      });
      state.noTermOpacity = initialTermsOpacity;
      changeOpacity(state.noTermStyle, state.layersOpacity * state.noTermOpacity);
    },

    setLayersOpacity(state, opacity) {
      state.layersOpacity = opacity;
      if (state.terms) {
        state.terms.forEach(term => {
          changeOpacity(term.olStyle, opacity * term.opacity);
          changeOpacity(term.olLineStyle, opacity * term.opacity);
        });
      }
      changeOpacity(state.noTermStyle, opacity * state.noTermOpacity);
      changeOpacity(state.multipleTermsStyle, opacity);
      changeOpacity(state.defaultStyle, opacity);
      if (state.wrappedTracks) {
        state.wrappedTracks.forEach(track => {
          changeOpacity(track.olStyle, opacity * track.opacity);
          changeOpacity(track.olLineStyle, opacity * track.opacity);
        });
      }
      changeOpacity(state.multipleTracksStyle, opacity);
    },
  },

  actions: {
    initialize({commit, getters, rootGetters}) {
      let terms = formatTerms(rootGetters['currentProject/terms'], initialLayersOpacity);
      commit('setTerms', terms);

      let tracks = formatTracks(getters.tracks, initialLayersOpacity);
      commit('setWrappedTracks', tracks);
    },

    toggleTermVisibility({state, commit}, indexTerm) {
      commit('toggleTermVisibility', indexTerm);
      let toggledTerm = state.terms[indexTerm];
      if (!toggledTerm.visible) {
        commit('removeTermFromSelectedFeatures', {idTerm: toggledTerm.id, terms: state.terms});
      }
    },

    setDisplayNoTerm({commit}, value) {
      commit('setDisplayNoTerm', value);
      if (!value) {
        commit('removeNoTermFromSelectedFeatures');
      }
    },

    async refreshData({state, commit, getters, rootGetters}) {
      let terms = formatTerms(rootGetters['currentProject/terms'], state.layersOpacity, state.terms);
      commit('setTerms', terms);

      let tracks = formatTracks(getters.tracks, state.layersOpacity, state.terms);
      commit('setWrappedTracks', tracks);
    }
  },

  getters: {
    termsMapping: state => {
      if (!state.terms) {
        return {};
      }

      return state.terms.reduce((mapping, term) => {
        mapping[term.id] = term;
        return mapping;
      }, {});
    },
    tracksMapping: state => {
      if (!state.wrappedTracks) {
        return {};
      }

      return state.wrappedTracks.reduce((mapping, track) => {
        mapping[track.id] = track;
        return mapping;
      }, {});
    },
    wrappedTracks: state => state.wrappedTracks,
    hiddenTermsIds: state => {
      if (!state.terms) {
        return [];
      }

      let list = state.terms.filter(term => !term.visible).map(term => term.id);
      if (!state.displayNoTerm) {
        list.push(0);
      }
      return list || [];
    },
  }
};


function formatTerms(terms, layersOpacity, previousTerms = []) {
  if (!terms) {
    return;
  }

  let result = [];
  let nbTerms = terms.length;
  for (let i = 0; i < nbTerms; i++) {
    let term = terms[i];
    let prevTerm = previousTerms.find(prevTerm => prevTerm.id === term.id);
    result.push(prevTerm ? prevTerm : formatTerm(term, layersOpacity));
  }
  return result;
}

function formatTerm(term, layersOpacity) {
  let result = {id: term.id};
  result.opacity = initialTermsOpacity;
  result.olStyle = createColorStyle(term.color, initialTermsOpacity * layersOpacity);
  result.olLineStyle = createColorLineStyle(term.color, initialTermsOpacity * layersOpacity);
  result.visible = true;
  result.color = term.color;
  return result;
}

function formatTracks(tracks, layersOpacity, previousTracks = []) {
  if (!tracks) {
    return;
  }

  let result = [];
  let nbTracks = tracks.length;
  for (let i = 0; i < nbTracks; i++) {
    let track = tracks[i];
    let prevTrack = previousTracks.find(prevTrack => prevTrack.id === track.id && prevTrack.color === track.color);
    result.push(prevTrack ? prevTrack : formatTrack(track, layersOpacity));
  }
  return result;
}

function formatTrack(track, layersOpacity) {
  let result = {id: track.id};
  result.opacity = initialTracksOpacity;
  result.olStyle = createStrokeStyle(track.color, initialTracksOpacity * layersOpacity);
  result.olLineStyle = createLineStrokeStyle(track.color, initialTracksOpacity * layersOpacity);
  result.color = track.color;
  return result;
}

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
      idImage: 0,
      nbBitsPerSample: 8,
      nbSamplesPerChannel: 1,

      apparentChannels: null,

      gammaPerApparentChannel: true,
      invertedPerApparentChannel: true,
      histogramLogScale: true,
      intensitiesByMinMax: true,

      filter: null
    };
  },

  mutations: {
    setIdImage(state, id) {
      state.idImage = id;
    },

    setNbBitsPerSample(state, value) {
      state.nbBitsPerSample = value;
    },

    setNbSamplesPerChannel(state, value) {
      state.nbSamplesPerChannel = value;
    },

    setGammaPerApparentChannel(state, value) {
      state.gammaPerApparentChannel = value;
    },

    setInvertedPerApparentChannel(state, value) {
      state.invertedPerApparentChannel = value;
    },

    setHistogramLogScale(state, value) {
      state.histogramLogScale = value;
    },

    setIntensitiesByMinMax(state, value) {
      state.intensitiesByMinMax = value;
    },

    setFilter(state, filter) {
      state.filter = filter;
    },

    setApparentChannels(state, apparentChannels) {
      state.apparentChannels = apparentChannels;
    },

    setApparentChannelVisibility(state, {indexApparentChannel, visible}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.visible = visible;
    },
    setApparentChannelColor(state, {indexApparentChannel, color, isColormap}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.color = color;
      channel.isColormap = isColormap;
    },
    setApparentChannelBounds(state, {indexApparentChannel, bounds}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.bounds = bounds;
    },
    setApparentChannelGamma(state, {indexApparentChannel, gamma}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.gamma = gamma;
    },
    setApparentChannelInverted(state, {indexApparentChannel, inverted}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.inverted = inverted;
    },

    resetApparentChannel(state, indexApparentChannel) {
      let channel = state.apparentChannels[indexApparentChannel];
      resetApparentChannel(channel, state.nbBitsPerSample);
    },
    resetApparentChannels(state) {
      state.apparentChannels.forEach(apparentChannel => {
        resetApparentChannel(apparentChannel, state.nbBitsPerSample);
      });
    },

    resetApparentChannelIntensities(state, indexApparentChannel) {
      let channel = state.apparentChannels[indexApparentChannel];
      resetApparentChannel(channel, state.nbBitsPerSample, true);
    },
    resetApparentChannelsIntensities(state) {
      state.apparentChannels.forEach(apparentChannel => {
        resetApparentChannel(apparentChannel, state.nbBitsPerSample, true);
      });
    },

    adjustToImage(state, indexApparentChannel) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.bounds = channel.imageBounds;
    },
    adjustAllToImage(state) {
      state.apparentChannels.forEach(apparentChannel => {
        if (apparentChannel.visible) {
          apparentChannel.bounds = apparentChannel.imageBounds;
        }
      });
    },
    adjustToSlice(state, {indexApparentChannel, bounds}) {
      let channel = state.apparentChannels[indexApparentChannel];
      channel.bounds = bounds;
    },
    adjustAllToSlice(state, allBounds) {
      state.apparentChannels.forEach((apparentChannel, i) => {
        if (apparentChannel.visible) {
          apparentChannel.bounds = allBounds[i];
        }
      });
    },

    setChannelsVisibility(state, channels) {
      state.apparentChannels.forEach(ac => {
        ac.visible = (channels.includes(ac.channel));
      });
    },

    resetImageColorManipulationSettings(state) {
      state.gammaPerApparentChannel = true;
      state.invertedPerApparentChannel = true;
      state.histogramLogScale = true;
      state.intensitiesByMinMax = true;
    },
  },

  actions: {
    async initialize({commit, dispatch}, {image, slices}) {
      commit('setIdImage', image.id);
      commit('setNbBitsPerSample', image.bitPerSample);
      commit('setNbSamplesPerChannel', image.samplePerPixel);
      await dispatch('refreshApparentChannels', {image});
      let channels = slices.map(slice => slice.channel);
      commit('setChannelsVisibility', channels);
    },
    async setImageInstance({commit, dispatch}, {image}) {
      commit('setIdImage', image.id);
      commit('setNbBitsPerSample', image.bitPerSample);
      commit('setNbSamplesPerChannel', image.samplePerPixel);
      await dispatch('refreshApparentChannels', {image});
    },
    async refreshApparentChannels({commit}, {image}) {
      let apparentChannels = formatApparentChannels(
        await fetchApparentChannels(image),
        image.bitPerSample
      );
      commit('setApparentChannels', apparentChannels);
    },

    resetColorManipulation({commit}) {
      commit('resetApparentChannels');
      commit('resetImageColorManipulationSettings');
      commit('setFilter', null);
    },

    setApparentChannelVisibility({commit, state, dispatch}, {indexApparentChannel, visible}) {
      let apparentChannel = state.apparentChannels[indexApparentChannel];
      let count = state.apparentChannels.filter(ac =>
        ac.visible && ac.channel === apparentChannel.channel
      ).length;
      if (visible && count === 0) {
        dispatch('addActiveSliceChannel', {channel: apparentChannel.channel});
      } else if (!visible && count === 1) {
        dispatch('removeActiveSliceChannel', {channel: apparentChannel.channel});
      }
      commit('setApparentChannelVisibility', {indexApparentChannel, visible});
    },
    async setActiveSlice({commit}, slice) {
      commit('setChannelsVisibility', [slice.channel]);
    },
    async setActiveSliceByPosition({commit}, {channel}) {
      commit('setChannelsVisibility', [channel]);
    },
    async setActiveSlicesByPosition({commit}, {channels}) {
      commit('setChannelsVisibility', channels);
    },
    // async setActiveSliceByRank({dispatch}, rank) {
    //   //TODO
    // },
    // async setActiveSlicesByRank({dispatch}, ranks) {
    //   //TODO
    // },
  },

  getters: {
    tileRequestParams: (state) => {
      let params = {};
      if (state.filter !== null) {
        params.filters = state.filter;
      }
      let visibleApparentChannels = [];
      if (state.apparentChannels) {
        visibleApparentChannels = state.apparentChannels.filter(
          (apparentChannel) => apparentChannel.visible
        );
      }
      if (visibleApparentChannels.length > 0) {
        params.channels = [];
        // eslint-disable-next-line camelcase
        params.minIntensities = [];
        // eslint-disable-next-line camelcase
        params.maxIntensities = [];
        params.gammas = [];
        params.colormaps = [];

        visibleApparentChannels.forEach(ac => {
          params.channels.push(ac.index);
          params.minIntensities.push(ac.bounds.min);
          params.maxIntensities.push(ac.bounds.max);
          params.gammas.push(ac.gamma);
          params.colormaps.push(formatColormapRequestParam(ac.color, ac.inverted));
        });
      }
      return params;
    },
  }
};

function formatColormapRequestParam(color, inverted) {
  if (color === null) {
    color = 'DEFAULT';
  }
  if (inverted) {
    return `!${color}`;
  }
  return color;
}

function defaultBounds(nBits) {
  return {min: 0, max: 2 ** nBits - 1};
}

function autoAdjustBounds(nBits, {min, max}) {
  if (nBits > 8) {
    return {min, max};
  }
  return {min: 0, max:255};
}

async function fetchApparentChannels(image) {
  return await image.fetchChannelHistogramBounds();
}

function formatApparentChannels(apparentChannels, nBits) {
  if (!apparentChannels) {
    return;
  }

  return apparentChannels.map(apparentChannel =>
    formatApparentChannel(apparentChannel, nBits)
  );
}

function formatApparentChannel(apparentChannel, nBits) {
  let result = {index: apparentChannel.apparentChannel};
  result.channel = apparentChannel.channel;
  result.sample = apparentChannel.sample;

  result.defaultColor = apparentChannel.color;
  result.color = apparentChannel.color;
  result.isColormap = false;
  result.visible = true;
  result.gamma = 1.0;
  result.inverted = false;
  result.imageBounds = {
    min: apparentChannel.minimum,
    max: apparentChannel.maximum
  };
  result.bounds = autoAdjustBounds(nBits, result.imageBounds);

  return result;
}

function resetApparentChannel(apparentChannel, nBits, onlyIntensities = false) {
  if (!onlyIntensities) {
    apparentChannel.color = apparentChannel.defaultColor;
    apparentChannel.isColormap = false;
    apparentChannel.visible = true;
  }

  apparentChannel.gamma = 1.0;
  apparentChannel.inverted = false;
  apparentChannel.bounds = defaultBounds(nBits);
}

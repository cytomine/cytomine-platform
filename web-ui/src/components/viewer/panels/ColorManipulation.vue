<!-- Copyright (c) 2009-2022. Authors: see NOTICE file.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.-->


<template>
<div>
  <h1>{{$t('colors')}}</h1>
  <b-message v-if="error" type="is-danger" has-icon icon-size="is-small" size="is-small">
    <p> {{ $t('unexpected-error-info-message') }} </p>
  </b-message>
  <template v-else>
    <table class="table">
      <thead>
        <tr>
          <th class="checkbox-column"><span class="far fa-eye"></span></th>
          <th class="name-column">
            <b-input v-model="searchString" :placeholder="$t('search-placeholder')" size="is-small" expanded />
          </th>
          <th class="bounds-column">
            <div class="buttons has-addons is-right">
              <button
                v-if="isImageMultidimensional"
                v-tooltip="$t('button-adjust-slice')"
                @click="adjustAllToSlice"
                class="button is-small"
                :disabled="areBoundsAdjustedToSlice"
              >
                <span class="icon is-small">
                  <i class="fas fa-magic"></i>
                  <span class="subscript">ZT</span>
                </span>
              </button>
              <button
                v-tooltip="adjustImageLabel"
                @click="adjustAllToImage"
                class="button is-small"
                :disabled="areBoundsAdjustedToImage"
              >
                <span class="icon is-small">
                  <i class="fas fa-magic"></i>
                </span>
              </button>
              <button
                v-tooltip="$t('button-reset')"
                @click="resetAll"
                class="button is-small"
                :disabled="areBoundsDefault"
              >
                <span class="icon is-small">
                  <i class="fas fa-tint-slash"></i>
                </span>
              </button>
            </div>
          </th>
          <th class="settings-column">
            <b-dropdown position="is-bottom-left">
              <template #trigger="{ active }">
                <button class="button is-small" :class="{'is-selected': active}">
                  <span class="icon is-small">
                    <i class="fas fa-cogs"></i>
                  </span>
                </button>
              </template>

              <b-dropdown-item custom>
                <b-field :label="$t('histogram-scale')" custom-class="is-small">
                  <div class="block">
                    <b-radio v-model="histogramLogScale" :native-value="false" size="is-small">
                      {{$t('linear')}}
                    </b-radio>
                    <b-radio v-model="histogramLogScale" :native-value="true" size="is-small">
                      {{$t('logarithmic')}}
                    </b-radio>
                  </div>
                </b-field>
                <b-field :label="$t('intensities-manipulation')" custom-class="is-small">
                  <div class="block">
                    <b-radio v-model="intensitiesByMinMax" :native-value="true" size="is-small">
                      {{$t('min-max')}}
                    </b-radio>
                    <b-radio v-model="intensitiesByMinMax" :native-value="false" size="is-small">
                      {{$t('brightness')}}/{{$t('contrast')}}
                    </b-radio>
                  </div>
                </b-field>
              </b-dropdown-item>
            </b-dropdown>
          </th>
        </tr>
      </thead>
      <tbody>
        <template v-for="mc in filteredManipulableChannels">
          <tr :key="`channel-${image.id}-${mc.index}`">
            <td class="checkbox-column">
              <b-checkbox
                size="is-small"
                :value="mc.visible"
                @input="setApparentChannelVisibility(mc.index, $event)"
                :disabled="isApparentChannelVisibilityDisabled(mc)"
              />
            </td>
            <td class="name-column">
              <cytomine-channel
                :name="mc.name"
                :color="mc.color"
                :default-color="mc.defaultColor"
                :channel-index="mc.channel"
                :sample-index="mc.sample"
                :nb-samples-per-channel="image.samplePerPixel"
                :editable-color="true"
                @setColor="setApparentChannelColor(mc.index, $event)"
                @click="toggleLookUpTableDetails(mc.index)"
              />
            </td>
            <td class="bounds-column">
              <editable-text-bound
                :bounds="mc.bounds"
                :default-bounds="defaultBounds"
                @setBounds="setApparentChannelBounds(mc.index, $event)"
                @editing="toggleLookUpTableDetailsIfNotVisible(mc.index)"
              />
            </td>
            <td class="checkbox-column">
              <a
                role="button"
                @click.stop="toggleLookUpTableDetails(mc.index)">
                <i class="fas fa-chevron-down" v-if="isVisibleLookUpTableDetails(mc.index)"></i>
                <i class="fas fa-chevron-up" v-else></i>
              </a>
            </td>
          </tr>
          <tr
            :key="`channel-${image.id}-${mc.index}-details`"
            v-if="isVisibleLookUpTableDetails(mc.index)"
          >
            <td colspan="4">
              <div class="chart-container channel-histogram">
                <histogram-chart
                  :log-scale="histogramLogScale"
                  :color="mc.color"
                  :histogram="mc.histogram.histogram"
                  :n-bins="mc.histogram.nBins"
                  :first-bin="mc.histogram.firstBin"
                  :last-bin="mc.histogram.lastBin"
                  :default-bounds="defaultBounds"
                  :image-bounds="{min: mc.histogram.minimum, max:mc.histogram.maximum}"
                  :current-bounds="mc.bounds"
                  :gamma="mc.gamma"
                  :inverted="mc.inverted"
                  css-classes="chart"
                />
              </div>
              <adjustable-look-up-table
                :index="index"
                :adjust-image-label="adjustImageLabel"
                :show-adjust-buttons="manipulableChannels.length > 1"
                :intensities-by-min-max="intensitiesByMinMax"
                :default-bounds="defaultBounds"
                :gamma="mc.gamma"
                :inverted="mc.inverted"
                :bounds="mc.bounds"
                :image-bounds="mc.imageBounds"
                :slice-bounds="{min: mc.histogram.minimum, max: mc.histogram.maximum}"
                @setBounds="setApparentChannelBounds(mc.index, $event)"
                @adjustToImage="adjustToImage(mc.index)"
                @adjustToSlice="adjustToSlice(mc.index)"
                @reset="reset(mc.index)"
                @setGamma="setApparentChannelGamma(mc.index, $event)"
                @invert="setApparentChannelInverted(mc.index, $event)"
              />
            </td>
          </tr>
        </template>

      </tbody>
    </table>
    <b-field horizontal :label="$t('filter')" v-if="filters && filters.length > 0">
      <b-select v-model="selectedFilter" size="is-small" expanded>
        <option :value="null">{{$t('original-no-filter')}}</option>
        <option v-for="filter in filters" :key="filter.id" :value="filter.method">
          {{filter.name}}
        </option>
      </b-select>
    </b-field>
  </template>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import constants from '@/utils/constants';
import {getWildcardRegexp} from '@/utils/string-utils';
import {sameHistogramBounds} from '@/utils/histogram-utils';

import {ImageFilterProjectCollection} from 'cytomine-client';

import CytomineChannel from '@/components/viewer/panels/colors/CytomineChannel';
import AdjustableLookUpTable from '@/components/viewer/panels/colors/AdjustableLookUpTable';
import HistogramChart from '@/components/charts/HistogramChart';
import EditableTextBound from '@/components/viewer/panels/colors/EditableTextBound';

export default {
  name: 'color-manipulation',
  components: {
    AdjustableLookUpTable,
    CytomineChannel,
    HistogramChart,
    EditableTextBound
  },
  props: {
    index: String
  },
  data() {
    return {
      filters: null,
      histograms: [],
      loading: false,
      error: false,
      searchString: '',
      visibleLookUpTableDetails: [],
    };
  },
  computed: {
    project: get('currentProject/project'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    slices() {
      return this.imageWrapper.activeSlices;
    },
    sliceChannels() {
      return this.$store.getters[this.imageModule + 'channels'];
    },
    plane() {
      return {
        zStack: this.imageWrapper.activeSlices[0].zStack,
        time: this.imageWrapper.activeSlices[0].time
      };
    },
    activePanel() {
      return this.imageWrapper.activePanel;
    },
    apparentChannels() {
      return this.imageWrapper.colors.apparentChannels;
    },

    isImageMultidimensional() {
      return this.image.depth * this.image.duration > 1;
    },
    adjustImageLabel() {
      if (this.isImageMultidimensional) {
        return this.$t('button-adjust-image');
      }
      return this.$t('button-adjust');
    },
    defaultBounds() {
      return {min: 0, max: 2 ** this.image.bitPerSample - 1};
    },

    manipulableChannels() {
      return this.apparentChannels?.map(ac => {
        return {
          ...ac,
          histogram: this.histograms.find(h => h.apparentChannel === ac.index),
          name: this.sliceChannels[ac.channel].name
        };
      });
    },
    visibleManipulableChannels() {
      return this.manipulableChannels?.filter(mc => mc.visible);
    },
    nbVisibleManipulableChannels() {
      return this.visibleManipulableChannels?.length ?? 0;
    },
    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    filteredManipulableChannels() {
      if (!this.searchString) {
        return this.manipulableChannels;
      }
      return this.manipulableChannels.filter(mc => this.regexp.test(mc.name));
    },

    areBoundsAdjustedToImage() {
      const count = this.visibleManipulableChannels?.filter(mc =>
        sameHistogramBounds(mc.bounds, mc.imageBounds)
      ).length ?? 0;
      return count === this.nbVisibleManipulableChannels;
    },
    areBoundsAdjustedToSlice() {
      const count = this.visibleManipulableChannels?.filter(mc => {
        const histogram = mc.histogram;
        if (!histogram) {
          return false;
        }
        return sameHistogramBounds(mc.bounds, {min: histogram.minimum, max: histogram.maximum});
      }).length ?? 0;
      return count === this.nbVisibleManipulableChannels;
    },
    areBoundsDefault() {
      const count = this.visibleManipulableChannels?.filter(mc =>
        sameHistogramBounds(mc.bounds, this.defaultBounds) && !mc.inverted && mc.gamma === 1.0
      ).length ?? 0;
      return count === this.nbVisibleManipulableChannels;
    },

    selectedFilter: {
      get() {
        return this.imageWrapper.colors.filter;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setFilter', value);
      }
    },
    histogramLogScale: {
      get() {
        return this.imageWrapper.colors.histogramLogScale;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setHistogramLogScale', value);
      }
    },
    intensitiesByMinMax: {
      get() {
        return this.imageWrapper.colors.intensitiesByMinMax;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setIntensitiesByMinMax', value);
      }
    },
  },
  watch: {
    plane() {
      this.fetchHistograms();
    }
  },
  methods: {
    /* Apparent channels manipulation */
    setApparentChannelVisibility(indexApparentChannel, visible) {
      this.$store.dispatch(this.imageModule + 'setApparentChannelVisibility',
        {indexApparentChannel, visible}
      );
    },
    setApparentChannelColor(indexApparentChannel, color) {
      this.$store.commit(this.imageModule + 'setApparentChannelColor',
        {indexApparentChannel, color, isColormap: false}
      );
    },
    setApparentChannelGamma(indexApparentChannel, gamma) {
      this.$store.commit(this.imageModule + 'setApparentChannelGamma',
        {indexApparentChannel, gamma}
      );
    },
    setApparentChannelInverted(indexApparentChannel, inverted) {
      this.$store.commit(this.imageModule + 'setApparentChannelInverted',
        {indexApparentChannel, inverted}
      );
    },
    setApparentChannelBounds(indexApparentChannel, bounds) {
      this.$store.commit(this.imageModule + 'setApparentChannelBounds',
        {indexApparentChannel, bounds}
      );
    },
    reset(indexApparentChannel) {
      this.$store.commit(this.imageModule + 'resetApparentChannelIntensities',
        indexApparentChannel
      );
    },
    resetAll() {
      this.$store.commit(this.imageModule + 'resetApparentChannelsIntensities');
    },
    adjustToImage(indexApparentChannel) {
      this.$store.commit(this.imageModule + 'adjustToImage',
        indexApparentChannel
      );
    },
    adjustAllToImage() {
      this.$store.commit(this.imageModule + 'adjustAllToImage');
    },
    adjustToSlice(indexApparentChannel) {
      const mc = this.manipulableChannels[indexApparentChannel];
      const bounds = {min: mc.histogram.minimum, max: mc.histogram.maximum};
      this.$store.commit(this.imageModule + 'adjustToSlice',
        {indexApparentChannel, bounds}
      );
    },
    adjustAllToSlice() {
      this.$store.commit(this.imageModule + 'adjustAllToSlice',
        this.manipulableChannels.map(mc => {
          return {
            min: mc.histogram.minimum,
            max: mc.histogram.maximum
          };
        })
      );
    },

    /* UX logic */
    isApparentChannelVisibilityDisabled(manipulableChannel) {
      return (this.nbVisibleManipulableChannels <= 1 && manipulableChannel.visible)
        || (this.nbVisibleManipulableChannels >= constants.MAX_MERGEABLE_CHANNELS
          && !manipulableChannel.visible);
    },
    toggleLookUpTableDetails(indexApparentChannel) {
      const found = this.isVisibleLookUpTableDetails(indexApparentChannel);

      if (found) {
        const i = this.visibleLookUpTableDetails.indexOf(indexApparentChannel);
        this.visibleLookUpTableDetails.splice(i, 1);
      } else {
        this.visibleLookUpTableDetails.push(indexApparentChannel);
      }
    },
    isVisibleLookUpTableDetails(indexApparentChannel) {
      return this.visibleLookUpTableDetails.indexOf(indexApparentChannel) >= 0;
    },
    toggleLookUpTableDetailsIfNotVisible(indexApparentChannel) {
      if (this.isVisibleLookUpTableDetails(indexApparentChannel)) {
        return;
      }

      this.toggleLookUpTableDetails(indexApparentChannel);
    },

    /* Helpers */
    async fetchHistograms() {
      try {
        // TODO [EXPERIMENTAL - large set of merged channels]
        // As for now we only allow multiple slices with varying C and fixed Z,T, this request is OK.


        this.histograms = (await this.slices[0].fetchChannelHistograms({nBins: 256}));
        // if (this.image.apparentChannels <= constants.MAX_MERGEABLE_CHANNELS) {
        //   // As for now we only allow multiple slices with varying C and fixed Z,T, this request is OK.
        //   this.histograms = (await this.slices[0].fetchChannelHistograms({nBins: this.histogramNBins}));
        // }
        // else {
        //   this.histograms = (await this.slices[0].fetchHistogram({nBins: this.histogramNBins}));
        // }
      } catch (error) {
        console.log(error);
        this.error = true;
      }
    },
  },
  async created() {
    try {
      let filters = (await ImageFilterProjectCollection.fetchAll({filterKey: 'project', filterValue: this.project.id})).array;
      filters.forEach(filter => filter.prefix = filter.imagingServer + filter.baseUrl);
      let prefixes = filters.map(filter => filter.prefix);
      if (this.selectedFilter && !prefixes.includes(this.selectedFilter)) {
        this.selectedFilter = null; // if selected filter no longer present in collection, unselect it
      }
      this.filters = filters;
      await this.fetchHistograms();

      // Show LUT details by default if there is only 1 channel.
      if (this.manipulableChannels.length === 1) {
        this.toggleLookUpTableDetails(this.manipulableChannels[0].index);
      }

      this.loading = false;
    } catch (error) {
      console.log(error);
    }
  },
};
</script>

<style scoped>
.table {
  margin-bottom: 1em !important;
  font-size: 0.9em;
}

.table tbody {
  display: block;
  overflow-y: scroll;
  overflow-x: hidden;
  max-height: 30em; /* TODO */
}

.table thead tr {
  display: block;
}

th, ::v-deep td {
  padding: 0.25em !important;
  vertical-align: middle !important;
}

::v-deep .checkbox-column {
  min-width: 2.2em;
  text-align: center !important;
}

::v-deep .name-column {
  width: 60%;
}

::v-deep .bounds-column {
  width: 40%;
  text-align: right;
}

::v-deep .settings-column {
  min-width: 2.2em;
  font-weight: normal;
}

/** Settings **/
.settings-column ::v-deep .dropdown-content {
  padding: 0;
}

.settings-column .dropdown-menu .dropdown-item {
  padding: 0.375rem 0.5rem;
}

.button.is-selected {
  background-color: #6899d0;
  color: white;
  stroke: white;
}

/** Checkbox **/
::v-deep .checkbox .control-label {
  padding: 0 !important;
}

::v-deep .checkbox {
  margin: 0 !important;
  position: relative;
  top: 0.25em;
}

/** Buttons & icons **/
.bounds-column .button .icon:first-child:last-child {
  margin-left: calc(-0.175em - 1px);
  margin-right: calc(-0.175em - 1px);
}

.subscript, ::v-deep .subscript {
  position: relative;
  top: 0.65em;
  left: 1px;
  font-size: 0.7em;
  font-weight: 600;
}

/** Chart (histogram) **/
.chart-container {
  height: 10em;
  position: relative;
}
</style>

<style>
.channel-histogram .chart {
  position: absolute;
  width: 100%;
  height: 100%;
}
</style>

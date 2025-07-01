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
  <div class="image-controls-container" v-if="isImageMultidimensional">

    <!-- ----- CHANNELS ----- -->
    <div class="image-dimension" v-if="hasChannels">
      <strong class="image-dimension-name">C</strong>
      <template v-if="areChannelsMergeable">
        <b-select
          :value="currentChannelsIndexesOption"
          @input="setCurrentChannelsIndexes"
          size="is-small"
          class="channel-selector"
          expanded
        >
          <option :value="null">
            {{ $t('merged-channels')}}
            <template v-if="currentChannelsIndexesOption === null">
               ({{orderedCurrentChannels.length}}/{{image.channels}})
              (<span
                v-for="(channel, i) in orderedCurrentChannels"
                :key="`${image.id}-${channel.index}-name-list`"
              >
                <channel-name :channel="channel"/><template v-if="i !== currentChannelsIndexes.length - 1">|</template>
              </span>)
            </template>
          </option>
          <option
            v-for="channel in channelOptions"
            :key="`${image.id}-${channel.value}-opt`"
            :value="channel.value"
          >
            <channel-name :channel="channel" />
          </option>
        </b-select>

      </template>
      <template v-else>
        <image-controls-shift-buttons
          :index="index"
          :forward="false"
          :current="currentSlice.channel"
          :size="image.channels"
          dimension="channel"
          @shift="shift('channel', $event)"
        />

        <cytomine-slider
          v-model="currentChannelIndex"
          :max="image.channels - 1"
          :integer-only="true"
          class="image-dimension-slider"
        >
          <template v-if="hasChannelName" #default="{ value }">
            {{channelValue(value) || "?"}}
          </template>
        </cytomine-slider>

        <image-controls-shift-buttons
          :index="index"
          :forward="true"
          :current="currentSlice.channel"
          :size="image.channels"
          dimension="channel"
          @shift="shift('channel', $event)"
        />
      </template>
    </div>


    <!-- ----- DEPTH ----- -->
    <div class="image-dimension" v-if="hasDepth">
      <strong class="image-dimension-name">Z</strong>
      <image-controls-shift-buttons
          :index="index"
          :forward="false"
          :current="currentSlice.zStack"
          :size="image.depth"
          dimension="zStack"
          @shift="shift('zStack', $event)"
      />

      <cytomine-slider
        v-model="currentZStackIndex"
        :max="image.depth - 1"
        :integer-only="true"
        class="image-dimension-slider"
      >
        <template v-if="hasZName">
          {{ zValue || "?" }}
        </template>
      </cytomine-slider>

      <image-controls-shift-buttons
          :index="index"
          :forward="true"
          :current="currentSlice.zStack"
          :size="image.depth"
          dimension="zStack"
          @shift="shift('zStack', $event)"
      />
    </div>



    <!-- ----- DURATION ----- -->
    <div class="image-dimension" v-if="hasDuration">
      <strong class="image-dimension-name">T</strong>
      <image-controls-shift-buttons
          :index="index"
          :forward="false"
          :current="currentSlice.time"
          :size="image.duration"
          dimension="time"
          @shift="shift('time', $event)"
      />

<!--      <div class="buttons has-addons">-->
<!--        <button class="button is-small">-->
<!--          <i class="fas fa-play"></i>-->
<!--        </button>-->
<!--      </div>-->

      <cytomine-slider
        v-model="currentTimeIndex"
        :max="image.duration - 1"
        :integer-only="true"
        class="image-dimension-slider"
      >
        <template v-if="image.fps" #default="{ value }">
          {{formatMinutesSeconds(value / image.fps)}} | {{value}}
        </template>
      </cytomine-slider>

      <image-controls-shift-buttons
          :index="index"
          :forward="true"
          :current="currentSlice.time"
          :size="image.duration"
          dimension="time"
          @shift="shift('time', $event)"
      />
    </div>
  </div>
</template>

<script>
import CytomineSlider from '@/components/form/CytomineSlider';
import ImageControlsShiftButtons from '@/components/viewer/ImageControlsShiftButtons';

import {formatMinutesSeconds, slicePositionToRank} from '@/utils/slice-utils.js';
import constants from '@/utils/constants';
import _ from 'lodash';
import ChannelName from '@/components/viewer/ChannelName';

export default {
  name: 'image-controls',
  components: {ChannelName, ImageControlsShiftButtons, CytomineSlider},
  props: {
    index: String
  },
  computed: {
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    imageWrapper() {
      return this.viewerWrapper.images[this.index];
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    channels() {
      return this.$store.getters[this.imageModule + 'channels'];
    },
    currentSlices() {
      return this.imageWrapper.activeSlices;
    },
    currentSlice() {
      return this.currentSlices[0];
    },
    nbSlices() {
      return this.image.depth * this.image.duration * this.image.channels;
    },
    currentChannelIndex: {
      get() {
        return this.currentSlice.channel;
      },
      async set(value) {
        await this.seek([value], this.currentSlice.zStack, this.currentSlice.time);
      }
    },
    currentChannelsIndexes: {
      get() {
        return this.currentSlices.map(slice => slice.channel);
      },
      async set(value) {
        await this.seek(value, this.currentSlice.zStack, this.currentSlice.time);
      }
    },
    currentZStackIndex: {
      get() {
        return this.currentSlice.zStack;
      },
      async set(value) {
        await this.seek(this.currentChannelsIndexes, value, this.currentSlice.time);
      }
    },
    currentTimeIndex: {
      get() {
        return this.currentSlice.time;
      },
      async set(value) {
        await this.seek(this.currentChannelsIndexes, this.currentSlice.zStack, value);
      }
    },
    hasChannels() {
      return this.image.channels > 1;
    },
    hasDepth() {
      return this.image.depth > 1;
    },
    hasDuration() {
      return this.image.duration > 1;
    },
    isImageMultidimensional() {
      return this.hasChannels || this.hasDuration || this.hasDepth;
    },
    showMultiChannels() {
      return this.currentChannelsIndexes.length > 1;
    },
    sliceInstances() {
      return this.imageWrapper.sliceInstances;
    },
    hasChannelName() {
      return this.currentSlice.channelName !== null;
    },
    areChannelsMergeable() {
      // this.image.apparentChannels <= constants.MAX_MERGEABLE_CHANNELS;
      return true; // TODO [EXPERIMENTAL - large set of merged channels]
    },
    channelOptions() {
      return this.channels.map(channel => ({value: [channel.index], ...channel}));
    },
    orderedCurrentChannels() {
      return [...this.currentChannelsIndexes].sort((a, b) => a - b).map(channelIndex =>
        this.channels.find(c => c.index === channelIndex)
      );
    },

    currentChannelsIndexesOption() {
      if (this.currentChannelsIndexes.length > 1) {
        return null;
      }
      return this.currentChannelsIndexes;
    },
    hasZName() {
      return this.currentSlice.zName !== null;
    },
    zValue() {
      return this.currentSlice.zName;
    }
  },
  methods: {
    setCurrentChannelsIndexes(value) {
      if (value === null) {
        const max = Math.min(constants.MAX_MERGEABLE_CHANNELS, this.image.channels);
        value = _.range(0, max);
      }
      this.currentChannelsIndexes = value;
    },
    formatMinutesSeconds(time) {
      return formatMinutesSeconds(time);
    },
    channelValue(channel) {
      if (this.channels.length === this.image.channels) {
        let info = this.channels[channel];
        return (info) ? info.name : null;
      } else {
        let rank = slicePositionToRank({
          channel,
          zStack: this.currentZStackIndex,
          time: this.currentTimeIndex
        }, this.image);
        let slice = this.sliceInstances[rank];
        return (slice) ? slice.channelName : null;
      }
    },

    async goToRank(rank) {
      await this.$store.dispatch(this.imageModule + 'setActiveSliceByRank', rank);
      this.$eventBus.$emit('reloadAnnotations', {idImage: this.image.id});
    },
    async seek(channels, zStack, time) {
      await this.$store.dispatch(this.imageModule + 'setActiveSlicesByPosition', {channels, zStack, time});
      this.$eventBus.$emit('reloadAnnotations', {idImage: this.image.id});
    },
    async shift(dimension, increment) {
      let time = (dimension === 'time') ? this.currentTimeIndex + increment : this.currentTimeIndex;
      let channels = (dimension === 'channel') ? [this.currentChannelIndex + increment] : this.currentChannelsIndexes;
      let zStack = (dimension === 'zStack') ? this.currentZStackIndex + increment : this.currentZStackIndex;
      await this.seek(channels, zStack, time);
    },
    canShiftForward(dimension) {
      switch (dimension) {
        case 'channel':
          return this.currentSlice.channel < this.image.channels - 1;
        case 'time':
          return this.currentSlice.time < this.image.duration - 1;
        case 'zStack':
          return this.currentSlice.zStack < this.image.depth - 1;
        case 'rank':
          return this.currentSlice.rank < this.nbSlices - 1;
      }
    },
    canShiftBackward(dimension) {
      switch (dimension) {
        case 'channel':
          return this.currentSlice.channel >= 1;
        case 'time':
          return this.currentSlice.time >= 1;
        case 'zStack':
          return this.currentSlice.zStack >= 1;
        case 'rank':
          return this.currentSlice.rank >= 1;
      }
    },

    async shortkeyHandler(key) {
      if (!this.isActiveImage) {
        return;
      }

      switch (key) {
        case 'nav-next-t':
          if (this.canShiftForward('time')) {
            await this.shift('time', 1);
          }
          return;
        case 'nav-previous-t':
          if (this.canShiftBackward('time')) {
            await this.shift('time', -1);
          }
          return;
        case 'nav-first-t':
          this.currentTimeIndex = 0;
          return;
        case 'nav-last-t':
          this.currentTimeIndex = this.image.duration - 1;
          return;
        case 'nav-next-z':
          if (this.canShiftForward('zStack')) {
            this.currentZStackIndex++;
          }
          return;
        case 'nav-previous-z':
          if (this.canShiftBackward('zStack')) {
            this.currentZStackIndex--;
          }
          return;
        case 'nav-first-z':
          this.currentZStackIndex = 0;
          return;
        case 'nav-last-z':
          this.currentZStackIndex = this.image.depth - 1;
          return;
        case 'nav-next-c':
          if (!this.showMultiChannels && this.canShiftForward('channel')) {
            this.currentChannelIndex++;
          }
          return;
        case 'nav-previous-c':
          if (!this.showMultiChannels && this.canShiftBackward('channel')) {
            this.currentChannelIndex--;
          }
          return;
        case 'nav-first-c':
          if (!this.showMultiChannels) {
            this.currentChannelIndex = 0;
          }
          return;
        case 'nav-last-c':
          if (!this.showMultiChannels) {
            this.currentChannelIndex = this.image.channels - 1;
          }
          return;
        case 'nav-next-slice':
          if (this.canShiftForward('rank')) {
            await this.goToRank(this.currentSlice.rank + 1);
          }
          return;
        case 'nav-previous-slice':
          if (this.canShiftBackward('rank')) {
            await this.goToRank(this.currentSlice.rank - 1);
          }
          return;
        case 'nav-first-slice':
          await this.seek([0], 0, 0);
          return;
        case 'nav-last-slice':
          await this.seek([this.image.channels - 1], this.image.depth - 1, this.image.duration - 1);
          return;
      }
    }
  },
  mounted() {
    this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
  }
};
</script>

<style lang="scss" scoped>
.image-controls-container {
  padding: 0.5rem;
  border-radius: 4px;
  box-shadow: 0 0 1px #777;
  background: white;
  display: flex;
  flex-direction: column;
}

.image-dimension {
  display: flex;
  align-items: center;
}

.image-dimension-name {
  padding-right: 0.5rem;
  width: 1rem;
}

.image-dimension-slider {
  flex-grow: 3;
}

.channel-selector {
  width: 100%;
}

</style>

<style>
.image-dimension .vue-slider {
  margin-left: 1rem;
  margin-right: 1rem;
}
</style>

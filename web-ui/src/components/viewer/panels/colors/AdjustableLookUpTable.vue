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
    <div class="buttons has-addons is-centered" v-if="showAdjustButtons">
      <button
        v-if="isImageMultidimensional"
        @click="$emit('adjustToSlice')"
        class="button is-small"
        :disabled="areBoundsAdjustedToSlice"
      >
        <span>
          {{ $t('button-adjust-slice') }}
        </span>
      </button>
      <button
        @click="$emit('adjustToImage')"
        class="button is-small"
        :disabled="areBoundsAdjustedToImage"
      >
        <span>
          {{ adjustImageLabel }}
        </span>
      </button>
      <button
        @click="$emit('reset')"
        class="button is-small"
        :disabled="areBoundsDefault"
      >
        <span>
         {{ $t('button-reset') }}
        </span>
      </button>
    </div>
    <div class="adjustments">
      <div class="adjustment" v-show="intensitiesByMinMax">
        <div class="adjustment-label">
          {{ $t('min-max') }}
        </div>
        <cytomine-slider
          class="adjustment-body"
          :value="arrayBounds"
          @input="setBounds"
          :min="defaultBounds.min"
          :max="defaultBounds.max"
          :contained="true"
          :smart-tooltip-position="true"
          size="is-small"
        />
      </div>

      <div class="adjustment" v-show="!intensitiesByMinMax">
        <div class="adjustment-label">
          {{ $t('brightness') }}
        </div>
        <cytomine-slider
          class="adjustment-body"
          :value="brightness"
          @input="setBrightness"
          :min="defaultBounds.min"
          :max="defaultBounds.max"
          :tooltip="false"
          :contained="true"
          size="is-small"
        />
      </div>
      <div class="adjustment" v-show="!intensitiesByMinMax">
        <div class="adjustment-label">
          {{ $t('contrast') }}
        </div>
        <cytomine-slider
          class="adjustment-body"
          :value="contrast"
          @input="setContrast"
          :min="defaultBounds.min"
          :max="defaultBounds.max"
          :tooltip="false"
          :contained="true"
          size="is-small"
        />
      </div>

      <div class="adjustment">
        <div class="adjustment-label">
          {{ $t('gamma') }}
        </div>
        <cytomine-slider
          class="adjustment-body"
          :value="gamma"
          @input="$emit('setGamma', $event)"
          :min="0.1"
          :max="4"
          :interval="0.1"
          :integer-only="false"
          :contained="true"
          :smart-tooltip-position="true"
          size="is-small"
        />
      </div>

      <div class="adjustment">
        <div class="adjustment-label">
          {{$t('inverse')}}
        </div>
        <div class="adjustment-body">
          <b-switch :value="inverted" @input="$emit('invert', $event)" size="is-small">
            <template v-if="inverted">{{$t('yes')}}</template>
            <template v-else>{{$t('no')}}</template>
          </b-switch>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {sameHistogramBounds} from '@/utils/histogram-utils';

import CytomineSlider from '@/components/form/CytomineSlider';

export default {
  name: 'AdjustableLookUpTable',
  components: {
    CytomineSlider
  },
  props: {
    index: String,
    adjustImageLabel: String,
    showAdjustButtons: Boolean,
    intensitiesByMinMax: Boolean,
    defaultBounds: Object,

    gamma: Number,
    inverted: Boolean,
    bounds: Object,

    imageBounds: Object,
    sliceBounds: Object
  },
  data() {
    return {
      brightness: 0,
      contrast: 0,
    };
  },
  computed: {
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    isImageMultidimensional() {
      return this.image.depth * this.image.duration > 1;
    },

    // Default bounds
    defaultRange() {
      return this.defaultBounds.max - this.defaultBounds.min;
    },
    defaultCenter() {
      return this.defaultRange / 2.0;
    },

    // Bounds
    range() {
      return this.bounds.max - this.bounds.min;
    },
    center() {
      return this.bounds.min + this.range / 2;
    },
    arrayBounds() {
      return [this.bounds.min, this.bounds.max];
    },

    areBoundsAdjustedToImage() {
      return sameHistogramBounds(this.bounds, this.imageBounds);
    },
    areBoundsAdjustedToSlice() {
      return sameHistogramBounds(this.bounds, this.sliceBounds);
    },
    areBoundsDefault() {
      return sameHistogramBounds(this.bounds, this.defaultBounds) && this.gamma === 1.0 && !this.inverted;
    }
  },
  watch: {
    bounds: {
      deep: true,
      handler: function () {
        this.computeBrightness();
        this.computeContrast();
      }
    }
  },
  methods: {
    setBounds(arrayBounds) {
      const first = arrayBounds[0];
      const second = arrayBounds[1];
      const inverted = (first > second);
      let min, max;
      if (inverted) {
        min = second;
        max = first;
      } else {
        min = first;
        max = second;
      }
      min = Math.max(this.defaultBounds.min, Math.min(min, this.defaultBounds.max));
      max = Math.min(this.defaultBounds.max, Math.max(max, this.defaultBounds.min));
      this.$emit('setBounds', {min, max});
    },
    setBrightness(value) {
      // https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/ContrastAdjuster.java.html
      const b = this.defaultBounds.min + this.defaultRange *
        ((this.defaultRange - value) / this.defaultRange);
      const mid = this.range / 2.0;
      this.$emit('setBounds', {
        min: Math.max(this.defaultBounds.min, Math.round(b - mid)),
        max: Math.min(this.defaultBounds.max, Math.round(b + mid))
      });
      this.brightness = value;
    },
    setContrast(value) {
      // https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/ContrastAdjuster.java.html
      let slope;
      if (value <= this.defaultCenter) {
        slope = value / this.defaultCenter;
      } else {
        slope = this.defaultCenter / (this.defaultRange - value);
      }
      if (slope > 0) {
        const mid = this.defaultRange / 2.0;
        this.$emit('setBounds', {
          min: Math.max(this.defaultBounds.min, Math.round(this.center - mid / slope)),
          max: Math.min(this.defaultBounds.max, Math.round(this.center + mid / slope))
        });
        this.contrast = value;
      }
    },
    computeBrightness() {
      // https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/ContrastAdjuster.java.html
      this.brightness = Math.round(
        this.defaultRange *
        (1.0 - (this.center - this.defaultBounds.min) / this.defaultRange)
      );
    },
    computeContrast() {
      // https://imagej.nih.gov/ij/developer/source/ij/plugin/frame/ContrastAdjuster.java.html
      const c = this.defaultRange / this.range;
      this.contrast = Math.round(
        (c > 0) ? this.defaultRange - (this.defaultCenter / c) : this.defaultCenter * c
      );
    }
  },
  created() {
    this.computeBrightness();
    this.computeContrast();
  }
};
</script>

<style scoped>
.adjustments {
  display: flex;
  flex-direction: column;
}

.adjustment {
  display: flex;
  align-items: center;
}

.adjustment-label {
  font-weight: 600;
  padding-right: 0.5rem;
  width: 25%;
  text-align: right;
}

.adjustment-body {
  flex-grow: 3;
}

.buttons {
  margin-bottom: 0 !important;
}
</style>

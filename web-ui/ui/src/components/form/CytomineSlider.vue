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
<vue-slider
  :value="value" @change="$emit('input', $event)"
  :min="min"
  :max="max"
  :tooltip="(tooltip) ? 'always' : 'none'"
  :interval="interval"
  :tooltip-placement="tooltipPlacement"
  :lazy="lazy"
  :dot-size="dotSize"
  :contained="contained"
  :enable-cross="false"
  @dragging="internalValue = $event"
  :class="[
    {'vue-slider-margin-for-tooltip': tooltip && isArray && !smartTooltipPosition},
    size
  ]"
>
  <template #tooltip="{value, index}" v-if="tooltip">
    <div
      :class="[
        'vue-slider-dot-tooltip-inner',
        `vue-slider-dot-tooltip-inner-${tooltipPlacement[index]}`,
        size
        ]"
      @mousedown.stop
      @click.stop="startEdition(index)"
    >
        <template v-if="indexEdited !== index">
          <slot name="default" :value="value" >
            {{Math.round(value * 1000)/1000}}
          </slot>
        </template>
        <b-input
          v-else
          type="text"
          ref="inputSlider"
          v-model="editedValue"
          @hook:mounted="focus()"
          @blur="stopEdition(index)"
          @keyup.enter.native="stopEdition(index)"
          :size="size"
        />
    </div>
  </template>
</vue-slider>
</template>

<script>
import VueSlider from 'vue-slider-component';

export default {
  name: 'cytomine-slider',
  components: {VueSlider},
  props: {
    value: {type: null},
    min: {type: Number, default: 0},
    max: {type: Number, default: 100},
    interval: {type: Number},
    integerOnly: {type: Boolean, default: true},
    lazy: {type: Boolean, default: true},
    tooltip: {type: Boolean, default: true},
    size: {type: String, default: 'is-normal'}, // is-small, is-normal
    contained: {type: Boolean, default: false}, // Whether the slider should be fully contained within its containing element.
    smartTooltipPosition: {type: Boolean, default: false}
  },
  data() {
    return {
      indexEdited: null,
      editedValue: 0,
      internalValue: 0, // Bypass slider lazy update
    };
  },
  computed: {
    isArray() {
      return Array.isArray(this.value);
    },
    range() {
      return this.max - this.min;
    },
    middle() {
      return this.range / 2;
    },
    tooltipPlacement() {
      if (this.isArray) {
        if (this.smartTooltipPosition) {
          const n = this.value.length + 1;
          const values = (Array.isArray(this.internalValue)) ? this.internalValue : this.value;
          return values.map((v, i) => (v >= this.range * (i + 1) / n) ? 'left' : 'right');
        }
        return this.value.map((_, i) => (i === 0) ? 'left' : 'right');
      }

      if (this.internalValue >= this.middle) {
        return ['left'];
      }

      return ['right'];
    },
    dotSize() {
      switch (this.size) {
        case 'is-small':
          return 10.5;
        default:
          return 14;
      }
    }
  },
  watch: {
    value() {
      this.internalValue = this.value;
    }
  },
  methods: {
    startEdition(index) {
      if (this.indexEdited !== index) {
        this.editedValue = this.isArray ? this.value[index] : this.value;
        this.indexEdited = index;
      }
    },
    stopEdition(index = 0) {
      if (this.indexEdited === index) {
        this.indexEdited = null;

        if (!this.editedValue || isNaN(this.editedValue)) {
          return; // if entered value is not a number, ignore
        }

        let parsedValue = this.integerOnly ? parseInt(this.editedValue) : Number(this.editedValue);
        parsedValue = Math.min(parsedValue, this.max);
        parsedValue = Math.max(parsedValue, this.min);

        if (this.isArray) {
          let newVal = this.value.slice();
          newVal[index] = parsedValue;
          if (newVal[0] > newVal[1]) { // reorder bounds if needed
            newVal.reverse();
          }
          this.$emit('input', newVal);
        } else {
          this.$emit('input', parsedValue);
        }
      }
    },
    focus() {
      this.$refs.inputSlider.focus();
    }
  },
  created() {
    this.internalValue = this.value;
  }
};
</script>

<style lang="scss">
@import '~vue-slider-component/theme/default.css';

.vue-slider-dot-tooltip-inner {
  font-size: 0.9rem !important;
}

.vue-slider-dot-tooltip-inner .control {
  font-size: inherit;
}

.vue-slider-dot-tooltip-inner input {
  width: 4rem;
  height: 1.5rem;
  font-size: 0.8rem;
}

.vue-slider-dot-tooltip-inner.is-small {
  font-size: 0.7rem !important;
  padding: 1px 3px;
  min-width: 10px;
  border-radius: 3px;
}
.vue-slider-dot-tooltip-inner.is-small input {
  width: 2rem;
  height: 1rem;
  font-size: 0.6rem;
}

.vue-slider-margin-for-tooltip {
  margin-left: 4rem;
  margin-right: 6rem;
}
</style>

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
<multiselect
  :value="value" @input="$emit('input', $event)"
  :label="label"
  :track-by="trackBy"
  :group-label="groupLabel"
  :group-values="groupValues"
  :group-select="true"
  :options="options"
  :optionHeight="30"
  :showLabels="false"
  :multiple="multiple"
  :close-on-select="internalCloseOnSelect"
  :searchable="searchable"
  :clear-on-select="multiple ? false : true"
  :showPointer="false"
  :placeholder="$t('select-options')"
  :allow-empty="allowEmpty"
  :disabled="disabled"
>

  <template #beforeList v-if="multiple && options.length > 0 && selectAllAvailable">
    <li class="multiselect__element multiselect__select-all" @click="selectAll()">
      <span :class="['multiselect__option', allSelected ? 'multiselect__option--selected' : '']">
        {{$t('select-all')}}
      </span>
    </li>
  </template>

  <template #selection="{isOpen}" v-if="multiple && options.length > 0">
    <div class="multiselect__tags-wrap" v-if="!isOpen">
      <strong v-if="allSelected"> {{allPlaceholder || $t('all')}} </strong>
      <template v-else>
        <span v-for="(option, index) in displayedOptions" :key="option[trackBy]">
          {{label ? option[label] : option}}<template v-if="index < displayedOptions.length - 1">,</template>
        </span>
        <strong v-if="countNotDisplayed > 0">
          {{ $tc("and-count-others", countNotDisplayed, {count: countNotDisplayed}) }}
        </strong>
      </template>
    </div>
    <div v-else></div>
  </template>

  <template #option="{option}">
    <slot name="option" :option="option"></slot>
  </template>

</multiselect>
</template>

<script>
import Multiselect from 'vue-multiselect';

export default {
  name: 'cytomine-multiselect',
  components: {Multiselect},
  props: {
    value: {type: null},
    options: {type: null},
    label: {type: String},
    trackBy: {type: String},
    groupLabel: {type: String},
    groupValues: {type: String},
    multiple: {type: Boolean, default: false},
    closeOnSelect: {type: Boolean, default: null},
    selectAllAvailable: {type: Boolean, default: true},
    searchable: {type: Boolean, default: true},
    allowEmpty: {type: Boolean, default: true},
    disabled: {type: Boolean, default: false},
    allPlaceholder: {type: String}
  },
  data() {
    return {
      maxNbDisplayed: 3
    };
  },
  computed: {
    allSelected() {
      if (!this.multiple) {
        return false;
      }

      return this.options.every(opt => this.value.includes(opt));
    },
    displayedOptions() {
      return this.value.slice(0, this.maxNbDisplayed);
    },
    countNotDisplayed() {
      return this.value.length - this.maxNbDisplayed;
    },
    internalCloseOnSelect() {
      return (this.closeOnSelect === null) ? (!this.multiple) : this.closeOnSelect;
    }
  },
  methods: {
    selectAll() {
      let newValue = this.allSelected ? [] : this.options;
      this.$emit('input', newValue);
    }
  }
};
</script>

<style lang="scss">
@import '~vue-multiselect/dist/vue-multiselect.min.css';
@import '~bulma/sass/utilities/initial-variables.sass';

.multiselect--active, .multiselect__content-wrapper {
  z-index: 50 !important;
}

.multiselect__option--selected {
  font-weight: 600 !important;
}

.multiselect__option--selected::before {
  content: "\f00c";
  font-family: 'Font Awesome 5 Free';
  font-size: 10px;
  position: absolute;
  left: 10px;
}

.multiselect__option {
  padding: 6px 5px 5px 30px !important;
  font-size: 1rem !important;
  min-height: 30px !important;
}

.multiselect__option:hover {
  background: #61b2e8 !important;
  color: white;
}

.multiselect__select-all {
  border-bottom: 1px solid #eee;
  display: inline-block;
  width: 100%;
  height:100%;
}

.multiselect__placeholder, .multiselect__tags-wrap, .multiselect__single {
  margin-bottom: 8px;
  line-height: 20px;
  vertical-align: top;
  font-size: 1rem;
}

.multiselect__tags-wrap :first-child {
  margin-left: 5px;
}

.multiselect__placeholder, .multiselect__single {
  padding-left: 5px;
  position: relative;
  padding-top: 0 !important;
}

.multiselect__input {
  font-size: 1rem !important;
}

.is-danger .multiselect__tags {
  border-color: $red;
}

.is-danger .multiselect__select::before {
  border-color: $red transparent transparent;
}
</style>

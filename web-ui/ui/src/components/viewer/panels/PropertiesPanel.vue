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
<div class="properties-panel">
  <h1>{{$t('properties')}}</h1>
  <b-field horizontal :label="$t('key')">
    <b-select size="is-small" v-model="selectedPropertyKey">
      <option :value="null">
        {{$t('no-key-selected')}}
      </option>
      <option v-for="key in propertiesKeys" :value="key" :key="key">
        {{ key }}
      </option>
    </b-select>
  </b-field>

  <b-field horizontal :label="$t('color')">
    <b-select size="is-small" v-model="selectedPropertyColor">
      <option v-for="color in colors" :value="color" :key="color.name">
        {{ $t(color.name) }}
      </option>
    </b-select>
  </b-field>
</div>
</template>

<script>
import {defaultColors} from '@/utils/style-utils.js';

export default {
  name: 'properties-panel',
  props: {
    index: String
  },
  computed: {
    colors() {
      return defaultColors;
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    selectedPropertyKey: {
      get() {
        return this.imageWrapper.properties.selectedPropertyKey;
      },
      set(value) {
        this.$store.dispatch(this.imageModule + 'setSelectedPropertyKey', value);
      }
    },
    selectedPropertyColor: {
      get() {
        return this.imageWrapper.properties.selectedPropertyColor;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setSelectedPropertyColor', value);
      }
    },
    propertiesKeys() {
      return this.imageWrapper.properties.propertiesKeys;
    }
  }
};
</script>

<style>
.properties-panel select {
  width: 15em;
}
</style>

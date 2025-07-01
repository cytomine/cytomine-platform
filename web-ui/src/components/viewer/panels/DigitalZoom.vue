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
  <h1>{{$t('digital-zoom')}}</h1>
  <b-checkbox v-model="digitalZoom">
    {{$t('digital-zoom-checkbox-label')}}
  </b-checkbox>

  <div class="actions level">
    <button class="button is-small level-item" @click="$emit('fitZoom')">
      {{ $t('button-best-fit-zoom') }}
    </button>
    <button class="button is-small level-item" @click="$emit('resetZoom')">
      {{ $t('button-reset-zoom') }}
    </button>
  </div>

</div>
</template>

<script>
export default {
  name: 'digital-zoom',
  props: {
    index: String
  },
  computed: {
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    digitalZoom: {
      get() {
        return this.imageWrapper.view.digitalZoom;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setDigitalZoom', Boolean(value));
      }
    }
  }
};
</script>

<style scoped>
  .actions .button {
    margin: 3px;
    box-sizing: border-box;
  }
</style>

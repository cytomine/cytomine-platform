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
  <div class="scale-line-collapsed-wrapper">
    <div v-if="scaleLineCollapsed">
      <button class="button is-small" @click="scaleLineCollapsed = false">
        <span class="fas fa-angle-double-left"></span>
      </button>
    </div>
    <div v-else>
      <button class="button is-small" @click="scaleLineCollapsed = true">
        <span class="fas fa-angle-double-right"></span>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'toggle-scale-line',
  props: {
    index: String
  },
  computed: {
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    scaleLineCollapsed: {
      get() {
        return this.imageWrapper.view.scaleLineCollapsed;
      },
      set(collapsed) {
        this.$store.dispatch(this.viewerModule + 'setScaleLineCollapsed', {
          index: this.index,
          collapsed: collapsed
        });
      }
    }
  },
};
</script>

<style scoped>
.button {
  height: 1.5em;
  width: 1.5em;
  padding: 3px;
}

.scale-line-collapsed-wrapper {
  display: block;
  position: absolute;
  right: 4rem;
  bottom: 1rem;
  background: white;
  border-radius: 2px;
  font-size: 5px;
  margin-left: 2px;
  padding: 0.5em;
}
</style>

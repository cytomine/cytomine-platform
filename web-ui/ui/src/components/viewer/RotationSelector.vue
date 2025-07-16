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
<div class="rotation-selector-wrapper" :class="{expanded}">
  <div class="ol-rotate ol-unselectable ol-control custom">
    <button type="button" @click="expanded = true">
      <span class="ol-compass" :style="{transform: `rotate(${rotation}rad)`}">⇧</span>
    </button>
  </div>
  <div v-show="expanded">
    <div class="top">
      <div class="rotation-controls">
        <b-field>
          <p class="control">
            <button class="button is-small" @click="degreesRotation = 0">{{$t('button-reset')}}</button>
          </p>
          <p class="control">
            <button class="button is-small" @click="increment(90)">+90°</button>
          </p>
          <p class="control">
            <button class="button is-small" @click="increment(-90)">-90°</button>
          </p>
        </b-field>
      </div>
      <button class="delete is-small" @click="expanded=false"></button>
    </div>
    <cytomine-slider v-model="degreesRotation" :max="360" :integerOnly="false" :lazy="false" />
  </div>
</div>
</template>

<script>
import CytomineSlider from '@/components/form/CytomineSlider';

export default {
  name: 'rotation-selector',
  components: {CytomineSlider},
  props: {
    index: String
  },
  data() {
    return {
      expanded: false
    };
  },
  computed: {
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    rotation() {
      return this.imageWrapper.view.rotation;
    },
    degreesRotation: {
      get() {
        return Math.round(this.rotation * 180 / Math.PI * 1000) / 1000;
      },
      set(value) {
        this.$store.dispatch(this.viewerModule + 'setRotation', {
          index: this.index,
          rotation: Number(value) / 180 * Math.PI
        });
      }
    }
  },
  methods: {
    increment(inc) {
      this.degreesRotation = (this.degreesRotation + inc + 360) % 360;
    }
  }
};
</script>

<style scoped>
.rotation-selector-wrapper {
  border-radius: 2px;
  box-shadow: 0 0 1px #777;
  background: white;
  margin-left: 2px;
}

.rotation-selector-wrapper.expanded {
  min-width: 16.5em;
  padding-top: 0.4em;
  min-height: 6em;
}

.rotation-selector-wrapper.expanded .ol-control button {
  cursor: initial;
}

.rotation-selector-wrapper .ol-control {
  padding: 0;
}

.rotation-selector-wrapper.expanded .ol-control button {
  box-shadow: none;
}

.ol-rotate.ol-control {
  left: 0 !important;
  top: 0 !important;
}

.top {
  margin: 0em 0.4em 0.75em 1.8em;
  display: flex;
}

.top .rotation-controls {
  flex: 1;
  display: flex;
  justify-content: center;
  margin-left: 1em;
  margin-right: 1em;
  margin-top: 0.5em;
}

.top .delete {
  z-index: 20 !important;
}
</style>

<style>
.rotation-selector-wrapper .vue-slider {
  margin-left: 1em;
  margin-right: 1em;
}
</style>

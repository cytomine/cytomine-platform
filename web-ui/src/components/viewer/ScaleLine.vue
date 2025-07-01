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
<div class="scale-line" :class="{'interpolation': interpolation}">
  <div class="scale-line-top" :style="{width: scaleLineLength + 'px'}">
    {{scaleLength}}
  </div>
  <div class="scale-line-bottom">
    <span v-show="magnification">
      {{$t('magnification')}}: {{magnification}}X
    </span>
  </div>

  <div class="scale-line-position" v-if="mousePosition">
    <div style="float: left;">x: {{Math.round(mousePosition[0])}}</div>
    <div>y: {{Math.round(mousePosition[1])}}</div>
  </div>
</div>
</template>

<script>
export default {
  name: 'scale-line',
  props: {
    image: Object,
    zoom: Number,
    mousePosition: Array
  },
  data() {
    return {
      scaleLineLength: 100
    };
  },
  computed: {
    magnification() {
      let magnification = Math.pow(2, this.zoom - this.image.zoom) * this.image.magnification;
      return Math.round(magnification * 100) / 100;
    },
    resolution() {
      let resolution = this.image.physicalSizeX ? this.image.physicalSizeX : 1;
      return Math.pow(2, this.image.zoom - this.zoom) * resolution;
    },
    scaleLength() {
      let length = this.scaleLineLength * this.resolution;
      if (this.image.physicalSizeX) {
        let unit = this.$t('um');
        if (length > 1000) {
          length /= 1000;
          unit = this.$t('mm');
        }
        return `${length.toPrecision(3)} ${unit}`;
      } else {
        return `${Math.round(length * 1000) / 1000} ${this.$t('pixels')}`;
      }
    },
    interpolation() {
      return this.zoom > this.image.zoom;
    }
  }
};
</script>

<style scoped>
.scale-line {
  background: white;
  position: absolute;
  padding: 0.4em 0.8em;
  display: block;
  right: 4rem;
  bottom: 1rem;
  font-size: 9px;
  font-family: Helvetica;
  min-width: 100px;
}

.scale-line-top {
  margin-top: 0.5em;
  box-sizing: content-box;
  border: 2px solid black;
  border-top: none;
  text-align: center;
  padding-bottom: 0.2em;
}

.scale-line-bottom {
  box-sizing: content-box;
  border: 2px solid black;
  border-top: none;
  border-bottom: none;
  text-align: center;
  padding-top: 0.2em;
  margin-bottom: 1em;
  min-height: 1em;
}

.interpolation {
  color: red;
}

.interpolation .scale-line-top, .interpolation .scale-line-bottom {
  border-color: red;
}

.scale-line-position {
  text-align: center;
  padding: 0 0.5em;
  color: black !important;
}
</style>

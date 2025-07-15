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
<v-popover
  placement="right"
  trigger="manual"
  :open="opened"
  :auto-hide="false"
> <!-- autoHide leads to erratic behaviour when adding/showing DOM elements => handle display of popover manually -->

  <div class="annot-preview" :class="{clickable}">
    <div :style="styleAnnotDetails" @click.self="viewAnnot(sameViewOnClick)">
      <button class="button is-small" @click="opened = !opened" ref="previewButton" v-if="showDetails">
        <i :class="['fas', opened ? 'fa-minus' : 'fa-plus']"></i>
      </button>
      <button class="button is-small" @click="viewAnnot()" v-else-if="clickable && sameViewOnClick">
        <i class="fas fa-external-link-alt"></i>
      </button>

    </div>
    <template v-if="showSliceInfo">
      <div v-if="image.channels > 1 || image.channels == null">C: {{annot.channel}}</div>
      <div v-if="image.depth > 1 || image.depth == null">Z: {{annot.zStack}}</div>
      <div v-if="image.duration > 1 || image.duration == null">T: {{annot.time}}</div>
    </template>
  </div>

  <template #popover>
    <annotation-details
      v-click-outside.capture="(event) => close(event)"
      :annotation="annot"
      :terms="terms"
      :users="users"
      :images="images"
      :tracks="tracks"
      :show-image-info="showImageInfo"
      @addTerm="$emit('addTerm', $event)"
      @addTrack="$emit('addTrack', $event)"
      @updateTerms="$emit('updateTermsOrTracks')"
      @updateTracks="$emit('updateTermsOrTracks')"
      @updateProperties="$emit('updateProperties')"
      @select="$emit('select', $event)"
      @centerView="$emit('centerView')"
      @deletion="$emit('deletion')"
      v-if="opened"
    /> <!-- Display component only if it is the currently displayed annotation
            (prevents fetching unnecessary information) -->
  </template>
</v-popover>
</template>

<script>
import {appendShortTermToken} from '@/utils/token-utils.js';
import {get} from '@/utils/store-helpers.js';

export default {
  name: 'annotation-preview',
  props: {
    annot: Object,
    size: Number,
    color: String,
    terms: Array,
    users: Array,
    images: Array,
    tracks: Array,
    showDetails: {type: Boolean, default: true},
    showImageInfo: {type: Boolean, default: true},
    showSliceInfo: {type: Boolean, default: false},
    clickable: {type: Boolean, default: true},
    sameViewOnClick: {type: Boolean, default: false}
  },
  components: {
    AnnotationDetails: () => import('./AnnotationDetails') // To resolve circular reference
  },
  data() {
    return {
      opened: false,
      revisionCrop: 0
    };
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    cropParameters() {
      let params = {
        square: true,
        complete: true,
        thickness: 2,
        increaseArea: 1.25,
        rev: this.revisionCrop,
      };

      if (this.color || this.color === '') {
        params.draw = true;
      }
      if (this.color) {
        params.color = `0x${this.color}`;
      }
      if (this.annot.updated) {
        params.updated = this.annot.updated;
      }

      return params;
    },
    cropUrl() {
      return this.annot.annotationCropURL(this.size, 'jpg', this.cropParameters);
    },
    styleAnnotDetails() {
      let url = appendShortTermToken(`${this.cropUrl}`, this.shortTermToken);
      //console.log('url', url);
      return {
        backgroundImage: `url(${url})`,
        backgroundRepeat: 'no-repeat',
        width: this.size + 'px',
        height: this.size + 'px'
      };
    },
    image() {
      return this.images.find(image => image.id === this.annot.image);
    }
  },
  methods: {
    viewAnnot(trySameView = false) {
      if (this.clickable) {
        this.$emit('select', {annot: this.annot, options:{trySameView}});
      }
    },
    close(event) {
      if (!this.opened) {
        return;
      }

      let el = event.target;
      while (el) {
        if (el.classList.contains('modal') || el.classList.contains('notifications') || el.isSameNode(this.$refs.previewButton)) {
          // do not close the popover if click was performed in modal or in notification
          // if click performed on previewButton, handled in @click
          return;
        }
        el = el.parentElement;
      }

      this.opened = false;
    },
    reloadAnnotationCropHandler(annot) {
      if (annot.id === this.annot.id) {
        this.revisionCrop++;
      }
    }
  },
  mounted() {
    this.$eventBus.$on('reloadAnnotationCrop', this.reloadAnnotationCropHandler);
  },
  beforeDestroy() {
    // unsubscribe from all events
    this.$eventBus.$off('reloadAnnotationCrop', this.reloadAnnotationCropHandler);
  }
};
</script>

<style scoped>
.annot-preview {
  display: inline-block;
  background-position: center center;
  background-repeat: no-repeat;
  margin: 10px;
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  border: 3px solid white;
  text-align: right;
}

.clickable {
  cursor: pointer;
}

.annot-preview .button {
  font-size: 10px;
  width: 20px;
  height: 20px;
  box-sizing: border-box;
  position: relative;
  left: 3px;
  bottom: 3px;
  border: none;
}
</style>

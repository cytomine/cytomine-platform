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
<div class="card" :class="{'full-height-card': fullHeightCard}">
  <router-link class="card-image recent-image" :to="`/project/${idProject}/image/${idImage}`">
    <figure class="image is-5by3" :style="figureStyle">
    </figure>
  </router-link>
  <div class="card-content">
    <div class="content">
      <p>
        <router-link :to="`/project/${idProject}/image/${idImage}`">
          <span v-if="isBlindMode" class="blind-indication">[{{this.$t('blinded-name-indication')}}]</span>
          {{ image.blindedName || image.instanceFilename || image.imageName }}
          <span v-if="showProject" class="in-project">({{$t('in-project', {projectName: image.projectName})}})</span>
        </router-link>
      </p>
      <slot></slot>
    </div>
  </div>
</div>
</template>

<script>
import {changeImageUrlFormat} from '@/utils/image-utils';
import {get} from '@/utils/store-helpers';
import {appendShortTermToken} from '@/utils/token-utils.js';

export default {
  name: 'image-preview',
  props: {
    image: {type: Object},
    project: {type: Object, default: null},
    fullHeightCard: {type: Boolean, default: true},
    showProject: {type: Boolean, default: false},
    blindMode: {type: Boolean, default: false},
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    idImage() {
      return this.image.image || this.image.id; // if provided object is image consultation, image.image
    },
    idProject() {
      return (this.project) ? this.project.id : this.image.project;
    },
    isBlindMode() {
      return (this.project) ? this.project.blindMode : this.blindMode;
    },
    rawPreviewUrl() {
      return this.image.thumb || this.image.imageThumb;
    },
    previewUrl() {
      return changeImageUrlFormat(this.rawPreviewUrl);
    },
    figureStyle() {
      return {backgroundImage: `url("${appendShortTermToken(this.previewUrl, this.shortTermToken)}")`};
    }
  },
};
</script>

<style scoped>
.image {
  background-repeat: no-repeat;
  background-position: center center;
  background-size: cover;
  position: relative;
  border-bottom: 1px solid #ddd;
}

.card.full-height-card {
  height: 100%;
}

.card-content {
  padding: 1.5rem;
  overflow-wrap: break-word;
}

.card-content a {
  font-weight: 600;
}

.blind-indication {
  font-size: 0.9em;
  text-transform: uppercase;
}
</style>

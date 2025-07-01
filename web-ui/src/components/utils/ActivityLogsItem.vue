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
<li :key="action.id">
  <strong>{{Number(action.created) | moment('l LTS')}}:</strong>
  <span class="content" @mouseenter="enter" @mouseleave="leave">
    <router-link v-if="route" :to="route">
      {{action.message}}
    </router-link>
    <template v-else>{{action.message}}</template>
  </span>
  <div v-if="previewUrl" class="preview" :class="{visible: showPreview}">
    <div class="box">
      <img :src="appendShortTermToken(previewUrl, shortTermToken)" />
    </div>
  </div>
</li>
</template>

<script>


import {appendShortTermToken} from '@/utils/token-utils.js';
import {get} from '@/utils/store-helpers.js';

const ANNOT = 1;
const IMAGE = 2;
const PROJECT = 3;

export default {
  name: 'activity-logs-item',
  props: {
    action: Object
  },
  data() {
    return {
      showPreview: false,
      previewUrl: null,
      route: null
    };
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    actionData() {
      return JSON.parse(this.action.data);
    },
    // eslint-disable-next-line vue/return-in-computed-property
    type() {
      switch (this.action.serviceName) {
        case 'userAnnotationService':
        case 'reviewedAnnotationService':
          return ANNOT;
        case 'imageInstanceService':
          return IMAGE;
        case 'projectService':
          return PROJECT;
        default:
          return null;
      }
    }
  },
  methods: {
    appendShortTermToken,
    enter() {
      this.showPreview = true;
    },
    leave() {
      this.showPreview = false;
    },
    annotationHandler(annot) {
      this.previewUrl = `${annot.url}?maxSize=125&complete=true&thickness=2&increaseArea=1.25&draw=true`;
      this.route = `/project/${annot.project}/image/${annot.image}/annotation/${annot.id}`;
    },
    imageHandler(img) {
      if (img.deleted) {
        return;
      }
      this.previewUrl = img.thumb;
      this.route = `/project/${img.project}/image/${img.id}`;
    },
    projectHandler(project) {
      this.route = `/project/${project.id}`;
    },
    addCommandHandler() {
      switch (this.type) {
        case ANNOT:
          this.annotationHandler(this.actionData);
          break;
        case IMAGE:
          this.imageHandler(this.actionData);
          break;
        case PROJECT:
          this.projectHandler(this.actionData);
          break;
      }
    },
    editCommandHandler() {
      switch (this.type) {
        case ANNOT:
          // erratic core behaviour -> need to keep the defined new?Annotation property
          this.annotationHandler(
            this.actionData.newUserAnnotation ||
            this.actionData.newReviewedAnnotation ||
            this.actionData.newAnnotation
          );
          break;
        case IMAGE:
          this.imageHandler(this.actionData.newImageInstance);
          break;
      }
    }
  },
  created() {
    switch (this.action.className) {
      case 'be.cytomine.command.AddCommand':
      case 'be.cytomine.domain.command.AddCommand':
        this.addCommandHandler();
        break;
      case 'be.cytomine.command.EditCommand':
      case 'be.cytomine.domain.command.EditCommand':
        this.editCommandHandler();
        break;
      case 'be.cytomine.command.DeleteCommand':
      case 'be.cytomine.domain.command.DeleteCommand':
        // object no longer available => cannot display preview or provide link
        break;
      default:
        console.log('Unknown command type ' + this.action.className);
    }
  }
};
</script>

<style scoped>
li {
  padding: 0.25em 2em;
  position: relative;
}

li:hover {
  background: rgba(0, 0, 0, 0.025);
}

.content {
  margin-left: 0.35em;
}

.preview {
  display: none;
  position: absolute;
  top: 1.75em;
  left: 15em;
  z-index: 100;
}

.preview.visible {
  display: block;
}

.preview img {
  max-height: 300px;
  max-width: 300px;
}

.box {
  padding: 0.7em;
}
</style>

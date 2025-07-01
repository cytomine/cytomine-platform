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
<div class="annotation-details">
  <template v-if="filteredLinks.length > 0">
    <div>
      <annotation-preview
          v-for="annotLink in filteredLinks"
          :key="`${annotation.id}-${annotLink.id}`"
          :annot="annotLink"
          :show-details="false"
          :show-image-info="false"
          :show-slice-info="false"
          :size="size"
          :color="(annotLink.id === annotation.id) ? mainColor : linkColor"
          :clickable="allowAnnotationSelection"
          :same-view-on-click="allowAnnotationSelection && showSelectAllButton"
          @select="selectAnnotation($event)"
          v-tooltip="annotLink.instanceFilename"
      />
    </div>
    <div class="level navigation" v-if="allowAnnotationSelection && showSelectAllButton">
      <button class="level-item button is-small" @click="selectPrevious">
        <i class="fas fa-angle-left fa-lg"></i> {{$t('button-previous-annot-link')}}
      </button>
      <button class="level-item button is-small" @click="selectNext">
        {{$t('button-next-annot-link')}} <i class="fas fa-angle-right fa-lg"></i>
      </button>
    </div>
    <a v-if="showSelectAllButton" class="button is-small is-fullwidth" @click="showLinkedAnnotations()">
      {{ $t('button-show-linked-annots') }}
    </a>
  </template>
  <em v-else>{{$t('no-linked-annotation')}}</em>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import AnnotationPreview from '@/components/annotations/AnnotationPreview';
import constants from '@/utils/constants';

import {Annotation} from 'cytomine-client';

export default {
  name: 'annotation-links-preview',
  components: {
    'annotation-preview': AnnotationPreview,
  },
  props: {
    index: {type: String, default: null},
    annotation: {type: Object},
    images: {type: Array},
    allowAnnotationSelection: {type: Boolean, default: false},
    showSelectAllButton: {type: Boolean, default: false},
    showMainAnnotation: {type: Boolean, default: false},
    size: {type: Number, default: 64},
    mainColor: {type: String, default: null},
    linkColor: {type: String, default: null},
  },
  data() {
    return {};
  },
  computed: {
    configUI: get('currentProject/configUI'),
    currentUser: get('currentUser/user'),

    image() {
      return this.images.find(image => image.id === this.annotation.image) ||
        {'id': this.annotation.image, 'instanceFilename': this.annotation.instanceFilename};
    },

    annotationURL() {
      return `/project/${this.annotation.project}/image/${this.annotation.image}/annotation/${this.annotation.id}`;
    },

    annotationLinks() {
      if (!this.annotation.annotationLink || this.annotation.annotationLink.length < 2) {
        return [];
      }

      let annots = this.annotation.annotationLink.map(link => {
        return new Annotation({
          id: link.annotation,
          updated: link.updated,
          image: link.image,
          instanceFilename: this.images.find(image => image.id === link.image).instanceFilename,
          url: `${constants.CYTOMINE_CORE_HOST}/api/annotation/${link.annotation}/crop.png`,
          cropURL: `${constants.CYTOMINE_CORE_HOST}/api/annotation/${link.annotation}/crop.png`
        });
      });
      annots.sort((a, b) => a.instanceFilename.localeCompare(b.instanceFilename));
      return annots;
    },
    orderedLinks() {
      let mainIndex = this.annotationLinks.findIndex(annot => annot.id === this.annotation.id);
      let next = this.annotationLinks.slice(mainIndex);
      let previous = this.annotationLinks.slice(0, mainIndex);
      return next.concat(previous);
    },
    filteredLinks() {
      if (this.showMainAnnotation) {
        return (this.orderedLinks.length > 0) ? this.orderedLinks : [this.annotation];
      }
      return this.orderedLinks.slice(1);
    },
    viewerWrapper() {
      return (this.index) ? this.$store.getters['currentProject/currentViewer'] : {};
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    },
  },
  methods: {
    showLinkedAnnotations() {
      this.filteredLinks.forEach(link => {
        this.$emit('select', {annot: link, options:{}});
      });
    },
    selectAnnotation({annot, options}) {
      if (this.allowAnnotationSelection) {
        this.$emit('select', {annot, options});
      }
    },
    selectNext() {
      let annot = this.orderedLinks[1];
      this.selectAnnotation({annot, options:{trySameView: true}});
    },
    selectPrevious() {
      let annot = this.orderedLinks[this.orderedLinks.length - 1];
      this.selectAnnotation({annot, options:{trySameView: true}});
    },
    shortkeyHandler(key) {
      if (!this.isActiveImage) {
        return;
      }

      if (key === 'nav-next-annot-link') {
        this.selectNext();
      } else if (key === 'nav-previous-annot-link') {
        this.selectPrevious();
      }
    }
  },
  mounted() {
    if (this.index !== null) {
      this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
    }
  },
  beforeDestroy() {
    if (this.index !== null) {
      this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
    }
  }
};
</script>

<style scoped>
>>> .annot-preview {
  margin: 3px;
}

.level.navigation {
  justify-content: center;
  margin: 3px 0;
}

.level.navigation .level-item:first-child {
  margin-right: 3px;
}

.level.navigation .level-item:last-child {
  margin-left: 3px;
}

.fa-angle-left {
  margin-right: 0.4em;
}

.fa-angle-right {
  margin-left: 0.4em;
}
</style>

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
  <div class="annotation-details-playground" ref="playground">
    <vue-draggable-resizable
      v-if="selectedFeature && selectedFeature.properties && reload"
      v-show="displayAnnotDetails"
      class="draggable"
      :parent="true"
      :resizable="false"
      drag-handle=".drag"
      @dragstop="dragStop"
      :w="width" h='auto' :x="positionAnnotDetails.x" :y="positionAnnotDetails.y"
      ref="detailsPanel"
    >
      <div class="actions">
        <h1>{{ $t('current-selection') }}</h1>
        <button class="drag button is-small close">
          <i class="fas fa-arrows-alt"></i>
        </button>
        <button class="button is-small close" @click="displayAnnotDetails = false">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="annotation-details-container">
        <annotation-details
          v-if="selectedFeature.properties.annot.hasOwnProperty('user')"
          :annotation="selectedFeature.properties.annot"
          :terms="terms"
          :images="images"
          :slices="slices"
          :profiles="profiles"
          :tracks="tracks"
          :users="allUsers"
          :showImageInfo="false"
          :showChannelInfo="showChannelInfo"
          :key="selectedFeature.id"
          :showComments="showComments"
          @addTerm="$emit('addTerm', $event)"
          @addTrack="$emit('addTrack', $event)"
          @updateTerms="$emit('updateTermsOrTracks', annot)"
          @updateTracks="$emit('updateTermsOrTracks', annot)"
          @updateProperties="$emit('updateProperties')"
          @select="$emit('select', $event)"
          @centerView="$emit('centerView', ($event) ? $event : annot)"
          @deletion="$emit('delete', annot)"
          @searchSimilarAnnotations="searchSimilarAnnotations"
        />

        <annotation-simple-details
          v-else
          :annotation="selectedFeature.properties.annot"
          @centerView="$emit('centerView', ($event) ? $event : annot)"
        />
      </div>

      <!-- HACK for prev/next linked annotation shortkeys -->
      <annotation-links-preview
        v-show="false"
        :index="index"
        :show-main-annotation="false"
        :show-select-all-button="false"
        :allow-annotation-selection="true"
        :annotation="selectedFeature.properties.annot"
        :images="images"
        @select="$emit('select', $event)"
      />
    </vue-draggable-resizable>
  </div>
</template>

<script>
import VueDraggableResizable from 'vue-draggable-resizable';
import {Cytomine, UserCollection} from 'cytomine-client';
import AnnotationDetails from '@/components/annotations/AnnotationDetails';
import AnnotationLinksPreview from '@/components/annotations/AnnotationLinksPreview';
import AnnotationSimpleDetails from '@/components/viewer/annotations/AnnotationSimpleDetails';

export default {
  name: 'annotations-details-container',
  components: {
    AnnotationDetails,
    AnnotationLinksPreview,
    AnnotationSimpleDetails,
    VueDraggableResizable,
  },
  props: {
    index: String,
  },
  data() {
    return {
      width: 320,
      projectUsers: [],
      reload: true,
      showComments: false
    };
  },
  computed: {
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    images() {
      if (this.imageWrapper.imageGroup) {
        return [this.image, ...this.imageWrapper.imageGroup.imageInstances];
      }
      return [this.image];
    },
    slices() {
      return this.imageWrapper.activeSlices;
    },
    showChannelInfo() {
      return this.imageWrapper.activeSlices && this.imageWrapper.activeSlices.length > 1;
    },
    profiles() {
      return this.imageWrapper.profile ? [this.imageWrapper.profile] : [];
    },
    displayAnnotDetails: {
      get() {
        return this.imageWrapper.selectedFeatures.displayAnnotDetails;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setDisplayAnnotDetails', value);
      }
    },
    positionAnnotDetails: {
      get() {
        return this.imageWrapper.selectedFeatures.positionAnnotDetails;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setPositionAnnotDetails', value);
      }
    },
    selectedFeature() {
      return this.$store.getters[this.imageModule + 'selectedFeature'];
    },
    allUsers() {
      return this.projectUsers;
    },
    annot() {
      return this.selectedFeature ? this.selectedFeature.properties.annot : {};
    },
    terms() {
      return this.$store.getters['currentProject/terms'] || [];
    },
    tracks() {
      return this.imageWrapper.tracks.tracks;
    }
  },
  watch: {
    selectedFeature() {
      if (this.selectedFeature) {
        // this.displayAnnotDetails = true;
        let targetAnnot = this.imageWrapper.selectedFeatures.showComments;
        this.showComments = (targetAnnot === this.annot.id);
        if (targetAnnot !== null) {
          this.$store.commit(this.imageModule + 'setShowComments', null);
        }
      }
    }
  },
  methods: {
    async fetchUsers() {
      let collection = new UserCollection({
        filterKey: 'project',
        filterValue: this.image.project.id,
      });

      this.projectUsers = (await collection.fetchAll()).array;
    },

    dragStop(x, y) {
      this.positionAnnotDetails = {x, y};
    },

    async handleResize() {
      await this.$nextTick(); // wait for update of clientWidth and clientHeight to their new values

      if (this.$refs.playground) {
        let maxX = Math.max(this.$refs.playground.clientWidth - this.width, 0);
        let height = 500;
        if (this.$refs.detailsPanel) {
          height = this.$refs.detailsPanel.height;
        }
        let maxY = Math.max(this.$refs.playground.clientHeight - height, 0);
        let x = Math.min(this.positionAnnotDetails.x, maxX);
        let y = Math.min(this.positionAnnotDetails.y, maxY);
        this.positionAnnotDetails = {x, y};

        // HACK to force the component to recreate and take into account new (x,y) ; should no longer be
        // necessary with version 2 of vue-draggable-resizable
        this.reload = false;
        this.$nextTick(() => this.reload = true);
      }
    },
    async searchSimilarAnnotations() {
      let data = (await Cytomine.instance.api.get(
        'retrieval/search',
        {
          params: {
            annotation: this.selectedFeature.properties.annot.id,
            nrt_neigh: 10 // eslint-disable-line camelcase
          }
        }
      )).data;
      this.$store.commit(this.imageModule + 'setShowSimilarAnnotations', true);
      this.$store.commit(this.imageModule + 'setSimilarAnnotations', data);
      this.$store.commit(this.imageModule + 'setQueryAnnotation', this.selectedFeature.properties.annot);
    }
  },
  created() {
    this.fetchUsers();
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
    this.$eventBus.$on('updateMapSize', this.handleResize);
  },
  destroyed() {
    window.removeEventListener('resize', this.handleResize);
    this.$eventBus.$off('updateMapSize', this.handleResize);
  }
};
</script>

<style scoped>
.annotation-details-playground {
  position: absolute;
  left: 3.5rem;
  top: 3.5rem;
  right: 4.5rem;
  bottom: 2em;
  pointer-events: none; /* to allow selection of elements below it */
  /* background: rgba(255, 255, 255, 0.2);
  border: 2px dashed rgba(0, 0, 0, 0.5); */
}

.draggable {
  background: #f2f2f2;
  display: flex;
  flex-direction: column;
  border-radius: 5px;
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  pointer-events: auto;
}

.actions {
  padding: 0.35em;
  text-align: right;
  background-color: #e5e5e5;
  border-bottom: 1px solid #b5b5b5;
  border-radius: 5px 5px 0 0;
  display: flex;
  align-items: center;
}

h1 {
  font-size: 0.9rem;
  padding: 0;
  flex: 1;
  text-align: left;
  margin-left: 0.4em;
}

.actions .button {
  margin-left: 0.25rem;
  width: 1.75rem;
}

.annotation-details-container {
  padding: 0.6em;
  overflow: auto;
  height: 100%;
}
</style>

<style>
.dragging .button.drag {
  background-color: #6899d0;
  color: #fff;
}

.annotation-details-playground .draggable {
  z-index: 15 !important;
}
</style>

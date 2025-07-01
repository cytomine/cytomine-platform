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
<div v-if="error" class="box error">
  <h2> {{ $t('error') }} </h2>
  <p>{{ $t('error-loading-image') }}</p>
  <p v-if="errorBadImageProject">{{ $t('error-loading-image-bad-project') }}</p>
</div>
<div v-else class="cytomine-viewer">
  <b-loading :is-full-page="false" :active="loading" />

  <div class="ae-sidebar">
    <app-engine-sidebar></app-engine-sidebar>
  </div>

  <div v-if="!loading" class="maps-wrapper">
    <div class="map-cell"
      v-for="(cell, i) in cells"
      :key="i"
      :style="`height:${elementHeight}%; width:${elementWidth}%;`"
      :class="{highlighted: cell && cell.highlighted}"
    >
      <cytomine-image
        v-if="cell && cell.image && cell.slices"
        :index="cell.index"
        :key="`${cell.index}-${cell.image.id}`"
        @close="closeMap(cell.index)"
      />
    </div>

    <image-selector />

    <!-- Emit event when a hotkey is pressed (to rework once https://github.com/iFgR/vue-shortkey/issues/78 is implemented) -->
    <div class="hidden" v-shortkey.once="shortkeysMapping" @shortkey="shortkeyEvent"></div>

  </div>

</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import CytomineImage from './CytomineImage';
import ImageSelector from './ImageSelector';
import AppEngineSidebar from '@/components/appengine/sidebar/AppEngineSidebar';

import viewerModuleModel from '@/store/modules/project_modules/viewer';

import constants from '@/utils/constants.js';
import shortcuts from '@/utils/shortcuts.js';

import {ImageInstance, SliceInstance, Annotation} from 'cytomine-client';

export default {
  name: 'cytomine-viewer',
  components: {
    CytomineImage,
    ImageSelector,
    AppEngineSidebar
  },
  data() {
    return {
      error: false,
      errorBadImageProject: false,
      loading: true,
      reloadInterval: null,
      idViewer: null
    };
  },
  computed: {
    project: get('currentProject/project'),
    currentUser: get('currentUser/user'),
    viewers() {
      return this.$store.state.projects[this.project.id].viewers;
    },
    idImages() {
      return this.$route.params.idImages.split('-');
    },
    idSlices() {
      return (this.$route.params.idSlices) ? this.$route.params.idSlices.split('-') : [];
    },
    paramIdViewer() {
      return this.$route.query.viewer;
    },
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    viewer() {
      return this.viewers[this.idViewer];
    },
    indexImages() {
      return this.viewer ? Object.keys(this.viewer.images) : [];
    },
    nbImages() {
      return this.indexImages.length;
    },
    nbHorizontalCells() {
      return Math.ceil(Math.sqrt(this.nbImages));
    },
    nbVerticalCells() {
      return this.nbHorizontalCells ? Math.ceil(this.nbImages / this.nbHorizontalCells) : 0;
    },
    cells() {
      let cells = new Array(this.nbHorizontalCells * this.nbVerticalCells);
      for (let i = 0; i < this.nbImages; i++) {
        let index = this.indexImages[i];
        let image = this.viewer.images[index].imageInstance;
        let slices = this.viewer.images[index].activeSlices;
        let highlighted = (this.viewer.images[index].view) ? this.viewer.images[index].view.highlighted : false;
        cells[i] = {index, image, slices, highlighted};
      }
      return cells;
    },
    elementHeight() {
      return 100 / this.nbVerticalCells;
    },
    elementWidth() {
      return 100 / this.nbHorizontalCells;
    },
    shortkeysMapping() {
      let allowed = ['nav-next-image', 'nav-previous-image', 'nav-next-slice', 'nav-previous-slice', 'nav-next-t',
        'nav-previous-t', 'nav-next-c', 'nav-previous-c', 'nav-first-slice', 'nav-last-slice', 'nav-first-t',
        'nav-last-t', 'nav-first-z', 'nav-last-z', 'nav-first-c', 'nav-last-c', 'nav-next-image-in-group',
        'nav-previous-image-in-group', 'nav-next-annot-link', 'nav-previous-annot-link',
        'tool-select', 'tool-point', 'tool-line', 'tool-freehand-line', 'tool-rectangle', 'tool-circle', 'tool-polygon',
        'tool-freehand-polygon', 'tool-screenshot', 'tool-fill', 'tool-correct-add', 'tool-correct-remove', 'tool-modify', 'tool-rescale',
        'tool-move', 'tool-rotate', 'tool-delete', 'tool-undo', 'tool-redo', 'tool-review-accept', 'tool-review-reject',
        'toggle-review-layer', 'toggle-all-review-layer', 'toggle-selected-layers', 'toggle-all-selected-layers',
        'tool-go-to-slice-t', 'tool-go-to-slice-z', 'tool-go-to-slice-c', 'toggle-information',
        'toggle-zoom', 'toggle-filters', 'toggle-layers', 'toggle-ontology', 'toggle-properties', 'toggle-broadcast',
        'toggle-review', 'toggle-overview', 'toggle-annotations', 'toggle-current', 'toggle-add-image', 'toggle-link',
        'nav-next-z', 'nav-previous-z', 'tool-copy', 'tool-paste', 'tool-review-reject', 'tool-review-toggle',
        'tool-go-to-slice-t', 'tool-go-to-slice-z', 'tool-go-to-slice-c', 'toggle-all-information', 'toggle-all-zoom',
        'toggle-all-filters', 'toggle-all-layers', 'toggle-all-ontology', 'toggle-all-properties',
        'toggle-all-broadcast', 'toggle-all-review', 'toggle-all-overview', 'toggle-all-annotations',
        'toggle-all-current', 'toggle-all-link'];

      return Object.keys(shortcuts).filter(key => allowed.includes(key.replace('viewer-', ''))).reduce((object, key) => {
        object[key.replace('viewer-', '')] = shortcuts[key];
        return object;
      }, {});
    }
  },
  watch: {
    paramIdViewer() {
      this.findIdViewer();
    },
    idViewer(_, old) {
      if (old) {
        this.loading = true;
        this.loadViewer();
      }
    },
    viewer() {
      if (!this.viewer) {
        console.log('Viewer closed from external source');
        this.$router.push(`/project/${this.$route.params.idProject}`);
      }
    },
    nbImages() {
      this.$eventBus.$emit('updateMapSize');
    }
  },
  methods: {
    findIdViewer() {
      if (this.paramIdViewer) {
        this.idViewer = this.paramIdViewer;
        return;
      }

      for (let id in this.viewers) {
        // if viewer containing the targetted images, and only them, store its id
        let imagesViewer = Object.values(this.viewers[id].images).map(img => img.imageInstance.id).join('-');
        if (imagesViewer === this.$route.params.idImages) {
          this.idViewer = id;
          return;
        }
      }

      this.idViewer = Math.random().toString(36).substr(2, 9);
    },

    closeMap(index) {
      if (this.nbImages === 1) {
        this.$store.unregisterModule(['projects', this.project.id, 'viewers', this.idViewer]);
        this.$router.push(`/project/${this.$route.params.idProject}`);
      } else {
        this.$store.dispatch(this.viewerModule + 'removeImage', index);
      }
    },

    async loadViewer() {
      try {
        this.$store.commit('currentProject/setCurrentViewer', this.idViewer);
        if (!this.viewer) {
          this.$store.registerModule(['projects', this.project.id, 'viewers', this.idViewer], viewerModuleModel);

          // List of unique images (prevent to fetch it multiple times)
          const uniqueIdImages = [...new Set(this.idImages)];
          let uniqueImages = {};
          await Promise.all(uniqueIdImages.map(async id => {
            uniqueImages[id] = await ImageInstance.fetch(id);
          }));

          // Ensure images are in the right project
          const nbWrongProjectImages = Object.values(uniqueImages).filter(image =>
            image.project !== this.project.id
          ).length;
          if (nbWrongProjectImages > 0) {
            this.errorBadImageProject = true;
            throw new Error('Some images are not from this project');
          }

          // Register images in the viewer, in right order
          let indexedImages = {};
          this.idImages.map(id => {
            indexedImages[this.viewer.indexNextImage] = uniqueImages[id];
            this.$store.dispatch(this.viewerModule + 'registerImage');
          });

          // For each image, initialize them asynchronously, and fetch corresponding slices
          await Promise.all(Object.entries(indexedImages).map(async ([index, image]) => {
            const position = this.idImages.indexOf(String(image.id));
            let idSlices = this.idSlices[position];

            let slices;
            if (idSlices) {
              idSlices = [...new Set(idSlices.split(':'))];
              slices = await Promise.all(idSlices.map(async id => await SliceInstance.fetch(id)));

              // Ensure slices are in the right project/image
              const z = slices[0].zStack;
              const t = slices[0].time;
              const nbWrongSlices = slices.filter(slice =>
                slice.image !== image.id || slice.zStack !== z || slice.time !== t
              ).length;
              if (nbWrongSlices > 0) {
                this.errorBadImageProject = true;
                throw new Error('Some slices are not from this project or cannot be displayed together');
              }
            } else {
              slices = [await image.fetchReferenceSlice()];
            }
            await this.$store.dispatch(`${this.viewerModule}images/${index}/initialize`, {image, slices});
          }));

          let images = {};
          //don't fetch multiple times the same image.
          let idImages = [...new Set(this.idImages)];
          await Promise.all(idImages.map(async id => {
            let image = await ImageInstance.fetch(id);
            images[id] = image;
          }));
          const imagesNotInCurrentProject = Object.values(images).filter(image => image.project !== this.project.id);
          if (imagesNotInCurrentProject.length > 0) {
            this.errorBadImageProject = true;
            throw new Error('Some images are not from this project');
          }
        } else {
          await this.$store.dispatch(this.viewerModule + 'refreshData');
        }
        this.loading = false;
      } catch (err) {
        console.log(err);
        this.error = true;
      }
    },

    async selectAnnotationHandler({index, annot, center = false}) {
      try {
        if (index && annot.image !== this.viewer.images[index].imageInstance.id) {
          annot = await Annotation.fetch(annot.id);
          let [image, slice] = await Promise.all([
            ImageInstance.fetch(annot.image),
            SliceInstance.fetch(annot.slice)
          ]);
          this.$store.commit(`${this.viewerModule}images/${index}/setRoutedAnnotation`, annot);
          await this.$store.dispatch(`${this.viewerModule}images/${index}/setImageInstance`, {image, slices: [slice]});
        } else if (index === null) {
          annot = await Annotation.fetch(annot.id);
          if (this.idImages.includes(String(annot.image))) {
            let index = this.cells.find(cell => cell.image.id === annot.image).index;
            this.$eventBus.$emit('selectAnnotation', {index, annot, center});
          } else {
            let [image, slice] = await Promise.all([
              ImageInstance.fetch(annot.image),
              SliceInstance.fetch(annot.slice)
            ]);
            await this.$store.dispatch(this.viewerModule + 'addImage', {image, slices: [slice], annot});
          }
        }
      } catch (err) {
        console.log(err);
        this.error = true;
      }
    },

    shortkeyEvent(event) {
      this.$eventBus.$emit('shortkeyEvent', event.srcKey);
    }
  },
  async created() {
    this.findIdViewer();
    await this.loadViewer();
    this.reloadInterval = setInterval(
      () => this.$eventBus.$emit('reloadAnnotations'),
      constants.VIEWER_ANNOTATIONS_REFRESH_INTERVAL
    );
  },
  mounted() {
    this.$eventBus.$on('selectAnnotation', this.selectAnnotationHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('selectAnnotation', this.selectAnnotationHandler);
    clearInterval(this.reloadInterval);
  }
};
</script>

<style scoped>
.cytomine-viewer {
  display: flex;
  height: 100%;
}

.ae-sidebar {
  width: 24rem;
  border-right-color: #333;
  border-right-width: 1px;
  border-right-style: solid;
}

.maps-wrapper {
  flex: 1;
  height: 100%;
  display: flex;
  flex-wrap: wrap;
  position: relative;
  overflow: hidden;
}

.map-cell {
  border-top: 0.2em solid #222;
  overflow: hidden;
}

.hidden {
  display: none;
}

.highlighted {
  border: 6px solid #0099ff;
}
</style>

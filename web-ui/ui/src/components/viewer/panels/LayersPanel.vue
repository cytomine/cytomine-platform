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
<div class="layers">
  <h1>{{ $t('annotation-layers') }}</h1>
  <b-message v-if="error" type="is-danger" has-icon icon-size="is-small" size="is-small">
    <p> {{ $t('unexpected-error-info-message') }} </p>
  </b-message>
  <template v-else>
    <b-field>
      <b-select :placeholder="$t('select-layer')" size="is-small" v-model="selectedLayer">
        <option v-for="layer in unselectedLayers" :value="layer" :key="layer.id">
          {{ layerName(layer) }}
        </option>
      </b-select>
      <button class="button is-small" @click="addLayer()" :disabled="!selectedLayer">{{ $t('button-add') }}</button>
    </b-field>
    <table class="table">
      <thead>
        <tr>
          <th class="checkbox-column"><span class="far fa-eye"></span></th>
          <th v-if="!reviewMode" class="checkbox-column"><span class="fas fa-pencil-alt"></span></th>
          <th class="name-column"></th>
          <th class="checkbox-column"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(layer, idx) in selectedLayers" :key="layer.id">
          <td class="checkbox-column">
            <b-checkbox size="is-small" :value="layer.visible" @input="toggleLayerVisibility(idx)" />
          </td>
          <td v-if="!reviewMode" class="checkbox-column">
            <b-checkbox size="is-small" :value="layer.drawOn" :disabled="!canDraw(layer)" @input="toggleLayerDrawOn(idx)" />
          </td>

          <td class="name-column">
            {{ layerName(layer) }}
          </td>
          <td class="checkbox-column">
            <button v-if="!reviewMode || !layer.isReview" class="button is-small" @click="removeLayer(idx)">
              <span class="fas fa-times"></span>
            </button>
          </td>
        </tr>
        <tr v-if="selectedLayers.length === 0">
          <td colspan="4" class="has-text-grey is-italic">{{$t('no-selected-layers')}}</td>
        </tr>
      </tbody>
    </table>

    <div class="opacity">
      <label>{{ $t('layers-opacity') }}</label>
      <input class="slider is-fullwidth is-small" v-model="layersOpacity" step="0.05" min="0" max="1" type="range">
    </div>
  </template>
</div>
</template>

<script>
import _ from 'lodash';
import {get} from '@/utils/store-helpers';

import {Cytomine, ProjectDefaultLayerCollection} from 'cytomine-client';

export default {
  name: 'layers-panel',
  props: {
    index: String,
    layersToPreload: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      error: false,

      layers: [], // Array<User> (representing user layers)
      indexLayers: [],
      selectedLayer: null
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    currentAccount: get('currentUser/account'),
    project: get('currentProject/project'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    imageWrapper() {
      return this.viewerWrapper.images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    slices() {
      return this.imageWrapper.activeSlices;
    },
    activePanel() {
      return this.imageWrapper.activePanel;
    },
    layersOpacity: {
      get() {
        return this.imageWrapper.style.layersOpacity;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setLayersOpacity', Number(value));
      }
    },
    selectedLayers() { // Array<User> (representing user layers)
      return this.imageWrapper.layers.selectedLayers || [];
    },
    selectedLayersIds() {
      return this.selectedLayers.map(layer => layer.id);
    },
    unselectedLayers() {
      return this.layers.filter(layer => !this.selectedLayersIds.includes(layer.id)).sort((a, b) => (a.fullName < b.fullName) ? -1 : 1);
    },
    nbReviewedAnnotations() {
      return this.indexLayers.reduce((cnt, layer) => cnt + layer.countReviewedAnnotation, 0);
    },
    hasReviewLayer() {
      return this.image.inReview || this.image.reviewed;
    },
    reviewLayer() {
      return {id: -1, isReview: true};
    },
    reviewMode() {
      return this.imageWrapper.review.reviewMode;
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    }
  },
  watch: {
    activePanel() {
      this.fetchIndexLayers();
    },
    reviewMode() {
      if (this.reviewMode) {
        if (!this.layers.includes(this.reviewLayer)) {
          this.layers.push(this.reviewLayer);
        }
        this.addLayer(this.reviewLayer);
      } else {
        if (!this.hasReviewLayer) {
          let index = this.selectedLayersIds.findIndex(id => id === this.reviewLayer.id);
          if (index !== -1) {
            this.removeLayer(index);
          }

          this.layers = this.layers.filter(layer => !layer.isReview);
        }
      }
    },
    layersToPreload: {
      deep: true,
      handler: function (layersToPreload) {
        layersToPreload.forEach(layerId => {
          let index = this.selectedLayersIds.findIndex(id => id === this.reviewLayer.id);
          if (index !== -1) {
            if (!this.selectedLayers[index].visible) {
              this.toggleLayerVisibility(index);
            }
            return;
          }

          this.addLayerById(layerId, true);
        });
      }
    }
  },
  methods: {
    addAnnotationEventHandler(annot, saved = true) {
      this.annotationEventHandler(annot);
      let updatedProject = this.$store.state.currentProject.project.clone();

      if (annot.type === 'UserAnnotation') {
        if (saved) {
          updatedProject.numberOfAnnotations++;
        }
      } else {
        updatedProject.numberOfReviewedAnnotations++;
      }

      this.$store.dispatch('currentProject/updateProject', updatedProject);
    },
    deleteAnnotationEventHandler(annot) {
      this.annotationEventHandler(annot);
    },
    annotationEventHandler(annot) {
      if (annot.image === this.image.id) {
        this.fetchIndexLayers();
      }
    },
    reloadAnnotationsHandler({idImage} = {}) {
      if (!idImage || idImage === this.image.id) {
        this.fetchIndexLayers();
      }
    },

    layerName(layer) {
      if (!Object.prototype.hasOwnProperty.call(layer, 'username')) {
        return layer.name;
      }

      if (layer.isReview) {
        return `${this.$t('review-layer')} (${this.nbReviewedAnnotations})`;
      }

      let id = (this.currentAccount.isDeveloper) ? ` (${this.$t('id')}: ${layer.id})` : '';

      let indexLayer = this.indexLayers.find(index => index.user === layer.id) || {};
      return `${layer.fullName}${id} (${indexLayer.countAnnotation || 0})`;
    },

    canDraw(layer) {
      return !layer.isReview && this.$store.getters['currentProject/canEditLayer'](layer.id);
    },

    addLayerById(id, visible) {
      let layer = this.layers.find(layer => layer.id === id);
      if (layer) {
        this.addLayer(layer, visible);
      }
    },

    addLayer(layer = this.selectedLayer, visible = true) {
      if (this.selectedLayersIds.includes(layer.id)) {
        return;
      }

      layer.visible = visible;
      layer.drawOn = (layer.id === this.currentUser.id && this.canDraw(layer));
      this.$store.dispatch(this.imageModule + 'addLayer', layer);

      this.selectedLayer = null;
    },

    removeLayer(index) {
      this.$store.dispatch(this.imageModule + 'removeLayer', index);
    },

    toggleLayerVisibility(index) {
      this.$store.dispatch(this.imageModule + 'toggleLayerVisibility', index);
    },

    toggleLayerDrawOn(index) {
      this.$store.commit(this.imageModule + 'toggleLayerDrawOn', index);
    },

    async fetchLayers() {
      this.layers = (await this.project.fetchUserLayers(this.image.id)).array;
      if (this.hasReviewLayer) {
        this.layers.push(this.reviewLayer);
      }

      let layers = (await Cytomine.instance.api.get(`image-instances/${this.image.id}/annotation-layers`)).data;
      this.layers.push(...layers);

      // if image instance was changed (e.g. with previous/next image navigation), some of the selected layers
      // may not be relevant for the current image => filter them
      let idLayers = this.layers.map(layer => layer.id);
      this.$store.commit(this.imageModule + 'filterSelectedLayers', idLayers);
    },

    async fetchIndexLayers(force = false) {
      if (!force && this.activePanel !== 'layers') {
        return;
      }
      // TODO: optimize, backend should be able to send indexes for several slices at once.
      let indexLayers = await Promise.all(this.slices.map(async slice => await slice.fetchAnnotationsIndex()));
      indexLayers = Object.values(_.groupBy(indexLayers.flat(), 'user'));
      this.indexLayers = indexLayers.map(userIndexLayers => userIndexLayers.reduce((a, b) => {
        return {
          user: a.user,
          countAnnotation: a.countAnnotation + b.countAnnotation,
          countReviewedAnnotation: a.countReviewedAnnotation + b.countReviewedAnnotation
        };
      }, {user: userIndexLayers[0].user, countAnnotation: 0, countReviewedAnnotation: 0}));
      // ----
    },

    shortkeyHandler(key) {
      if (!key.startsWith('toggle-all-') && !this.isActiveImage) { // shortkey should only be applied to active map
        return;
      }

      key = key.replace('toggle-all-', 'toggle-');
      if (key === 'toggle-selected-layers') {
        this.selectedLayers.forEach((layer, index) => this.toggleLayerVisibility(index));
      } else if (key === 'toggle-review-layer') {
        let index = this.selectedLayersIds.findIndex(id => id === this.reviewLayer.id);
        if (index !== -1) {
          this.toggleLayerVisibility(index);
          return;
        }

        if (this.layers.includes(this.reviewLayer)) {
          this.addLayer(this.reviewLayer);
        }
      }
    }
  },
  async created() {
    try {
      await Promise.all([
        this.fetchLayers(),
        this.fetchIndexLayers(true),
      ]);
    } catch (error) {
      console.log(error);
      this.error = true;
      this.$notify({type: 'error', text: this.$t('notif-error-loading-annotation-layers')});
      return;
    }

    let layersToAdd = this.layersToPreload.map(id => ({id, visible: true}));

    if (!this.imageWrapper.layers.selectedLayers) { // we do not use computed property selectedLayers because we don't want the replacement by [] if the store array is null
      if (!this.layersToPreload.includes(this.currentUser.id)) {
        layersToAdd.push({id: this.currentUser.id, visible: true});
      }

      try {
        let defaultLayers = await ProjectDefaultLayerCollection.fetchAll({
          filterKey: 'project',
          filterValue: this.project.id
        });

        let addedIds = layersToAdd.map(layer => layer.id);

        defaultLayers.array.forEach(({user, hideByDefault}) => {
          if (!addedIds.includes(user)) {
            layersToAdd.push({id: user, visible: !hideByDefault});
          }
        });
      } catch (error) {
        console.log(error);
      }
    }
    layersToAdd.map(layer => this.addLayerById(layer.id, layer.visible));
  },
  mounted() {
    this.$eventBus.$on('addAnnotation', this.addAnnotationEventHandler);
    this.$eventBus.$on('deleteAnnotation', this.deleteAnnotationEventHandler);
    this.$eventBus.$on('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('addAnnotation', this.addAnnotationEventHandler);
    this.$eventBus.$off('deleteAnnotation', this.deleteAnnotationEventHandler);
    this.$eventBus.$off('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
  }
};
</script>

<style scoped>
>>> select {
  width: 21em;
}

.table {
  margin-bottom: 1em !important;
  font-size: 0.9em;
}

.table tbody {
  display: block;
  overflow: auto;
  max-height: 10em;
}

.table thead tr {
  display: block;
}

td, th {
  padding: 0.25em !important;
  vertical-align: middle !important;
}

td .button {
  width: 1.5em;
  height: 1.5em;
  font-size: 0.9em;
  padding: 0;
}

.checkbox-column {
  min-width: 2.2em;
  text-align: center !important;
}

.name-column {
  width: 100%;
}

>>> .checkbox .control-label {
  padding: 0 !important;
}

>>> input[type="range"].slider {
  margin: 0;
  padding: 0;
}

.opacity {
  display: flex;
  align-items: center;
}

.opacity label {
  text-transform: uppercase;
  font-size: 0.8em;
  width: 15em;
}
</style>

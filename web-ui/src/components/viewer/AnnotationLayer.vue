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
<vl-layer-vector
  :visible="layer.visible"
  :extent="imageExtent"
  :update-while-interacting="false"
>

  <vl-source-vector
    ref="olSource"
    :loader-factory="loaderFactory"
    :strategy-factory="strategyFactory"
    url="-"
  > <!-- HACK because loader factory not used if URL not specified -->
    <vl-style-func :factory="styleFunctionFactory" />
  </vl-source-vector>

</vl-layer-vector>
</template>

<script>
import WKT from 'ol/format/WKT';
import {AnnotationCollection, Cytomine} from 'cytomine-client';
import {annotBelongsToLayer} from '@/utils/annotation-utils';
import {get} from '@/utils/store-helpers';

export default {
  name: 'annotation-layer',
  props: {
    index: String,
    layer: Object
  },
  data() {
    return {
      format: new WKT(),

      resolution: null,
      lastExtent: null,
      clustered: null,
      maxResolutionNoClusters: null
    };
  },
  computed: {
    project: get('currentProject/project'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    slices() {
      return this.imageWrapper.activeSlices;
    },
    sliceIds() {
      return this.slices.map(slice => slice.id);
    },
    annotsIdsToSelect() {
      return this.imageWrapper.selectedFeatures.annotsToSelect.map(annot => annot.id);
    },
    imageExtent() {
      return [0, 0, this.image.width, this.image.height];
    },
    selectedFeatures() {
      return this.imageWrapper.selectedFeatures.selectedFeatures;
    },
    ongoingEdit() {
      return this.imageWrapper.draw.ongoingEdit;
    },
    terms() {
      return this.imageWrapper.style.terms || [];
    },
    styleFunctionFactory() {
      // Force computed property update when one of those properties change (leading to new style function =>
      // rerendering - see https://github.com/ghettovoice/vuelayers/issues/68#issuecomment-404223423)
      this.imageWrapper.selectedFeatures.selectedFeatures;
      this.imageWrapper.style.layersOpacity;
      this.terms.forEach(term => {
        term.visible;
        term.color;
        term.opacity;
      });
      this.imageWrapper.style.displayNoTerm;
      this.imageWrapper.style.noTermOpacity;
      this.imageWrapper.properties.selectedPropertyKey;
      this.imageWrapper.properties.selectedPropertyColor;
      this.imageWrapper.review.reviewMode;
      this.imageWrapper.style.wrappedTracks.forEach(track => {
        track.color;
      });

      return () => {
        return this.$store.getters[this.imageModule + 'genStyleFunction'];
      };
    },
    reviewMode() {
      return this.imageWrapper.review.reviewMode;
    }
  },
  watch: {
    reviewMode() { // in review mode, reviewed annotation no longer displayed => need to force reload
      this.clearFeatures();
    }
  },
  methods: {
    clearFeatures(cache = true) {
      if (this.$refs.olSource) {
        this.$store.commit(this.imageModule + 'removeLayerFromSelectedFeatures', {layer: this.layer, cache});
        this.$refs.olSource.clearFeatures();
      }
    },

    annotBelongsToLayer(annot) {
      return annotBelongsToLayer(annot, this.layer, this.sliceIds);
    },

    addAnnotationHandler(annot) {
      if (this.annotBelongsToLayer(annot) && this.$refs.olSource) {
        this.$refs.olSource.addFeature(this.createFeature(annot));
      }
    },
    selectAnnotationHandler({annot, index}) {
      if (index === this.index && this.annotBelongsToLayer(annot) && this.$refs.olSource) {
        let olFeature = this.$refs.olSource.getFeatureById(annot.id);
        if (!olFeature) {
          this.$store.commit(this.imageModule + 'setAnnotToSelect', annot);
        } else {
          this.$store.dispatch(this.imageModule + 'selectFeature', olFeature);
        }
      }
    },
    reloadAnnotationsHandler({idImage, clear = false, hard = false} = {}) {
      if (!idImage || idImage === this.image.id) {
        if (clear) {
          this.clearFeatures();
        } else if (hard) {
          this.clearFeatures(false);
          this.loader();
        } else {
          this.loader();
        }
      }
    },
    reviewAnnotationHandler(annot) {
      if (this.reviewMode) { // if the image is in review mode, reviewed annotation should no longer be displayed on user layer => call delete handler
        this.deleteAnnotationHandler(annot);
      }
    },
    editAnnotationHandler(annot) {
      if (this.annotBelongsToLayer(annot) && this.$refs.olSource) {
        let olFeature = this.$refs.olSource.getFeatureById(annot.id);
        if (!olFeature) {
          return;
        }
        olFeature.setGeometry(this.format.readGeometry(annot.location));
        olFeature.set('annot', annot);

        let indexSelectedFeature = this.selectedFeatures.findIndex(ftr => ftr.id === annot.id);
        if (indexSelectedFeature >= 0) {
          this.$store.commit(this.imageModule + 'changeAnnotSelectedFeature', {
            indexFeature: indexSelectedFeature,
            annot
          });
        }
      }
    },
    deleteAnnotationHandler(annot) {
      if (this.annotBelongsToLayer(annot) && this.$refs.olSource) {
        let olFeature = this.$refs.olSource.getFeatureById(annot.id);
        if (!olFeature) {
          return;
        }
        this.$refs.olSource.removeFeature(olFeature);

        if (this.selectedFeatures.some(ftr => ftr.id === annot.id)) {
          this.$store.commit(this.imageModule + 'clearSelectedFeatures');
        }
      }
    },

    strategyFactory() {
      return (extent, resolution) => {
        this.lastExtent = extent;

        if (this.$refs.olSource && this.resolution && this.clustered !== null && ( // some features have already been loaded
          !this.clustered && resolution > this.maxResolutionNoClusters // recluster
                    || resolution !== this.resolution && this.clustered)) { // change of resolution while clustering

          // clear loaded extents to force reloading features
          this.$refs.olSource.$source.loadedExtentsRtree_.clear();
        }

        return [extent];
      };
    },

    async fetchAnnotations() {
      let annotations = (await Cytomine.instance.api.get(`/annotation-layers/${this.layer.id}/annotations`)).data;
      annotations.forEach((annotation) => {
        annotation.location = decodeURIComponent(atob(annotation.location));
        annotation.term = [];
        annotation.image = this.image.id;
        annotation.project = this.project.id;
      });

      return annotations;
    },
    async fetchAnnots(extent) {
      [0, 1].forEach(index => {
        if (extent[index] < 0) {
          extent[index] = 0;
        }
      });
      [2, 3].forEach(index => {
        if (this.imageExtent[index] < extent[index]) {
          extent[index] = this.imageExtent[index];
        }
      });

      let annots = await new AnnotationCollection({
        username: !this.layer.isReview ? this.layer.id : null,
        image: this.image.id,
        slices: this.sliceIds,
        reviewed: this.layer.isReview,
        notReviewedOnly: !this.layer.isReview && this.reviewMode,
        bbox: extent.join(),
        showWKT: true,
        showTerm: true,
        showGIS: true,
        showTrack: true,
        showLink: true,
        showImageGroup: true,
        kmeans: true
      }).fetchAll();

      return annots.array;
    },

    updateFeature(feature, annot) {
      let indexSelectedFeature = this.selectedFeatures.findIndex(ftr => ftr.id === feature.getId());
      let isFeatureSelected = indexSelectedFeature !== -1;

      if (!annot) {
        console.log(`Removing annot ${feature.getId()} in layer ${this.layer.id} (external action)`);
        this.$refs.olSource.removeFeature(feature);
        if (isFeatureSelected) {
          this.$store.commit(this.imageModule + 'clearSelectedFeatures');
        }
        return;
      }

      let storedAnnot = feature.get('annot');
      if (!this.clustered && annot.updated === storedAnnot.updated && this.sameTerms(annot.term, storedAnnot.term)) {
        // no modification performed since feature was loaded
        return;
      }

      if (isFeatureSelected) {
        if (this.ongoingEdit) {
          // if feature is selected and under modification, updating it may lead to conflict
          console.log(`Skipping update of selected annot ${annot.id} in layer ${this.layer.id} (ongoing edit)`);
          return;
        }
        console.log(`Updating selected annot ${annot.id} in layer ${this.layer.id} (external action)`);
        this.$store.commit(this.imageModule + 'changeAnnotSelectedFeature', {
          indexFeature: indexSelectedFeature,
          annot
        });
      }

      console.log(`Updating annot ${annot.id} in layer ${this.layer.id} (external action)`);
      feature.set('annot', annot);
      feature.setGeometry(this.format.readGeometry(annot.location));
    },

    async loader(extent = this.lastExtent, resolution = this.resolution) {
      this.resolution = resolution;

      if (!this.layer.visible || !extent) {
        return;
      }

      let arrayAnnots;
      try {
        if (Object.prototype.hasOwnProperty.call(this.layer, 'username')) {
          arrayAnnots = await this.fetchAnnots(extent);
          // Order by size, so bigger ones are always sent to back
          arrayAnnots.sort(
            function (a, b) {
              if (a.area < b.area) {
                return 1;
              } else {
                return -1;
              }
            }
          );
        } else {
          arrayAnnots = await this.fetchAnnotations();
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-fetch-annotations-viewer')});
        return;
      }

      if (!this.$refs.olSource) {
        return;
      }

      let wasClustered = this.clustered;
      if (arrayAnnots.length) {
        this.clustered = arrayAnnots[0].count !== null;
        if (!this.clustered && resolution > this.maxResolutionNoClusters) {
          this.maxResolutionNoClusters = resolution;
        }
      }

      let annots = arrayAnnots.reduce((obj, annot) => {
        obj[annot.id] = annot;
        return obj;
      }, {});
      let seenAnnots = [];

      if (wasClustered !== null && wasClustered !== this.clustered) {
        this.clearFeatures(); // clearing features will retrigger the loader
      } else {
        let features = this.clustered ? this.$refs.olSource.$source.getFeatures()
          : this.$refs.olSource.$source.getFeaturesInExtent(extent);

        features.forEach(feature => {
          this.updateFeature(feature, annots[feature.getId()]);
          seenAnnots.push(feature.getId());
        });
      }

      arrayAnnots.forEach(annot => {
        if (!seenAnnots.includes(annot.id)) {
          this.$refs.olSource.addFeature(this.createFeature(annot));
        }
      });
    },

    loaderFactory() {
      return (extent, resolution) => this.loader(extent, resolution);
    },

    createFeature(annot) {
      let feature = this.format.readFeature(annot.location);
      feature.setId(annot.id);
      feature.set('annot', annot);

      if (this.annotsIdsToSelect.includes(annot.id)) {
        this.$store.dispatch(this.imageModule + 'selectFeature', feature);
      }

      return feature;
    },

    sameTerms(terms1, terms2) {
      if (terms1.length !== terms2.length) {
        return false;
      }
      return terms1.every(term => terms2.includes(term));
    }
  },
  mounted() {
    this.$eventBus.$on('addAnnotation', this.addAnnotationHandler);
    this.$eventBus.$on('selectAnnotationInLayer', this.selectAnnotationHandler);
    this.$eventBus.$on('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$on('reviewAnnotation', this.reviewAnnotationHandler);
    this.$eventBus.$on('editAnnotation', this.editAnnotationHandler);
    this.$eventBus.$on('deleteAnnotation', this.deleteAnnotationHandler);
  },
  beforeDestroy() {
    // unsubscribe from all events
    this.$eventBus.$off('addAnnotation', this.addAnnotationHandler);
    this.$eventBus.$off('selectAnnotationInLayer', this.selectAnnotationHandler);
    this.$eventBus.$off('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$off('reviewAnnotation', this.reviewAnnotationHandler);
    this.$eventBus.$off('editAnnotation', this.editAnnotationHandler);
    this.$eventBus.$off('deleteAnnotation', this.deleteAnnotationHandler);
  }
};
</script>

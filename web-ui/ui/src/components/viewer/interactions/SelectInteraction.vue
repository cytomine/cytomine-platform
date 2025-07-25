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
<vl-interaction-select
  :ident="`select-target-${index}`"
  :filter="filterFunction"
  :features.sync="selectedFeatures"
  :toggle-condition="never"
  :remove-condition="shiftKeyOnly"
  :multi=true
  ref="interactionSelect"
>
  <vl-style-func :factory="styleFunctionFactory" />
</vl-interaction-select>
</template>

<script>
import {isCluster} from '@/utils/style-utils.js';
import {never, shiftKeyOnly} from 'ol/events/condition';

export default {
  name: 'select-interaction',
  props: {
    index: String
  },
  computed: {
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    selectedFeatures: {
      get() {
        return this.imageWrapper.selectedFeatures.selectedFeatures;
      },
      set(value) {
        //used when selecting a vertex of a feature
        let notAnnotations = value.filter(x => !Object.keys(x).includes('id') && x.properties === null);
        if (notAnnotations.length === 1) {
          value = notAnnotations;
          this.$store.commit(this.imageModule + 'setSelectedFeatures', value);
          return;
        }

        value.sort(
          function (a, b) {
            if (a.properties.annot.area > b.properties.annot.area) {
              return 1;
            } else {
              return -1;
            }
          }
        );

        let previousTarget = this.imageWrapper.selectedFeatures.selectionTargetedFeatures;
        let previousSelectedFeature = this.imageWrapper.selectedFeatures.selectedFeatures[0];

        this.$store.commit(this.imageModule + 'setSelectionTargetedFeatures', value);

        //see https://github.com/cytomine/Cytomine-Web-UI/issues/13 for more details of this algorithm
        if (this.imageWrapper.selectedFeatures.selectedFeatures.length === 0) {
          if (value.length >= 1) {
            value = [value[0]];
          }
        } else {
          if (value.length > 1) {
            //if previous selection is in the new target, we will take the first element not yet visited
            if (value.map(x => x.id).includes(previousSelectedFeature.id)) {
              let index = previousTarget.findIndex(x => x.id === previousSelectedFeature.id);
              let visitedFeatures = previousTarget.slice(0, index + 1);
              index = 0;
              for (let i = 0; i < value.length; i++) {
                if (!visitedFeatures.map(x => x.id).includes(value[i].id)) {
                  index = i;
                  break;
                }
              }
              value = [value[index]];
            } else {
              value = [value[0]];
            }
          }
        }

        if (value.length >= 1 && value[0].properties !== null) {
          let annot = value[0].properties.annot;
          if (typeof annot.recordAction === 'function') {
            annot.recordAction();
          }
        }
        this.$store.commit(this.imageModule + 'setSelectedFeatures', value);
        this.$store.commit(this.imageModule + 'setShowSimilarAnnotations', false);
      }
    },
    terms() {
      return this.imageWrapper.style.terms || [];
    },
    styleFunctionFactory() {
      this.imageWrapper.selectedFeatures.selectedFeatures;
      this.imageWrapper.style.layersOpacity;
      this.terms.forEach(term => {
        term.visible;
        term.color;
        term.opacity;
      });
      this.imageWrapper.style.displayNoTerm;
      this.imageWrapper.style.noTermOpacity;
      this.imageWrapper.draw.activeEditTool; // style is different in edit mode (vertices displayed)
      this.imageWrapper.properties.selectedPropertyValues;
      this.imageWrapper.properties.selectedPropertyColor;
      this.imageWrapper.review.reviewMode;
      if (this.imageWrapper.style.wrappedTracks) {
        this.imageWrapper.style.wrappedTracks.forEach(track => {
          track.color;
        });
      }
      return () => {
        return this.$store.getters[this.imageModule + 'genStyleFunction'];
      };
    },
    filterFunction() {
      return feature => !isCluster(feature);
    },
    never() {
      return never;
    },
    shiftKeyOnly() {
      return shiftKeyOnly;
    }
  },
  watch: {
    async styleFunctionFactory() {
      // HACK: style function is not called again when redefined => force the update of style for selected features
      if (this.$refs.interactionSelect && this.$refs.interactionSelect.$interaction) {
        await this.$refs.interactionSelect.$interaction.getFeatures().forEach(ft => ft.changed());
      }
    }
  }
};
</script>

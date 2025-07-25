<template>
  <div class="similar-annotations-playground">
    <vue-draggable-resizable
      class="draggable"
      v-show="displayAnnotDetails && selectedFeature && showSimilarAnnotations"
      :parent="false"
      :resizable="false"
      :x="350"
      :y="0"
      :h="'auto'"
      :w="450"
    >
      <b-loading :is-full-page="false" :active="loading"/>

      <div class="actions">
        <h1>{{ $t('similar-annotations') }}</h1>

        <button
          class="button is-small close"
          v-if="annotation.id !== queryAnnotation.id"
          @click="returnToQueryAnnotation()"
        >
          <i class="fas fa-arrow-circle-left"/>
        </button>

        <button class="button is-small close" @click="showSimilarAnnotations = false">
          <i class="fas fa-times"/>
        </button>
      </div>

      <div class="annotation-content" v-if="!loading">
        <div class="annotation-data" v-for="data in similarities" :key="data.annotation.id">
          <annotation-preview
            :annot="data.annotation"
            :key="data.annotation.id"
            :same-view-on-click="true"
            :show-details="false"
            :show-image-info="false"
            :show-slice-info="false"
            :size="size"
            @select="$emit('select', $event)"
          />

          <div>
            {{ data.distance.toFixed(2) }}%
          </div>
        </div>

        <div class="no-annotation-content" v-if="similarities.length === 0">
          <p>{{ $t('no-similar-annotations-found') }}</p>
        </div>
      </div>

      <div v-if="suggestedTerms.length > 0">
        <div class="actions">
          <h1>{{ $t('suggested-terms') }}</h1>
        </div>

        <div>
          <b-tag class="term-suggestion" v-for="value in suggestedTerms" :key="value[0].id">
            <cytomine-term :term="value[0]"/>
            ({{ value[1] }})
            <button class="button is-small" @click="addTerm(value[0])">
              <span class="icon is-small"><i class="fas fa-plus"/></span>
            </button>
          </b-tag>
        </div>
      </div>
    </vue-draggable-resizable>
  </div>
</template>

<script>
import {Annotation, AnnotationTerm} from 'cytomine-client';

import AnnotationPreview from '@/components/annotations/AnnotationPreview.vue';
import CytomineTerm from '@/components/ontology/CytomineTerm';
import VueDraggableResizable from 'vue-draggable-resizable';

export default {
  name: 'SimilarAnnotation',
  components: {
    AnnotationPreview,
    CytomineTerm,
    VueDraggableResizable,
  },
  props: {
    image: {type: Object},
    index: {type: String, required: true},
    size: {type: Number, default: 64},
  },
  data() {
    return {
      annotations: [],
      loading: true,
      suggestedTerms: [],
    };
  },
  computed: {
    annotation() {
      return this.$store.getters[this.imageModule + 'selectedFeature'].properties.annot;
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    showSimilarAnnotations: {
      get() {
        return this.imageWrapper.selectedFeatures.showSimilarAnnotations;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setShowSimilarAnnotations', value);
      }
    },
    data() {
      return this.imageWrapper.selectedFeatures.similarAnnotations;
    },
    displayAnnotDetails() {
      return this.imageWrapper.selectedFeatures.displayAnnotDetails;
    },
    queryAnnotation() {
      return this.imageWrapper.selectedFeatures.queryAnnotation;
    },
    selectedFeature() {
      return this.$store.getters[this.imageModule + 'selectedFeature'];
    },
    similarities() {
      return this.annotations.map((annotation, index) => ({
        annotation,
        distance: this.data.similarities[index][1]
      }));
    },
    terms() {
      return this.$store.getters['currentProject/terms'] || [];
    },
  },
  methods: {
    async addTerm(term) {
      try {
        await new AnnotationTerm({annotation: this.annotation.id, term: term.id}).save();
        this.$emit('updateTermsOrTracks', this.annotation);
      } catch (error) {
        this.$notify({type: 'error', text: this.$t('notif-error-add-term')});
      }
    },
    countTerm() {
      let termCount = {};
      for (let annotation of this.annotations) {
        for (let term of annotation.term) {
          termCount[term] = (termCount[term] || 0) + 1;
        }
      }
      // Delete already existing terms
      for (let term of this.annotation.term) {
        delete termCount[term];
      }
      this.suggestedTerms = Object.keys(termCount).map((key) => [key, termCount[key]]);
      this.suggestedTerms.sort((a, b) => b[1] - a[1]);
      this.suggestedTerms.forEach((count) => count[0] = this.findTerm(count[0]));
      this.suggestedTerms = this.suggestedTerms.filter((term) => term[0] !== undefined);
      this.suggestedTerms = this.suggestedTerms.slice(0, 3); // Only keep the 3 most frequent terms
    },
    findTerm(id) {
      return this.terms.find((term) => term.id === Number(id));
    },
    async fetchAnnotations() {
      await Promise.all(this.data['similarities'].map(async ([id, _]) => { // eslint-disable-line no-unused-vars
        this.annotations.push(await Annotation.fetch(id));
      }));
    },
    returnToQueryAnnotation() {
      this.$emit('select', {annot: this.queryAnnotation, options: {trySameView: true}});
    }
  },
  async created() {
    this.$eventBus.$on('update-suggested-terms', this.countTerm);
    await this.fetchAnnotations();
    this.countTerm();
    this.loading = false;
  },
  beforeDestroy() {
    this.$eventBus.$off('update-suggested-terms', this.countTerm);
  }
};
</script>

<style scoped>
.actions {
  align-items: center;
  background-color: #e5e5e5;
  border-bottom: 1px solid #b5b5b5;
  border-radius: 5px 5px 0 0;
  display: flex;
  padding: 0.35em;
  text-align: right;
}

.actions .button {
  margin-left: 0.25rem;
  width: 1.75rem;
}

.annotation-content {
  display: flex;
  flex-wrap: wrap;
}

.annotation-data {
  align-items: center;
  display: flex;
  flex-direction: column;
  margin-bottom: 0.5rem;
}

.draggable {
  background: #f2f2f2;
  border-radius: 5px;
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  display: flex;
  flex-direction: column;
  pointer-events: auto;
}

h1 {
  flex: 1;
  font-size: 1rem;
  margin-left: 0.4em;
  padding: 0;
  text-align: left;
}

.no-annotation-content {
  padding: 0.5rem;
}

.similar-annotations-playground {
  top: 3.5rem;
  bottom: 2em;
  left: 3.5rem;
  right: 4.5rem;
  pointer-events: none;
  position: absolute;
}

.similar-annotations-playground .draggable {
  z-index: 20 !important;
}

.term-suggestion {
  flex-direction: column;
  margin: 0.5rem;
}
</style>
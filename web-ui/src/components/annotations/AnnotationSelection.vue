<template>
  <form @submit.prevent="selectAnnotation()">
    <cytomine-modal :active="active" :title="$t('select-annotation')" @close="$emit('update:active', false)">
      <b-loading class="small" :active="loading" :is-full-page="false"/>

      <template v-if="!loading">
        <div class="annotation-content">
          <selectable-annotation
            v-for="annotation in annotations"
            :key="annotation.id"
            :annotation="annotation"
            :images="images"
            :is-selected="selectedAnnotation === annotation.id"
            :users="layersIds"
            @update:selected="selectedAnnotation = $event"
          />
        </div>
      </template>

      <template #footer>
        <button class="button" type="button" @click="cancelAnnotation()">
          {{ $t('button-cancel') }}
        </button>
        <button class="button is-link">
          {{ $t('select') }}
        </button>
      </template>
    </cytomine-modal>
  </form>
</template>

<script>
import {AnnotationCollection} from 'cytomine-client';

import {get} from '@/utils/store-helpers';

import CytomineModal from '@/components/utils/CytomineModal';
import SelectableAnnotation from '@/components/annotations/SelectableAnnotation';

export default {
  name: 'AnnotationSelection',
  props: {
    active: {type: Boolean, default: false},
  },
  data() {
    return {
      annotations: [],
      loading: true,
      selectedAnnotation: null,
    };
  },
  components: {
    CytomineModal,
    SelectableAnnotation,
  },
  computed: {
    project: get('currentProject/project'),
    imagesWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images;
    },
    images() {
      return Object.values(this.imagesWrapper);
    },
    imageIds() {
      return this.images.map(image => image.imageInstance.id);
    },
    layers() {
      let layers = Object
        .values(this.imagesWrapper)
        .map(image => image.layers.selectedLayers)
        .flat();

      return Array.from(new Map(layers.map(item => [item.id, item])).values());
    },
    layersIds() {
      return this.layers.map(layer => layer.id);
    },
    terms() {
      return this.$store.getters['currentProject/terms'] || [{id: 0, name: this.$t('no-term')}];
    }
  },
  methods: {
    cancelAnnotation() {
      this.selectedAnnotation = null;
      this.$emit('update:active', false);
    },
    selectAnnotation() {
      if (!this.selectedAnnotation) {
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-select')});
        return;
      }

      this.$emit('select-annotation', this.selectedAnnotation);
      this.$emit('update:active', false);

      this.selectedAnnotation = null;
    },
    async fetchAnnotations() {
      let annotations = await new AnnotationCollection({
        project: this.project.id,
        image: this.imageIds,
        terms: this.terms.map(term => term.id),
        showWKT: true,
      }).fetchAll();

      this.annotations = annotations.array;
    }
  },
  async created() {
    await this.fetchAnnotations();
    this.loading = false;
  }
};
</script>

<style scoped>
.annotation-content {
  display: flex;
  flex-wrap: wrap;
}
</style>

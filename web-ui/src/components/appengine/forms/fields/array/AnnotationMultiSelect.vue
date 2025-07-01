<template>
  <div>
    <b-loading class="small" :active="loading" :is-full-page="false"/>

    <template v-if="!loading">
      <div class="annotation-content">
        <selectable-annotation
          v-for="annotation in annotations"
          :key="annotation.id"
          :annotation="annotation"
          :images="images"
          :is-selected="selectedAnnotations.includes(annotation)"
          @update:selected="updateSelection($event)"
        />
      </div>
    </template>
  </div>
</template>

<script>
import {AnnotationCollection} from 'cytomine-client';

import {get} from '@/utils/store-helpers';
import SelectableAnnotation from '@/components/annotations/SelectableAnnotation';

export default {
  name: 'AnnotationMultiSelect',
  components: {
    SelectableAnnotation,
  },
  data() {
    return {
      annotations: [],
      loading: true,
      selectedAnnotations: [],
    };
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
  },
  methods: {
    updateSelection(annotation) {
      if (this.selectedAnnotations.includes(annotation)) {
        this.selectedAnnotations = this.selectedAnnotations.filter(item => item !== annotation);
      } else {
        this.selectedAnnotations.push(annotation);
      }
    },
    async fetchAnnotations() {
      let annotations = await new AnnotationCollection({
        project: this.project.id,
        image: this.imageIds,
        showWKT: true,
      }).fetchAll();

      this.annotations = annotations.array;
    },
  },
  watch: {
    selectedAnnotations(annotations) {
      this.$emit('input', annotations.map(annotation => annotation.id));
    }
  },
  async created() {
    await this.fetchAnnotations();
    this.loading = false;
  },
};
</script>

<style scoped>
.annotation-content {
  display: flex;
  flex-wrap: wrap;
}
</style>

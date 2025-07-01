<template>
  <form @submit.prevent="select">
    <cytomine-modal
      :active="active"
      :title="$t('select-image-to-add')"
      @close="$emit('update:active', false)"
    >
      <b-loading class="small" :active="loading" :is-full-page="false"/>

      <template v-if="!loading">
        <div class="image-content">
          <selectable-image
            v-for="image in images"
            :key="image.id"
            :image="image"
            :is-selected="selectedImage === image.id"
            @update:selected="selectedImage = $event"
          />
        </div>
      </template>

      <template #footer>
        <button class="button" type="button" @click="cancel()">
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
import CytomineModal from '@/components/utils/CytomineModal';
import SelectableImage from '@/components/image/SelectableImage';
import {get} from '@/utils/store-helpers';
import {ImageInstanceCollection} from 'cytomine-client';

export default {
  name: 'ImageSelection',
  components: {
    CytomineModal,
    SelectableImage,
  },
  props: {
    active: {type: Boolean, required: true},
    formats: {type: Array, required: false, default: null},
  },
  data() {
    return {
      images: [],
      loading: true,
      selectedImage: null,
    };
  },
  computed: {
    project: get('currentProject/project'),
  },
  async created() {
    await this.fetchImages();
    this.loading = false;
  },
  methods: {
    cancel() {
      this.selectedImage = null;
      this.$emit('update:active', false);
    },
    select() {
      if (!this.selectedImage) {
        this.$notify({type: 'error', text: this.$t('notif-error-image-selection')});
        return;
      }
      this.$emit('select-image', this.selectedImage);
      this.$emit('update:active', false);
      this.selectedImage = null;
    },
    async fetchImages() {
      this.images = (await ImageInstanceCollection.fetchAll({
        filterKey: 'project',
        filterValue: this.project.id,
      })).array;

      if (this.formats) {
        this.images = this.images.filter(image => this.formats.includes(image.contentType));
      }
    }
  },
};
</script>

<style scoped>
.image-content {
  display: flex;
  flex-wrap: wrap;
}
</style>

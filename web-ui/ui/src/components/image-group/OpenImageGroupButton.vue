<template>
  <div class="field has-addons">
    <p class="control">
      <router-link
          :to="viewerURL(images)"
          class="button is-small is-link"
          :disabled="disabled"
      >
        {{$t('button-open')}}
      </router-link>
    </p>
    <p class="control" v-if="images.length > 1">
      <b-dropdown position="is-bottom-left" :disabled="disabled" scrollable :max-height="350">
        <template #trigger>
          <button class="button is-small is-link">
            <i class="fas fa-caret-down"></i>
          </button>
        </template>

        <b-dropdown-item custom :focusable="false">
          {{$t('open-image-group-by-batch-of')}}
          <input v-model.number="batchSize" type="number" place="input" min="1" :max="maxBatchSize"/>
        </b-dropdown-item>
        <hr class="dropdown-divider">
        <b-dropdown-item has-link v-for="batch in batches" :key="`${batch.start}-${batch.end}`">
          <router-link
            :to="viewerURL(batch.images)"
          >
            <template v-if="batch.start+1 !== batch.end">
              {{$t('open-images-from-to', {from: batch.start+1, to: batch.end})}}
            </template>
            <template v-else>
              {{$t('open-image-index', {index: batch.start+1})}}
            </template>
            <ol :start="batch.start+1">
              <li v-for="image in batch.images" :key="`${batch.start}-${image.id}`">
                <image-name :image="image"/>
              </li>
            </ol>
          </router-link>
        </b-dropdown-item>
      </b-dropdown>
    </p>
  </div>
</template>

<script>
import ImageName from '@/components/image/ImageName';
export default {
  name: 'open-image-group-button',
  components: {ImageName},
  data() {
    return {
      batchSize: 4
    };
  },
  props: ['imageGroup'],
  computed: {
    images() {
      return this.imageGroup.imageInstances;
    },
    disabled() {
      return this.images.length === 0;
    },
    maxBatchSize() {
      return this.images.length;
    },
    batches() {
      return Array.from({length: Math.ceil(this.images.length / this.batchSize)}, (v, i) => {
        let start = i * this.batchSize;
        let end = Math.min(start + this.batchSize, this.images.length);
        return {start, end, images: this.images.slice(start, end)};
      });
    }
  },
  watch: {
    maxBatchSize() {
      if (this.batchSize > this.maxBatchSize) {
        this.batchSize = this.maxBatchSize;
      }
    }
  },
  methods: {
    viewerURL(images) {
      let ids = images.map(img => img.id);
      return `/project/${this.imageGroup.project}/image/${ids.join('-')}`;
    },
  },
  created() {
    if (this.batchSize > this.maxBatchSize) {
      this.batchSize = this.maxBatchSize;
    }
  }
};
</script>

<style scoped>
input {
  width: 50px;
  margin-left: 2px;
}

ol {
  padding-left: 1rem;
}
</style>
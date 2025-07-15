<template>
  <div>
    <div class="metadata-viewer">
      <h1>{{ $t('image-metadata') }}</h1>
      <a @click="closeMetadata()">
        <span class="fas fa-times-circle"></span>
      </a>
    </div>

    <b-input
      v-model="searchString"
      :placeholder="$t('search-placeholder')"
      type="search"
      icon="search"
      size="is-small"
    />

    <table class="table">
      <thead>
      <tr>
        <th>Metadata</th>
        <th>Value</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="property in filteredProps" :key="property.id">
        <td><strong>{{ property.key }}</strong></td>
        <td>{{ property.value }}</td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import {Cytomine} from 'cytomine-client';

import {getWildcardRegexp} from '@/utils/string-utils';

export default {
  name: 'MetadataPanel',
  props: {
    index: String,
  },
  data() {
    return {
      metadata: [],
      searchString: '',
    };
  },
  computed: {
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    image() {
      return this.viewerWrapper.images[this.index].imageInstance;
    },
    filteredProps() {
      if (!this.searchString) {
        return this.metadata;
      }
      let regexp = getWildcardRegexp(this.searchString);
      return this.metadata.filter(prop => regexp.test(prop.key) || regexp.test(prop.value));
    },
  },
  methods: {
    async fetchMetadata() {
      this.metadata = (await Cytomine.instance.api.get(
        `imageinstance/${this.image.id}/metadata.json`
      )).data.collection.map(md => ({fullKey: `${md.namespace}.${md.key}`, ...md}));
      this.metadata.sort((a, b) => a.fullKey.localeCompare(b.fullKey));
    },
    closeMetadata() {
      this.$eventBus.$emit('close-metadata');
    }
  },
  async created() {
    try {
      await this.fetchMetadata();
    } catch (error) {
      console.log(error);
    }
  }
};
</script>

<style lang="scss" scoped>
$backgroundPanel: #f2f2f2;

.metadata-viewer {
  display: flex;
}

h1 {
  margin: 0;
  text-align: center;
}

a {
  margin-left: auto;
}

.table {
  background-color: $backgroundPanel;
  margin-bottom: 1em !important;
  font-size: 0.9em;
}

.table thead tr {
  display: block;
}

.table tbody {
  display: block;
  overflow-x: scroll;
  overflow-y: scroll;
  min-width: 30em;
  max-width: 60em;
  min-height: 30em;
  max-height: 30em;
}

.table th {
  width: 1%;
}
</style>

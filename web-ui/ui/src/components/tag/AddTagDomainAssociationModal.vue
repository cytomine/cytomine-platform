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
<cytomine-modal-card :title="$t('associate-tags')" active>
  <b-loading :is-full-page="false" :active="loading" class="small" />
  <template v-if="!loading">
    <b-field>
      <domain-tag-input v-model="selectedTags" :domains="notAssociatedTags" placeholder="search-or-create-tag" allowNew />
    </b-field>
  </template>

  <template #footer>
    <button class="button" @click="$parent.close()">
      {{$t('button-cancel')}}
    </button>
    <button class="button is-link" @click="addAssociations">
      {{$t('button-add')}}
    </button>
  </template>
</cytomine-modal-card>
</template>

<script>

import {TagCollection} from 'cytomine-client';
import DomainTagInput from '@/components/utils/DomainTagInput';
import CytomineModalCard from '@/components/utils/CytomineModalCard';

export default {
  name: 'add-tag-modal',
  props: {
    associatedTags: Array
  },
  components: {
    CytomineModalCard,
    DomainTagInput,
  },
  data() {
    return {
      loading: true,
      tags: [],
      selectedTags: []
    };
  },
  computed: {
    notAssociatedTags() {
      return this.tags.filter(tag => !this.associatedTags.map(u => u.tag).includes(tag.id));
    }
  },
  methods: {
    async addAssociations() {
      this.$emit('addObjects', this.selectedTags);
      this.$parent.close();
    },
    async fetchTags() {
      this.tags = (await TagCollection.fetchAll()).array;
    },

  },
  async created() {
    this.fetchTags();
    this.loading = false;
  }
};
</script>

<style scoped>
>>> .modal-card, >>> .modal-card-body {
  overflow: visible !important;
}

</style>

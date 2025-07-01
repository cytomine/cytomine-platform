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
<cytomine-modal :active="active" :title="$t('add-images')" @close="$emit('update:active', false)">
  <b-loading :is-full-page="false" :active="loading" class="small" />
  <template v-if="!loading">
    <template>
      <b-input class="search-images" v-model="searchString" :placeholder="$t('search-placeholder')"
      type="search" icon="search" />

      <cytomine-table
        :collection="imageCollection"
        :currentPage.sync="currentPage"
        :perPage.sync="perPage"
        :sort.sync="sortField"
        :order.sync="sortOrder"
        :detailed="false"
      >
        <template #default="{row: image}">
          <b-table-column :label="$t('overview')">
            <image-thumbnail :image="image" :size="128" :key="`${image.id}-thumb-128`" :extra-parameters="{Authorization: 'Bearer ' + shortTermToken }"/>
          </b-table-column>

          <b-table-column field="originalFilename" :label="$t('name')" sortable>
            {{ image.originalFilename }}
          </b-table-column>

          <b-table-column field="created" :label="$t('created-on')" sortable>
            {{ Number(image.created) | moment('ll LT') }}
          </b-table-column>

          <b-table-column label=" " centered>
            <button v-if="wasAdded(image)" class="button is-small is-link" disabled>
              {{$t('button-added')}}
            </button>
            <span v-else-if="isInProject(image)">
              {{$t('already-in-project')}}
            </span>
            <button v-else class="button is-small is-link" @click="addImage(image)">
              {{$t('button-add')}}
            </button>
          </b-table-column>
        </template>

        <template #empty>
          <div class="content has-text-grey has-text-centered">
            <p>{{$t('no-image')}}</p>
          </div>
        </template>
      </cytomine-table>
    </template>
  </template>
</cytomine-modal>
</template>

<script>
import {get} from '@/utils/store-helpers';
import {AbstractImageCollection, ImageInstance} from 'cytomine-client';
import CytomineModal from '@/components/utils/CytomineModal';
import CytomineTable from '@/components/utils/CytomineTable';
import ImageThumbnail from '@/components/image/ImageThumbnail';

export default {
  name: 'add-image-modal',
  props: {
    active: Boolean,
  },
  components: {
    ImageThumbnail,
    CytomineTable,
    CytomineModal
  },
  data() {
    return {
      loading: true,
      perPage: 10,
      searchString: '',
      idsAddedImages: [],
      currentPage: 1,
      sortField: 'created',
      sortOrder: 'desc',
    };
  },
  computed: {
    project: get('currentProject/project'),
    shortTermToken: get('currentUser/shortTermToken'),
    imageCollection() {
      let collection = new AbstractImageCollection({
        project: this.project.id,
      });
      if (this.searchString) {
        collection['originalFilename'] = {
          ilike: encodeURIComponent(this.searchString)
        };
      }

      return collection;
    },
  },
  watch: {
    active(val) {
      if (val) {
        this.idsAddedImages = [];
      }
    }
  },
  methods: {
    async addImage(abstractImage) {
      let propsTranslation = {imageName: abstractImage.originalFilename, projectName: this.project.name};
      try {
        let image = await new ImageInstance({baseImage: abstractImage.id, project: this.project.id}).save();
        this.idsAddedImages.push(abstractImage.id);
        this.$emit('addImage', image);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-add-image', propsTranslation)
        });

        let updatedProject = this.project.clone();
        updatedProject.numberOfImages++;
        this.$store.dispatch('currentProject/updateProject', updatedProject);
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-add-image', propsTranslation)
        });
      }
    },
    isInProject(image) {
      return image.inProject;
    },
    wasAdded(image) {
      return this.idsAddedImages.includes(image.id);
    }
  },
  async created() {
    this.loading = false;
  }
};
</script>

<style scoped>
>>> .animation-content {
  max-width: 60% !important;
  width: 60%;
}

>>> .modal-card {
  width: 100%;
  height: 80vh;
}

>>> .image-thumbnail {
  max-height: 4rem;
  max-width: 10rem;
}
</style>

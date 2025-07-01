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
  <cytomine-modal :active="active" :title="$t('add-images-to-image-group')" @close="close()">
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
              <image-thumbnail
                  :extra-parameters="{Authorization: 'Bearer ' + shortTermToken}"
                  :key="image.preview"
                  :size="128"
                  :url="image.preview"
              />
            </b-table-column>

            <b-table-column field="instanceFilename" :label="$t('name')" sortable>
              {{ image.instanceFilename }}
            </b-table-column>

            <b-table-column field="created" :label="$t('created-on')" sortable>
              {{ Number(image.created) | moment('ll LT') }}
            </b-table-column>

            <b-table-column label=" " centered>
              <button v-if="wasAdded(image)" class="button is-small is-link" disabled>
                {{$t('button-added')}}
              </button>
              <span v-else-if="isInImageGroup(image)">
                {{$t('already-in-this-image-group')}}
              </span>
              <span v-else-if="image.imageGroup">
                {{$t('already-in-other-image-group')}}
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
import {ImageInstanceCollection, ImageGroupImageInstance} from 'cytomine-client';
import CytomineModal from '@/components/utils/CytomineModal';
import CytomineTable from '@/components/utils/CytomineTable';
import ImageThumbnail from '@/components/image/ImageThumbnail';

export default {
  name: 'add-to-image-group-modal',
  props: {
    active: Boolean,
    imageGroup: Object,
    programmatic: {type: Boolean, default: false}
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
      let collection = new ImageInstanceCollection({
        filterKey: 'project',
        filterValue: this.project.id,
        withImageGroup: true,
      });
      if (this.searchString) {
        collection['instanceFilename'] = {
          ilike: encodeURIComponent(this.searchString)
        };
      }
      return collection;
    },
    blindMode() {
      return this.$store.state.currentProject.project.blindMode;
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
    close() {
      if (!this.programmatic) {
        this.$emit('update:active', false);
      } else {
        this.$emit('close');
      }
    },
    imageNameNotif(image) {
      return this.blindMode ? image.blindedName : image.instanceFilename;
    },
    async addImage(imageInstance) {
      try {
        let link = await new ImageGroupImageInstance({image: imageInstance.id, group: this.imageGroup.id}).save();
        this.idsAddedImages.push(imageInstance.id);
        this.$emit('addToImageGroup', link);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-group-link-creation', {imageName: this.imageNameNotif(imageInstance)})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-group-link-creation', {imageName: this.imageNameNotif(imageInstance)})
        });
      }
    },
    isInImageGroup(image) {
      return image.imageGroup === this.imageGroup.id;
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

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
<div class="box error" v-if="!configUI['project-image-groups-tab']">
  <h2> {{ $t('access-denied') }} </h2>
  <p>{{ $t('insufficient-permission') }}</p>
</div>
<div class="box error" v-else-if="error">
  <h2> {{ $t('error') }} </h2>
  <p>{{ $t('unexpected-error-info-message') }}</p>
</div>
<div v-else class="content-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <div v-if="!loading" class="panel">
    <p class="panel-heading">
      {{$t('image-groups')}}
      <button v-if="canAddImage" class="button is-link" @click="addImageGroupModal = true">
        {{$t('button-add-image-group')}}
      </button>
    </p>
    <div class="panel-block">
      <div class="search-block">
        <b-input
            class="search-images"
            v-model="searchString"
            :placeholder="$t('search-placeholder')"
            type="search" icon="search"
        />
        <button class="button" @click="toggleFilterDisplay()">
        <span class="icon">
          <i class="fas fa-filter"></i>
        </span>
          <span>
          {{filtersOpened ? $t('button-hide-filters') : $t('button-show-filters')}}
        </span>
          <span v-if="nbActiveFilters" class="nb-active-filters">
          {{nbActiveFilters}}
        </span>
        </button>
      </div>

      <b-collapse :open="filtersOpened">
        <div class="filters">
          <div class="columns">
            <div class="column filter is-half">
              <div class="filter-label">
                {{$t('images')}}
              </div>
              <div class="filter-body">
                <cytomine-slider v-model="boundsImages" :max="maxNbImages" />
              </div>
            </div>

<!--            <div class="column filter">-->
<!--              <div class="filter-label">-->
<!--                {{$t('annotation-links')}}-->
<!--              </div>-->
<!--              <div class="filter-body">-->
<!--                <cytomine-slider v-model="boundsAnnotationLinks" :max="maxNbAnnotationLinks" />-->
<!--              </div>-->
<!--            </div>-->
          </div>

        </div>
      </b-collapse>

      <b-table
          :data="filteredImageGroups"
          :paginated="true"
          :current-page.sync="currentPage"
          :per-page="perPage"
          pagination-size="is-small"
          detailed
          detail-key="id"
          :opened-detailed.sync="openedDetails"
          :default-sort="sort.field"
          :default-sort-direction="sort.order"
          @sort="updateSort"
      >
        <template #default="{row: imageGroup}">
          <b-table-column :label="$t('overview')" width="100">
            <router-link
                v-if="imageGroup.imageInstances.length > 0"
                :to="viewerURL(imageGroup)"
            >
              <image-group-preview :image-group="imageGroup" :key="`preview-${imageGroup.id}`"/>
            </router-link>
          </b-table-column>

          <b-table-column
              field="name"
              :label="$t('name')"
              sortable
              width="400"
          >
            <router-link :to="viewerURL(imageGroup)">
              {{imageGroup.name}}
            </router-link>
          </b-table-column>

          <b-table-column
            field="numberOfImages"
            :label="$t('images')" centered sortable width="150"
          >
            {{ imageGroup.numberOfImages }}
          </b-table-column>

<!--          <b-table-column-->
<!--            field="numberOfAnnotationLinks"-->
<!--            :label="$t('annotation-links')" centered sortable width="150"-->
<!--          >-->
<!--            {{ imageGroup.numberOfAnnotationLinks }}-->
<!--          </b-table-column>-->

          <b-table-column label=" " centered width="150">
            <open-image-group-button :image-group="imageGroup" :key="`open-ig-${imageGroup.id}`"/>
          </b-table-column>
        </template>

        <template #detail="{row: imageGroup}">
          <image-group-details
              :image-group="imageGroup"
              :excludedProperties="excludedProperties"
              editable
              @delete="refreshData"
              @deleteImage="refreshData"
              @addToImageGroup="refreshData"
          />
        </template>

        <template #empty>
          <div class="content has-text-grey has-text-centered">
            <p>{{$t('no-image-group')}}</p>
          </div>
        </template>

        <template #bottom-left>
          <b-select v-model="perPage" size="is-small">
            <option value="10">{{$t('count-per-page', {count: 10})}}</option>
            <option value="25">{{$t('count-per-page', {count: 25})}}</option>
            <option value="50">{{$t('count-per-page', {count: 50})}}</option>
            <option value="100">{{$t('count-per-page', {count: 100})}}</option>
          </b-select>
        </template>
      </b-table>
    </div>

    <add-image-group-modal :active.sync="addImageGroupModal" @newImageGroup="newImageGroup" />
  </div>
</div>
</template>

<script>
import {get, sync, syncBoundsFilter} from '@/utils/store-helpers';

import CytomineSlider from '@/components/form/CytomineSlider';

import ImageGroupDetails from '@/components/image-group/ImageGroupDetails';
import ImageGroupPreview from '@/components/image-group/ImageGroupPreview';
import AddImageGroupModal from '@/components/image-group/AddImageGroupModal';
import AddToImageGroupModal from '@/components/image-group/AddToImageGroupModal';
import OpenImageGroupButton from '@/components/image-group/OpenImageGroupButton';

import {ImageGroupCollection} from 'cytomine-client';
import {getWildcardRegexp} from '@/utils/string-utils';
import {isBetweenBounds} from '@/utils/bounds';


// store options to use with store helpers to target projects/currentProject/listImages module
const storeOptions = {rootModuleProp: 'storeModule'};
// redefine helpers to use storeOptions and correct module path
const localSyncBoundsFilter = (filterName, maxProp) => syncBoundsFilter(null, filterName, maxProp, storeOptions);


export default {
  name: 'list-image-groups',
  components: {
    OpenImageGroupButton,
    ImageGroupPreview,
    ImageGroupDetails,
    CytomineSlider,
    AddImageGroupModal
  },
  data() {
    return {
      loading: true,
      error: false,
      excludedProperties: ['overview'],
      imageGroups:[],
      addImageGroupModal: false
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    configUI: get('currentProject/configUI'),
    project: get('currentProject/project'),
    blindMode() {
      return this.project.blindMode;
    },
    canManageProject() {
      return this.$store.getters['currentProject/canManageProject'];
    },
    canAddImage() {
      return !this.currentUser.guestByNow && (this.canManageProject || !this.project.isReadOnly);
    },

    storeModule() { // path to the vuex module in which state of this component is stored (projects/currentProject/listImages)
      return this.$store.getters['currentProject/currentProjectModule'] + 'listImageGroups';
    },

    searchString: sync('searchString', {...storeOptions, debounce: 500}),
    filtersOpened: sync('filtersOpened', storeOptions),

    maxNbImages() {
      return Math.max(10, ...this.imageGroups.map(ig => ig.numberOfImages));
    },
    maxNbAnnotationLinks() {
      return 100; //Math.max(100, ...this.imageGroups.map(ig => ig.numberOfAnnotationLinks));
    },

    boundsImages: localSyncBoundsFilter('boundsImages', 'maxNbImages'),
    boundsAnnotationLinks: localSyncBoundsFilter('boundsAnnotationLinks', 'maxNbAnnotationLinks'),

    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    filteredImageGroups() {
      let filtered = this.imageGroups;

      if (this.searchString) {
        filtered = filtered.filter(group => this.regexp.test(group.name));
      }

      filtered = filtered.filter(group => {
        return isBetweenBounds(group.numberOfImages, this.boundsImages) /*&&
          isBetweenBounds(group.numberOfAnnotationLinks, this.boundsAnnotationLinks)*/;
      });

      return filtered;
    },


    nbActiveFilters() {
      return this.$store.getters[this.storeModule + '/nbActiveFilters'];
    },
    nbEmptyFilters() {
      return this.$store.getters[this.storeModule + '/nbEmptyFilters'];
    },

    currentPage: sync('currentPage', storeOptions),
    perPage: sync('perPage', storeOptions),
    sort: sync('sort', storeOptions),
    openedDetailsStore: get('openedDetails', storeOptions),
    openedDetails: { // HACK cannot use sync because buefy modifies the property => vuex warning because modif outside store
      get() {
        return this.openedDetailsStore.slice();
      },
      set(value) {
        this.$store.commit(this.storeModule + '/setOpenedDetails', value);
      }
    }
  },
  methods: {
    async fetchImageGroups() {
      this.imageGroups = (await ImageGroupCollection.fetchAll({
        filterKey: 'project',
        filterValue: this.project.id,
      })).array;
    },

    async refreshData() {
      try {
        await this.fetchImageGroups();

        // if an item has been deleted, the currentPage value might not be valid => reinitialize it
        if ((this.currentPage - 1) * this.perPage >= this.filteredImageGroups.length) {
          this.currentPage = 1;
        }
      } catch (error) {
        console.log(error);
        this.error = true;
      }
    },

    toggleFilterDisplay() {
      this.filtersOpened = !this.filtersOpened;
    },

    updateSort(field, order) {
      this.sort = {field, order};
    },

    viewerURL(imageGroup) {
      let ids = imageGroup.imageInstances.map(img => img.id);
      return `/project/${imageGroup.project}/image/${ids.join('-')}`;
    },

    newImageGroup(imageGroup) {
      this.refreshData();
      this.addImageGroupModal = false;
      this.$buefy.modal.open({
        parent: this,
        component: AddToImageGroupModal,
        hasModalCard: true,
        props: {imageGroup, programmatic: true, active: true},
        events: {
          'addToImageGroup': this.refreshData,
        }
      });

    }
  },
  async created() {
    try {
      await Promise.all([
        this.fetchImageGroups()
      ]);
      this.loading = false;
    } catch (error) {
      console.log(error);
      this.error = true;
    }
  }
};
</script>

<style scoped>
.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-block {
  display: flex;
}

>>> .search-images {
  max-width: 30rem;
  margin-right: 1rem;
}

>>> td, >>> th {
  vertical-align: middle !important;
}
</style>

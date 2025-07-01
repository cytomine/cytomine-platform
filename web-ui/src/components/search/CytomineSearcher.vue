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
<div :class="['navbar-item', 'search', displayResults ? 'is-active' : '']" v-click-outside="deactivate">
  <b-field class="no-margin" :type="error ? 'is-danger' : null">
    <b-input
      class="global-search"
      v-model="searchString"
      :placeholder="$t('search-placeholder')"
      type="search"
      icon="search"
      @click.native="activate()"
      :loading="loading"
      :disabled="error"
    />
    <p class="control">
      <router-link class="button" to="/advanced-search" active-class="router-link-active" @click.native="deactivate">+</router-link>
    </p>
  </b-field>

  <div class="navbar-dropdown search-results" v-show="true">
    <h2>{{$t('project')}} ({{filteredProjects.length}})</h2>
    <p v-if="filteredProjects.length > 0">
      <router-link
        v-for="project in subsetProjects"
        :key="project.id"
        :to="`/project/${project.id}`"
        class="navbar-item"
        v-html="highlightedName(project.name)"
        @click.native="deactivate"
      />
      <a v-if="moreProjects" class="navbar-item">...</a>
    </p>
    <span v-else class="navbar-item no-result">{{$t('no-project')}}</span>

    <h2>{{$t('images')}} ({{filteredImages.length}})</h2>
    <p v-if="filteredImages.length > 0">
      <router-link
        v-for="img in subsetImages"
        :key="img.id"
        :to="`/project/${img.project}/image/${img.id}`"
        class="navbar-item"
        @click.native="deactivate"
        v-html="htmlImageName(img)"
      />
      <a v-if="moreImages" class="navbar-item">...</a>
    </p>
    <span v-else class="navbar-item no-result">{{$t('no-image')}}</span>

    <div v-if="moreImages || moreProjects" class="search-view-all">
      <router-link class="button is-small" :to="`/advanced-search/${searchString}`" @click.native="deactivate">
        {{$t('button-view-all')}} ({{totalNbResults}})
      </router-link>
    </div>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import {ImageInstanceCollection, ProjectCollection} from 'cytomine-client';
import {getWildcardRegexp} from '@/utils/string-utils';

export default {
  name: 'cytomine-searcher',
  data() {
    return {
      isActive: false,
      loading: false,
      error: false,
      searchString: '',
      projects: [],
      images: [],
      maxNbDisplayed: 5
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    displayResults() {
      return this.isActive && !this.error && this.searchString.length > 0;
    },
    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    filteredProjects() {
      if (!this.searchString) {
        return this.projects;
      }

      return this.projects.filter(project => {
        return this.regexp.test(project.name);
      });
    },
    subsetProjects() {
      return this.filteredProjects.slice(0, this.maxNbDisplayed);
    },
    moreProjects() {
      return this.filteredProjects > this.subsetProjects;
    },
    filteredImages() {
      if (!this.searchString) {
        return this.images;
      }

      return this.images.filter(image => {
        return this.regexp.test(this.imageName(image));
      });
    },
    subsetImages() {
      return this.filteredImages.slice(0, this.maxNbDisplayed);
    },
    moreImages() {
      return this.filteredImages > this.subsetImages;
    },
    totalNbResults() {
      return this.filteredImages.length + this.filteredProjects.length;
    }
  },
  methods: {
    async fetchImages() {
      this.images = await ImageInstanceCollection.fetchAllLight();
    },
    async fetchProjects() {
      this.projects = (await new ProjectCollection({
        light: true,
        filterKey: 'user',
        filterValue: this.currentUser.id
      }).fetchAll()).array;
    },
    async activate() {
      if (!this.isActive) {
        try {
          this.loading = true;
          await Promise.all([
            this.fetchImages(),
            this.fetchProjects()
          ]);
        } catch (error) {
          console.log(error);
          this.error = true;
        }
        this.loading = false;
        this.isActive = true;
      }
    },
    deactivate() {
      this.isActive = false;
    },
    imageName(image) {
      return String(image.blindedName || image.instanceFilename);
    },
    highlightedName(value) {
      return value.replace(this.regexp, '<strong>$1</strong>');
    },
    htmlImageName(img) {
      let blindIndication = img.blindedName ? `<span class="blind">[${this.$t('blinded-name-indication')}] </span>` : '';
      let inProject = `<span class="in-project">(${this.$t('in-project', {projectName: img.projectName})})</span>`;
      return `${blindIndication}${this.highlightedName(this.imageName(img))}&nbsp;${inProject}`;
    }
  }
};
</script>

<style scoped>
.navbar-item.search {
  height: 100%;
}

.navbar-item:not(.is-active) .navbar-dropdown { /* display dropdown if inactive even on mobile */
  display: none;
}

.navbar-dropdown.search-results h2 {
  background: #f1f1f1;
  text-transform: uppercase;
  font-size: 0.9em;
  font-weight: 600;
  padding: 0.2em 0 0.3em 3em;
  border-top: 1px solid #e3e3e3;
  margin-bottom: 0;
}

.navbar-dropdown.search-results .navbar-item {
  font-weight: normal;
  padding: 0.2em 1em 0.2em 1.75em;
  white-space: pre;
}

.search-view-all {
  border-top: 1px solid #e3e3e3;
  padding-top: 0.5em;
  text-align: center;
}

.no-result {
  color: grey;
}

>>> .blind {
  font-size: 0.9em;
  text-transform: uppercase;
}

.global-search {
  max-width: 15em;
}

.no-margin {
  margin: 0 !important;
}

>>> .global-search .fas {
  padding-right: 0px !important;
}
</style>

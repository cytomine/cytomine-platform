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
<div class="content-wrapper">
  <b-loading :is-full-page="false" :active.sync="loading" />

  <template v-if="!loading">
    <div v-if="welcomeMessage" class="box" v-html="welcomeMessage"></div>

    <div class="columns">
      <div class="column is-two-thirds">
        <div class="box">
          <h2> {{ $t('recently-opened') }} </h2>
          <b-message v-if="!recentProjects" type="is-danger" has-icon icon-size="is-small">
            {{$t('failed-fetch-recent-projects')}}
          </b-message>
          <b-table v-else :data="recentProjects">

            <template #default="{row: project}">
              <b-table-column :label="$t('project')" width="100" centered>
                <router-link class="project-name" :to="`/project/${project.id}`">
                  {{ project.name }}
                </router-link>
              </b-table-column>

              <b-table-column :label="$t('images')" width="400">
                <list-images-preview :project="project" />
              </b-table-column>
            </template>

            <template #empty>
              <div class="content has-text-grey has-text-centered">
                <p>{{$t('no-recent-project')}}</p>
              </div>
            </template>
          </b-table>

          <p class="has-text-centered all-projects">
            <router-link class="button is-link" to="/projects">{{$t('view-all-projects')}}</router-link>
          </p>
        </div>
      </div>

      <div class="column right-column">
        <div class="box stats">
          <h2> {{ $t('statistics') }} </h2>
          <table class="table is-fullwidth">
            <tbody>
              <tr>
                <td>{{projects ? projects.length : "?"}}</td>
                <td>{{$t('projects')}}</td>
                <td>
                  <v-popover>
                    <i class="fas fa-info-circle"></i>
                    <template #popover>
                      <p>{{$t('number-projects-info-message')}}</p>
                    </template>
                  </v-popover>
                </td>
              </tr>
              <tr>
                <td>{{nbImages != null ? nbImages : "?"}}</td>
                <td>{{$t('images')}}</td>
                <td>
                  <v-popover>
                    <i class="fas fa-info-circle"></i>
                    <template #popover>
                      <p>{{$t('number-images-info-message')}}</p>
                    </template>
                  </v-popover>
                </td>
              </tr>
              <tr>
                <td>{{nbUserAnnots != null ? nbUserAnnots : "?"}}</td>
                <td>{{$t('user-annotations')}}</td>
                <td>
                  <v-popover>
                    <i class="fas fa-info-circle"></i>
                    <template #popover>
                      <p>{{$t('number-annotations-info-message')}}</p>
                    </template>
                  </v-popover>
                </td>
              </tr>
              <tr>
                <td>{{nbReviewed != null ? nbReviewed : "?"}}</td>
                <td>{{$t('reviewed-annotations')}}</td>
                <td>
                  <v-popover>
                    <i class="fas fa-info-circle"></i>
                    <template #popover>
                      <p>{{$t('number-reviewed-annotations-info-message')}}</p>
                    </template>
                  </v-popover>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="box">
          <h2> {{ $t('last-opened-image') }} </h2>
          <image-preview
            v-if="lastOpenedImage"
            :image="lastOpenedImage"
            :fullHeightCard="false"
            :showProject="true"
            :blindMode="lastOpenedImage.blindMode"
          />
          <div class="has-text-grey has-text-centered" v-else>
            {{$t('no-recent-image')}}
          </div>
        </div>
      </div>
    </div>
  </template>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import ListImagesPreview from '@/components/image/ListImagesPreview';
import ImagePreview from '@/components/image/ImagePreview';

import {ImageInstanceCollection, ProjectCollection, Configuration} from 'cytomine-client';

import constants from '@/utils/constants.js';

export default {
  name: 'global-dashboard',
  components: {
    ListImagesPreview,
    ImagePreview
  },
  props: {
    nbRecent: {
      type: Number,
      default: 3
    }
  },
  data() {
    return {
      projects: null,
      recentProjectsId: null,
      recentImages: [],
      nbUserAnnots: null,
      nbReviewed: null,
      welcomeMessage: null,
      loading: true
    };
  },
  computed: {
    currentUser: get('currentUser/user'),

    nbImages() {
      if (!this.projects) {
        return;
      }

      return this.projects.array.reduce((count, project) => {
        return count + project.numberOfImages;
      }, 0);
    },
    recentProjects() {
      if (!this.recentProjectsId || !this.projects) {
        return;
      }

      let array = [];
      this.recentProjectsId.forEach(id => {
        let project = this.projects.array.find(project => project.id === id);
        if (project) {
          array.push(project);
        }
      });
      return array;
    },
    lastOpenedImage() {
      if (this.recentImages && this.recentImages.length > 0 && this.projects) {
        let lastOpened = this.recentImages[0];
        let project = this.projects.array.find(project => project.id === lastOpened.project);
        if (project) {
          lastOpened.projectName = project.name;
          lastOpened.blindMode = project.blindMode;
        }
        return lastOpened;
      }
      return false;
    }
  },
  methods: {
    async fetchProjects() {
      this.projects = await ProjectCollection.fetchAll();
    },
    async fetchNbAnnots() {
      this.nbUserAnnots = await this.currentUser.fetchNbAnnotations();
    },
    async fetchNbReviewedAnnots() {
      this.nbReviewed = await this.currentUser.fetchNbAnnotations(true);
    },
    async fetchRecentProjectsId() {
      let listRecent = await ProjectCollection.fetchLastOpened(this.nbRecent);
      this.recentProjectsId = listRecent.map(recent => recent.id);
    },
    async fetchRecentImages() {
      this.recentImages = await ImageInstanceCollection.fetchLastOpened({max: 1});
    },
    async fetchWelcomeMessage() {
      try {
        this.welcomeMessage = (await Configuration.fetch(constants.CONFIG_KEY_WELCOME)).value;
      } catch (error) {
        // no welcome message defined
      }
    }
  },
  async created() {
    await Promise.all([
      this.fetchProjects(),
      this.fetchNbAnnots(),
      this.fetchNbReviewedAnnots(),
      this.fetchRecentProjectsId(),
      this.fetchRecentImages(),
      this.fetchWelcomeMessage()
    ].map(p => p.catch(e => console.log(e)))); // ignore errors (handled in template) and ensure all promises finish, even if some errors occur in the process
    this.loading = false;
  }
};
</script>

<style scoped>
td {
  vertical-align: middle !important;
}

a.project-name {
  font-weight: 600;
  font-size: 1.1rem;
}

.all-projects {
  margin-top: 1rem;
}

.right-column {
  display: flex;
  flex-direction: column;
}

.stats {
  padding-bottom: 2.5rem;
}

td:first-child {
  font-weight: 600;
  text-align: right;
  width: 25%;
}

.fas.fa-info-circle {
  cursor: pointer;
}
</style>

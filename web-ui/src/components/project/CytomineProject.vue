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
  <div class="box error" v-if="permissionError || notFoundError">
    <h2> {{ $t(permissionError ? 'access-denied' : 'not-found') }} </h2>
    <p> {{ $t(permissionError ? 'insufficient-permission' : 'not-found-error') }} </p>
  </div>
  <div v-else class="project-container">
    <project-sidebar v-if="!loading" :key="idProject" />

    <div class="app-content">
      <b-loading :is-full-page="false" :active="loading" />
      <router-view v-if="!loading" />
    </div>
  </div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import ProjectSidebar from './ProjectSidebar.vue';
import projectModuleModel from '@/store/modules/project';

export default {
  name: 'cytomine-project',
  components: {ProjectSidebar},
  data() {
    return {
      loading: true,
      permissionError: false,
      notFoundError: false
    };
  },
  computed: {
    idProject() {
      return this.$route.params.idProject;
    },
    project: get('currentProject/project'),
    projectModule() {
      return this.$store.state.projects[this.idProject];
    }
  },
  watch: {
    async idProject() {
      this.loading = true;
      await this.loadProject();
    },
    projectModule() {
      if (!this.projectModule) {
        console.log('Project closed from external source');
        this.$store.commit('currentProject/resetState');
        this.$router.push('/projects');
      }
    }
  },
  methods: {
    async loadProject() {
      try {
        if (!this.$store.state.projects[this.idProject]) { // module does not exist yet
          this.$store.registerModule(['projects', this.idProject], projectModuleModel);
        }
        await this.$store.dispatch('currentProject/loadProject', this.idProject);
        this.loading = false;
      } catch (error) {
        console.log(error);
        if (error.response && error.response.status === 403) {
          this.permissionError = true;
        } else {
          this.notFoundError = true;
        }
      }
    },
    deleteAnnotationEventHandler(annot) {
      let updatedProject = this.$store.state.currentProject.project.clone();
      if (annot.type === 'UserAnnotation') {
        updatedProject.numberOfAnnotations--;
      } else {
        updatedProject.numberOfReviewedAnnotations--;
      }

      this.$store.dispatch('currentProject/updateProject', updatedProject);
    }
  },
  async created() {
    await this.loadProject();
  },
  mounted() {
    this.$eventBus.$on('deleteAnnotation', this.deleteAnnotationEventHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('deleteAnnotation', this.deleteAnnotationEventHandler);
  }
};
</script>

<style>
.project-container {
  display: flex;
  height: 100%;
  flex: 1;
  background: #d4d4d4;
  overflow-y: auto;
  position: relative;
}

.app-content {
  flex: 1;
  position: relative;
  overflow-y: auto;
}
</style>

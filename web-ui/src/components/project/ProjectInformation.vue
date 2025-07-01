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
<div class="box error" v-if="!configUI['project-information-tab']">
  <h2> {{ $t('access-denied') }} </h2>
  <p>{{ $t('insufficient-permission') }}</p>
</div>
<div v-else class="content-wrapper">
  <div class="box">
    <project-details
      :project="project"
      :excluded-properties="excludedProperties"
      editable
      @update="updateProject"
      @delete="deleteProject()"
    />
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import ProjectDetails from './ProjectDetails';
import {Project} from 'cytomine-client';
export default {
  name: 'project-information',
  components: {ProjectDetails},
  data() {
    return {
      excludedProperties: [
        'imagesPreview'
      ]
    };
  },
  computed: {
    project: get('currentProject/project'),
    configUI: get('currentProject/configUI')
  },
  methods: {
    updateProject(updatedProject) {
      this.$store.dispatch('currentProject/updateProject', updatedProject);
    },
    async deleteProject() {
      try {
        await Project.delete(this.project.id);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-project-deletion', {projectName: this.project.name})
        });
        this.$router.push('/projects');
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-project-deletion', {projectName: this.project.name})
        });
      }
    }
  },
  created() {
    this.$store.dispatch('currentProject/reloadProject');
  }
};
</script>

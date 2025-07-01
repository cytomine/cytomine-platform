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
<div>
  <h2>{{$t('editing-mode')}}</h2>
  <p class="explanation">{{$t('editing-mode-explanation')}}</p>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-radio v-model="editingMode" native-value="CLASSIC">
        {{$t('classic')}}
      </b-radio>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('classic-mode-explanation')}}
      </b-message>
    </div>
  </div>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-radio v-model="editingMode" native-value="RESTRICTED">
        {{$t('restricted')}}
      </b-radio>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('restricted-mode-explanation')}}
      </b-message>
    </div>
  </div>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-radio v-model="editingMode" native-value="READ-ONLY">
        {{$t('read-only')}}
      </b-radio>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('read-only-mode-explanation')}}
      </b-message>
    </div>
  </div>

  <h2>{{$t('blind-mode')}}</h2>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-checkbox v-model="blindMode">
        {{$t('blind-mode')}}
      </b-checkbox>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('blind-mode-explanation')}}
      </b-message>
    </div>
  </div>

  <h2>{{$t('annotation-layers')}}</h2>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-checkbox v-model="hideManagersLayers">
        {{$t('hide-managers-layers')}}
      </b-checkbox>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('hide-managers-layers-explanation')}}
      </b-message>
    </div>
  </div>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-checkbox v-model="hideContributorsLayers">
        {{$t('hide-contributors-layers')}}
      </b-checkbox>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('hide-contributors-layers-explanation')}}
      </b-message>
    </div>
  </div>

  <h2>{{$t('default-annotation-layers')}}</h2>

  <b-field grouped>
    <b-select size="is-small" :placeholder="$t('select-layer-placeholder')" v-model="layerToAdd">
      <option v-for="layer in unselectedLayers" :key="layer.id" :value="layer">
        {{layer.fullName}}
      </option>
    </b-select>

    <button class="button is-small" :disabled="!layerToAdd" @click="addDefaultLayer()">
      {{$t('button-add')}}
    </button>
  </b-field>

  <table v-if="selectedLayers.length > 0" class="table">
    <tbody>
      <tr>
        <th>{{$t('layer')}}</th>
        <th>{{$t('hide-by-default')}}</th>
        <th></th>
      </tr>
      <tr v-for="(layer, idx) in selectedLayers" :key="layer.id">
        <td>{{layer.fullName}}</td>
        <td class="is-centered">
          <b-checkbox v-model="defaultLayers[idx].hideByDefault" size="is-small" @input="saveDefaultLayer(idx)" />
        </td>
        <td>
          <button class="button is-small" @click="deleteDefaultLayer(idx)"> {{$t('button-remove')}}</button>
        </td>
      </tr>
    </tbody>
  </table>

  <h2>{{$t('default-property')}}</h2>
  <default-property />

  <h2>{{$t('download-permission')}}</h2>

  <div class="columns">
    <div class="column is-one-quarter">
      <b-checkbox v-model="imagesDownloadable">
        {{$t('images_downloadable_by_contributor')}}
      </b-checkbox>
    </div>
    <div class="column">
      <b-message type="is-info" has-icon size="is-small">
        {{$t('images_downloadable_by_contributor-explanation')}}
      </b-message>
    </div>
  </div>


  <h2>Actions</h2>
  <project-actions :project="project" size="is-normal" @update="externalProjectUpdate" @delete="deleteProject()" />
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import ProjectActions from '../ProjectActions';
import DefaultProperty from './DefaultProperty';
import {Project, ProjectDefaultLayer, ProjectDefaultLayerCollection} from 'cytomine-client';

export default {
  name: 'general-configuration',
  components: {
    ProjectActions,
    DefaultProperty
  },
  data() {
    return {
      editingMode: '',
      blindMode: null,
      hideManagersLayers: null,
      hideContributorsLayers: null,
      imagesDownloadable: null,

      layerToAdd: null,
      defaultLayers: []
    };
  },
  computed: {
    project: get('currentProject/project'),
    layers: get('currentProject/members'),

    currentEditingMode() {
      return this.project.isReadOnly ? 'READ-ONLY' : this.project.isRestricted ? 'RESTRICTED' : 'CLASSIC';
    },

    selectedLayers() {
      return this.defaultLayers.map(defaultLayer => {
        let layer = this.layers.find(layer => layer.id === defaultLayer.user);
        return {...defaultLayer, ...layer};
      });
    },
    unselectedLayers() {
      let selectedLayersIds = this.defaultLayers.map(layer => layer.user);
      return this.layers.filter(layer => !selectedLayersIds.includes(layer.id)).sort((a, b) => (a.fullName < b.fullName) ? -1 : 1);
    }
  },
  watch: {
    editingMode(mode) {
      if (mode === this.currentEditingMode) {
        return;
      }
      this.updateProject({isReadOnly: mode === 'READ-ONLY', isRestricted: mode === 'RESTRICTED'});
    },

    blindMode() {
      if (this.blindMode === this.project.blindMode) {
        return;
      }
      this.updateProject({blindMode: this.blindMode});
    },

    hideManagersLayers() {
      if (this.hideManagersLayers === this.project.hideAdminsLayers) {
        return;
      }
      this.updateProject({hideAdminsLayers: this.hideManagersLayers});
    },

    hideContributorsLayers() {
      if (this.hideContributorsLayers === this.project.hideUsersLayers) {
        return;
      }
      this.updateProject({hideUsersLayers: this.hideContributorsLayers});
    },

    imagesDownloadable() {
      if (this.imagesDownloadable === this.project.areImagesDownloadable) {
        return;
      }
      this.updateProject({areImagesDownloadable: this.imagesDownloadable});
    }
  },
  methods: {
    initData() {
      this.editingMode = this.currentEditingMode;
      this.blindMode = this.project.blindMode;
      this.hideManagersLayers = this.project.hideAdminsLayers;
      this.hideContributorsLayers = this.project.hideUsersLayers;
      this.imagesDownloadable = this.project.areImagesDownloadable;
    },

    async fetchDefaultLayers() {
      this.defaultLayers = (await ProjectDefaultLayerCollection.fetchAll({
        filterKey: 'project',
        filterValue: this.project.id
      })).array;
    },

    async updateProject(newProps) {
      let updatedProject = this.project.clone();
      updatedProject.populate(newProps);
      try {
        await updatedProject.save();
        this.$store.commit('currentProject/setProject', updatedProject);
      } catch (error) {
        console.log(error);
        this.initData(); // reset data
        this.$notify({type: 'error', text: this.$t('notif-error-general-config-update')});
      }
    },

    async addDefaultLayer() {
      if (!this.layerToAdd) {
        return;
      }

      try {
        let defaultLayer = new ProjectDefaultLayer({
          project: this.project.id,
          user: this.layerToAdd.id
        });
        await defaultLayer.save();
        this.defaultLayers.push(defaultLayer);
        this.layerToAdd = null;
      }  catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-default-layer-add')});
      }
    },

    async saveDefaultLayer(idx) {
      try {
        await this.defaultLayers[idx].save();
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-default-layer-update')});
      }
    },

    async deleteDefaultLayer(idx) {
      try {
        await this.defaultLayers[idx].delete();
        this.defaultLayers.splice(idx, 1);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-default-layer-delete')});
      }
    },

    externalProjectUpdate(updatedProject) {
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
  async created() {
    this.initData();
    this.fetchDefaultLayers();
  }
};
</script>

<style scoped>
h2 {
  color: #444;
  border-bottom: 1px solid #ddd;
  margin-top: 1.5em;
}

h2:first-child {
  margin-top: 0;
}

.columns {
  align-items: center;
}

.column {
  padding: 0.8em;
}

.column:first-child {
  padding-left: 3.5em;
}

.explanation {
  margin-bottom: 1.4em;
}

.is-centered {
  text-align: center;
}

th, td {
  padding: 0.5em 2em !important;
}

>>> .message-body {
  padding: 1em !important;
}

>>> select, >>> input[type=text] {
  width: 26em;
}
</style>

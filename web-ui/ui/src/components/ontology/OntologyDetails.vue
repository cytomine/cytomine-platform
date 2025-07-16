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
    <b-loading :is-full-page="false" :active="loading" />
    <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
      {{$t('unexpected-error-info-message')}}
    </b-message>
    <div v-else-if="!loading" class="ontology-details-wrapper">
      <table class="table is-fullwidth">
        <tbody>
        <tr>
          <td colspan="2">
            <strong>{{$t('terms')}}</strong>
            <b-message type="is-info" has-icon icon-size="is-small">
            {{$t('hierarchical-drag-drop-term')}}
          </b-message>
          <ontology-tree :ontology="fullOntology" :allowSelection="false" :allowDrag="canEdit" :allowEdition="canEdit">
              <template #no-result>
                <em class="has-text-grey">{{$t('no-term')}}</em>
              </template>
            </ontology-tree>
          </td>
        </tr>
        <tr v-if="currentAccount.isDeveloper">
          <td><strong>{{$t('id')}}</strong></td>
          <td>{{ontology.id}}</td>
        </tr>
        <tr>
          <td><strong>{{$t('projects')}}</strong></td>
          <td>
            <template v-if="projects.length">
            <span v-for="(project, index) in projects" :key="project.id">
              <router-link :to="`/project/${project.id}`">{{project.name}}</router-link>
              <span v-if="index < projects.length - 1">, </span>
            </span>
            </template>
            <em class="has-text-grey" v-else-if="nbProjects > 0">
              {{$t('used-in-project-no-access')}}
            </em>
            <em class="has-text-grey" v-else>
              {{$t('not-used-in-any-project')}}
            </em>
          </td>
        </tr>
        <tr>
          <td><strong>{{$t('creator')}}</strong></td>
          <td>
            {{creator.fullName || $t('unknown')}}
          </td>
        </tr>
        <tr v-if="canEdit">
          <td><strong>{{$t('actions')}}</strong></td>
          <td>
            <div class="buttons">
              <button class="button" @click="isRenameModalActive = true">
                {{$t('button-rename')}}
              </button>
              <button
                  class="button is-danger"
                  @click="confirmDeletion()"
                  :disabled="nbProjects > 0"
                  :title="nbProjects ? $t('cannot-delete-ontology-with-projects') : ''"
              >
                {{$t('button-delete')}}
              </button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>

      <rename-modal
          :title="$t('rename-ontology')"
          :currentName="ontology.name"
          :active.sync="isRenameModalActive"
          @rename="rename"
      />

    </div>
  </div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import {Ontology, User, ProjectCollection} from 'cytomine-client';
import OntologyTree from './OntologyTree';
import RenameModal from '@/components/utils/RenameModal';

export default {
  name: 'ontology-details',
  props: {
    ontology: Object
  },
  components: {
    OntologyTree,
    RenameModal
  },
  data() {
    return {
      loading: true,
      error: false,

      fullOntology: {},
      projects: [],
      managedProjects: [],
      creator: null,

      isRenameModalActive: false
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    currentAccount: get('currentUser/account'),
    canEdit() {
      return !this.currentUser.guestByNow && (this.currentUser.adminByNow || this.currentUser.id === this.fullOntology.user ||
        (this.hasAccessToAllProjects && (this.allProjectsAreCollaborative || this.manageAllProjects)));
    },
    nbProjects() {
      return this.fullOntology.projects.length;
    },
    hasAccessToAllProjects() {
      return this.nbProjects === this.projects.length;
    },
    allProjectsAreCollaborative() {
      return !this.projects.some(p => p.isRestricted || p.isReadOnly);
    },
    manageAllProjects() {
      return !this.projects.some(p => !this.managedProjects.find(mp => mp.id === p.id));
    }
  },
  watch: {
    ontology() {
      this.fetchOntology();
    },
  },
  methods: {
    async fetchOntology() {
      this.loading = true;
      this.error = false;

      try {
        this.fullOntology = await Ontology.fetch(this.ontology.id);

        // projects prop of ontology contains all projects, including those that the current user cannot see => need to refetch the collection
        this.projects = (await ProjectCollection.fetchAll({filterKey: 'ontology', filterValue: this.ontology.id})).array;
        // ---
      } catch (error) {
        console.log(error);
        this.error = true;
      }

      this.fetchManagedProjects();

      try {
        this.creator = await User.fetch(this.fullOntology.user);
      } catch (error) {
        console.log(error);
      }

      this.loading = false;
    },
    async fetchManagedProjects() {
      this.managedProjects = (await ProjectCollection.fetchAll({
        filterKey: 'user',
        filterValue: this.currentUser.id,
        light: true,
        admin: true
      })).array;
    },

    async rename(newName) {
      let oldName = this.ontology.name;
      try {
        this.fullOntology.name = newName;
        await this.fullOntology.save();
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-ontology-rename', {name: this.fullOntology.name})
        });
        this.$emit('rename', newName);
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-ontology-rename', {name: oldName})
        });
      }
      this.isRenameModalActive = false;
    },

    confirmDeletion() {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-ontology', {name: this.ontology.name}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.$emit('delete')
      });
    }
  },
  created() {
    this.fetchOntology();
  }
};
</script>

<style>
.ontology-details-wrapper .ontology-tree {
  max-width: 40em;
}

.ontology-details-wrapper td:first-child {
  width: 8em;
}
</style>

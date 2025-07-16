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
<div class="list-members-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
    <h2> {{ $t('error') }} </h2>
    <p> {{ $t('unexpected-error-info-message') }} </p>
  </b-message>
  <template v-else-if="!loading">
    <div class="columns">
      <div class="column is-one-quarter">
        <b-input v-model="searchString" :placeholder="$t('search-placeholder')" type="search" icon="search" />
      </div>

      <div class="column filter is-one-quarter">
        <div class="filter-label">
          {{$t('role')}}
        </div>
        <div class="filter-body">
          <cytomine-multiselect v-model="selectedRoles" :options="availableRoles" multiple
            :searchable="false" label="label" track-by="value"/>
        </div>
      </div>
      <div class="column is-one-half has-text-right-desktop buttons">
        <button class="button is-link add-member" @click="addMemberModal = true">
          {{$t('button-add-members')}}
        </button>

        <button class="button is-danger" @click="confirmMembersRemoval()" :disabled="selectedMembers.length === 0">
          {{$t('button-remove-selected-members')}}
        </button>
      </div>
    </div>

    <cytomine-table
      :collection="MemberCollection"
      :is-empty="this.selectedRoles.length === 0"
      :currentPage.sync="currentPage"
      :perPage.sync="perPage"
      :sort.sync="sortField"
      :order.sync="sortOrder"
      :detailed=false
      :checkable=true
      :isRowCheckable="(row) => row.id !== currentUser.id"
      :checkedRows.sync="selectedMembers"
      :revision="revision"
    >

      <template #default="{row: member}">
        <b-table-column field="username" :label="$t('username')" sortable width="100">
          {{member.username}}
        </b-table-column>

        <b-table-column field="fullName" :label="$t('name')" sortable width="150">
          {{member.name}}
        </b-table-column>

        <b-table-column field="projectRole" :label="$t('role')" sortable width="50">
          <icon-project-member-role
            :is-manager="member.role !== contributorRole.value"
            :is-representative="member.role === representativeRole.value"
            editable
            @toggleManager="confirmToggleManager(member)"
            @toggleRepresentative="toggleRepresentative(member)"
          />
        </b-table-column>

        <b-table-column label="" centered width="50">
          <router-link :to="`/project/${project.id}/activity/user/${member.id}`" class="button is-small is-link">
            {{$t('button-view-activity')}}
          </router-link>
        </b-table-column>
      </template>

      <template #empty>
        <div class="content has-text-grey has-text-centered">
          <p>{{$t('no-member-fitting-criteria')}}</p>
        </div>
      </template>

      <template #footer>
        <div class="has-text-centered">
          <a @click="download(exportURL)" class="button is-link">{{$t('button-export-as-csv')}}</a>
        </div>
      </template>
    </cytomine-table>

    <div class="legend">
      <h2>{{$t('legend')}}</h2>
      <p><icon-project-member-role /> : {{$t('project-contributor')}}</p>
      <p><icon-project-member-role :is-manager="true" /> : {{$t('project-manager')}}</p>
      <p><icon-project-member-role :is-manager="true" :is-representative="true" /> : {{$t('project-representative')}}</p>
    </div>

    <add-member-modal :active.sync="addMemberModal" @addMembers="refreshMembers()" />
  </template>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import CytomineTable from '@/components/utils/CytomineTable';
import CytomineMultiselect from '@/components/form/CytomineMultiselect';
import AddMemberModal from './AddMemberModal';
import {Cytomine, UserCollection, ProjectRepresentative} from 'cytomine-client';
import IconProjectMemberRole from '@/components/icons/IconProjectMemberRole';
import {appendShortTermToken} from '@/utils/token-utils.js';

export default {
  name: 'projet-members',
  components: {
    IconProjectMemberRole,
    CytomineTable,
    CytomineMultiselect,
    AddMemberModal
  },
  data() {
    return {
      loading: true,
      error: false,

      addMemberModal: false,

      currentPage: 1,
      perPage: 25,
      sortField: '',
      sortOrder: '',

      searchString: '',

      contributorRole: {label:this.$t('contributor'), value : 'contributor'},
      managerRole: {label:this.$t('manager'), value: 'manager'},
      representativeRole: {label:this.$t('representative'), value: 'representative'},
      availableRoles: [],
      selectedRoles: [],
      selectedMembers: [],


      revision: 0
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    project: get('currentProject/project'),
    shortTermToken: get('currentUser/shortTermToken'),

    MemberCollection() {
      let collection = new UserCollection({
        filterKey: 'project',
        filterValue: this.project.id,
      });
      if (this.selectedRoles.length > 0) {
        collection['projectRole'] = {
          in: this.selectedRoles.map(option => option.value).join()
        };
      }
      if (this.searchString) {
        collection['fullName'] = {
          ilike: encodeURIComponent(this.searchString)
        };
      }

      return collection;
    },

    exportURL() {
      // TODO in core: should export only the filtered users
      return Cytomine.instance.host + Cytomine.instance.basePath + `project/${this.project.id}/user/download?format=csv`;
    },
  },

  methods: {
    appendShortTermToken,
    async refreshMembers() {
      try {
        this.revision++;
        await this.$store.dispatch('currentProject/fetchProjectMembers');
      } catch (error) {
        console.log(error);
        this.error = true;
      }
    },
    download(url) {
      window.open(appendShortTermToken(url, this.shortTermToken));
    },
    confirmMembersRemoval() {
      this.$buefy.dialog.confirm({
        title: this.$t('remove-members'),
        message: this.$tc('remove-members-confirmation-message', this.selectedMembers.length, {
          count: this.selectedMembers.length,
          username: this.selectedMembers[0].fullName
        }),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.removeSelectedMembers()
      });
    },
    async removeSelectedMembers() {
      try {
        await this.project.deleteUsers(this.selectedMembers.map(member => member.id));
        await this.refreshMembers();
        this.$notify({type: 'success', text: this.$t('notif-success-remove-project-members')});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-remove-project-members')});
      }
    },

    confirmToggleManager(member) {
      if (member.id === this.currentUser.id && member.role !== this.contributorRole.value) {
        this.$buefy.dialog.confirm({
          title: this.$t('remove-yourself-from-manager'),
          message: this.$tc('remove-yourself-from-manager-confirmation-message'),
          type: 'is-danger',
          confirmText: this.$t('button-confirm'),
          cancelText: this.$t('button-cancel'),
          onConfirm: () => this.toggleManager(member)
        });
      } else {
        this.toggleManager(member);
      }
    },
    async toggleManager(member) {
      try {
        if (member.role !== this.contributorRole.value) {
          if (member.role === this.representativeRole.value) {
            await ProjectRepresentative.delete(0, this.project.id, member.id);
          }
          await this.project.deleteAdmin(member.id);
        } else {
          await this.project.addAdmin(member.id);
        }
        this.revision++;

        if (member.id === this.currentUser.id) {
          await this.$store.dispatch('currentProject/fetchUIConfig');
          if (!this.$store.state.currentProject.configUI['project-configuration-tab']) {
            this.$router.push(`/project/${this.project.id}`);
          }
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-change-role', {username: member.fullName})});
      }
    },
    async toggleRepresentative(member) {
      try {
        if (member.role === this.representativeRole.value) {
          if ((await this.project.fetchRepresentatives()).array.length < 2) {
            this.$notify({type: 'error', text: this.$t('notif-error-not-enough-representative')});
          } else {
            await ProjectRepresentative.delete(0, this.project.id, member.id);
          }
        } else {
          await new ProjectRepresentative({user: member.id, project: this.project.id}).save();
        }
        this.revision++;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-change-role', {username: member.fullName})});
      }
    },
  },
  mounted() {
    appendShortTermToken();
  },
  async created() {
    this.availableRoles = [this.contributorRole, this.managerRole, this.representativeRole];
    this.selectedRoles = this.availableRoles;
    this.revision++;
    this.loading = false;
  }
};
</script>

<style scoped>
.filter {
  display: flex;
  align-items: center;
}

.filter-label {
  margin-right: 1em;
  margin-bottom: 0 !important;
}

.filter-body {
  flex-grow: 1;
}

.list-members-wrapper {
  min-height: 40vh;
}

.tag {
  font-size: 0.75em !important;
  font-weight: 600;
}

.tag.ldap {
  background-color: #A0C0E6;
}

.legend {
  margin-top: 0.8rem;
  margin-bottom: 1rem;
  border-radius: 10px;
  padding: 1rem 1.5em;
  background: #f8f8f8;
}

.legend p {
  padding-bottom: 0.4em;
}

.legend .icons {
  display: inline-block;
  width: 40px;
  margin-right: 0.7em;
}

a:hover .fas {
  color: rgb(35, 102, 216);
}

a:hover .fas.disabled, a:hover .disabled .fas {
  color: rgba(35, 102, 216, 0.5);
}

.fas {
  color: black;
  width: 20px;
  position: relative;
}

.fas.fa-flag {
  font-size: 0.8em;
  bottom: 0.5em;
}

.fas.disabled, .disabled .fas {
  color: rgba(0, 0, 0, 0.1);
}
</style>

<style>
.table-members {
  margin-top: 1.2em;
}
</style>

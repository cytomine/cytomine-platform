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
  <div class="box" v-if="!loading">
    <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
      <h2> {{ $t('error') }} </h2>
      <p> {{ $t('unexpected-error-info-message') }} </p>
    </b-message>
    <template v-else>
      <div class="columns">
        <h2 class="column is-marginless">{{$t('members-activity')}}</h2>
        <p class="column has-text-right is-size-7 has-text-grey">
          {{$t('data-last-updated-on', {time: $options.filters.moment(lastUpdate, 'LTS')})}}
        </p>
      </div>

      <div class="columns">
        <div class="column is-one-quarter">
          <b-input v-model="searchString" :placeholder="$t('search-placeholder')" type="search" icon="search" />
        </div>

        <div class="column">
          <button class="button" @click="filtersOpened = !filtersOpened">
            <span class="icon">
              <i class="fas fa-filter"></i>
            </span>
            <span>{{filtersOpened ? $t('button-hide-filters') : $t('button-show-filters')}}</span>
          </button>
        </div>
      </div>

      <b-collapse :open="filtersOpened">
        <div class="filters columns">
          <div class="column filter">
            <div class="filter-label">
              {{$t('status')}}
            </div>
            <div class="filter-body">
              <cytomine-multiselect v-model="selectedStatus" :options="availableStatus" multiple
                :searchable="false" />
            </div>
          </div>

          <div class="column filter">
            <div class="filter-label">
              {{$t('role')}}
            </div>
            <div class="filter-body">
              <cytomine-multiselect v-model="selectedRoles" :options="availableRoles" multiple
                :searchable="false" />
            </div>
          </div>
        </div>
      </b-collapse>

      <b-table
        :data="filteredMembers"
        default-sort="username"
        :paginated="true"
        :per-page="perPage"
        pagination-size="is-small"
      >
        <template #default="{row: member}">
          <b-table-column field="username" :label="$t('username')" sortable width="100">
            <username :user="member" :online="onlineIds ? onlineIds.includes(member.id) : null" :displayFullName="false" />
          </b-table-column>

          <b-table-column field="name" :label="$t('name')" sortable width="150">
            {{ member.name }}
          </b-table-column>

          <b-table-column field="role" :label="$t('role')" centered sortable width="20">
            <icon-project-member-role :is-manager="member.role === managerRole" />
          </b-table-column>

          <b-table-column field="lastImageName" :label="$t('last-image')" sortable width="100">
            <template v-if="member.lastImageId">
              <router-link :to="`/project/${project.id}/image/${member.lastImageId}`">
                {{member.lastImageName}}
              </router-link>
            </template>
            <em v-else class="has-text-grey">{{$t('no-record')}}</em>
          </b-table-column>

          <b-table-column field="lastConnection" :label="$t('last-connection')" sortable width="100">
            <template v-if="member.lastConnection">
              {{Number(member.lastConnection) | moment('ll LT')}}
            </template>
            <em v-else class="has-text-grey">{{$t('no-record')}}</em>
          </b-table-column>

          <b-table-column field="frequency" :label="$t('number-connections')" sortable width="100">
            {{member.frequency}}
          </b-table-column>

          <b-table-column label="" centered width="50">
            <router-link :to="`/project/${project.id}/activity/user/${member.id}`" class="button is-small is-link">
              {{$t('button-details')}}
            </router-link>
          </b-table-column>
        </template>

        <template #empty>
          <div class="content has-text-grey has-text-centered">
            <p>{{$t('no-member-fitting-criteria')}}</p>
          </div>
        </template>

        <template #footer v-if="isManager">
          <p class="has-text-centered">
            <router-link class="button is-link add-member" :to="`/project/${project.id}/configuration?tab=members`">
              {{$t('button-manage-members')}}
            </router-link>
          </p>
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

      <div class="legend">
        <h2>{{$t('legend')}}</h2>
        <p><icon-project-member-role /> : {{$t('project-contributor')}}</p>
        <p><icon-project-member-role :is-manager="true" /> : {{$t('project-manager')}}</p>
      </div>
    </template>
  </div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import CytomineMultiselect from '@/components/form/CytomineMultiselect';
import Username from '@/components/user/Username';

import constants from '@/utils/constants.js';
import {getWildcardRegexp} from '@/utils/string-utils';
import IconProjectMemberRole from '@/components/icons/IconProjectMemberRole';

export default {
  name: 'members-activity',
  components: {
    IconProjectMemberRole,
    CytomineMultiselect,
    Username
  },
  data() {
    return {
      loading: true,
      error: false,

      searchString: '',
      filtersOpened: false,
      perPage: 25,
      members: [],
      onlineIds: null,

      contributorRole: this.$t('contributor'),
      managerRole: this.$t('manager'),
      availableRoles: [],
      selectedRoles: [],

      onlineStatus: this.$t('online'),
      offlineStatus: this.$t('offline'),
      availableStatus: [],
      selectedStatus: [],

      lastUpdate: null,
      timeout: null
    };
  },
  computed: {
    project: get('currentProject/project'),
    isManager() {
      return this.$store.getters['currentProject/canManageProject'];
    },
    idManagers() {
      return this.$store.state.currentProject.managers.map(manager => manager.id);
    },
    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    filteredMembers() {
      let statusKnown = Boolean(this.onlineIds); // whether or not the info regarding online users could be fetched
      let includeOnline = this.selectedStatus.includes(this.onlineStatus);
      let includeOffline = this.selectedStatus.includes(this.offlineStatus);
      return this.members.filter(member => {
        let online = statusKnown ? this.onlineIds.includes(member.id) : null;
        return (this.regexp.test(member.name) || this.regexp.test(member.username))
                    && (!statusKnown || (includeOnline && online) || (includeOffline && !online))
                    && this.selectedRoles.includes(member.role);
      });
    }
  },
  methods: {
    async fetchData() {
      try {
        await Promise.all([
          this.fetchMembers(),
          this.fetchOnlines()
        ]);
      } catch (error) {
        console.log(error);
        this.error = true;
        return;
      }

      this.lastUpdate = new Date();
      clearTimeout(this.timeout);
      this.timeout = setTimeout(this.fetchData, constants.MEMBERS_ACTIVITY_REFRESH_INTERVAL);
    },
    async fetchMembers() {
      let members = (await this.project.fetchUsersActivity()).array;
      members.forEach(member => {
        member.role = this.idManagers.includes(member.id) ? this.managerRole : this.contributorRole;
      });
      this.members = members;
    },
    async fetchOnlines() {
      try {
        let onlines = await this.project.fetchConnectedUsers();
        this.onlineIds = onlines.map(o => o.id);
      } catch (error) {
        console.log(error);
        this.onlineIds = null;
      }
    }
  },
  async created() {
    this.availableRoles = [this.contributorRole, this.managerRole];
    this.selectedRoles = this.availableRoles;

    this.availableStatus = [this.onlineStatus, this.offlineStatus];
    this.selectedStatus = this.availableStatus;
  },
  async activated() {
    await this.fetchData();
    this.loading = false;
  },
  deactivated() {
    clearTimeout(this.timeout);
  }
};
</script>

<style scoped>
.legend {
  margin-top: 0.8rem;
  margin-bottom: 1rem;
  border-radius: 10px;
  padding: 1.4em;
  background: #f8f8f8;
}

.fas.fa-user-cog {
  width: 20px;
}
</style>

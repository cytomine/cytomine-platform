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
  <template v-if="!loading">
    <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
      <h2> {{ $t('error') }} </h2>
      <p> {{ $t('unexpected-error-info-message') }} </p>
    </b-message>
    <template v-else>
      <div class="columns">
        <div class="column is-one-quarter">
          <b-input v-model="searchString" :placeholder="$t('search-placeholder')" type="search" icon="search" />
        </div>

        <div class="column is-one-half has-text-right-desktop">
          <button class="button is-link" @click="startUserCreation()">
            {{$t('button-new-user')}}
          </button>
        </div>
      </div>

      <cytomine-table
        :collection="userCollection"
        :currentPage.sync="currentPage"
        :perPage.sync="perPage"
        :sort.sync="sortField"
        :order.sync="sortOrder"
        :detailed=true
        :revision="revision"
      >
        <template #default="{row: user}">

          <b-table-column field="username" :label="$t('username')" sortable width="100">
            {{user.username}}
          </b-table-column>

          <b-table-column field="fullName" :label="$t('name')" sortable width="150">
            {{user.fullName}}
          </b-table-column>

          <b-table-column field="role" :label="$t('role')" sortable width="50">
            <span class="tag" :class="getRoleData(user).class">{{$t(getRoleData(user).label)}}</span>
          </b-table-column>

          <b-table-column field="email" :label="$t('email')" sortable width="150">
            <a :href="`mailto:${user.email}`">{{ user.email }}</a>
          </b-table-column>

          <b-table-column field="origin" :label="$t('source')" centered sortable width="50">
            <span :class="['tag', user.LDAP ? 'is-link' : 'is-grey']">
              {{displayMemberOrigin(user)}}
            </span>
          </b-table-column>

          <b-table-column field="created" :label="$t('created')" sortable width="150">
            {{Number(user.created) | moment('ll LT')}}
          </b-table-column>

          <b-table-column field="updated" :label="$t('updated')" sortable width="150">
            <template v-if="user.updated">{{Number(user.updated) | moment('ll LT')}}</template>
            <template v-else>-</template>
          </b-table-column>

          <b-table-column label="" width="100">
            <div class="buttons">
              <button class="button is-link is-small" @click="startUserEdition(user)">
                {{$t('button-edit')}}
              </button>
              <button v-if="user.enabled" class="button is-danger is-small" @click="lock(user)">
                {{$t('button-lock')}}
              </button>
              <button v-else class="button is-success is-small" @click="unlock(user)">
                {{$t('button-unlock')}}
              </button>
            </div>
          </b-table-column>
        </template>

        <template #detail="{row: user}">
          <user-details :user="user" />
        </template>

        <template #empty>
          <div class="content has-text-grey has-text-centered">
            <p>{{$t('no-user-fitting-criteria')}}</p>
          </div>
        </template>
      </cytomine-table>

      <user-modal :active.sync="modal" :user="editedUser" @addUser="refreshUsers" @updateUser="updateUser" />
    </template>
  </template>
</div>
</template>

<script>

import CytomineTable from '@/components/utils/CytomineTable';
import {UserCollection} from 'cytomine-client';
import UserModal from './UserModal';
import UserDetails from './UserDetails';
import {rolesMapping} from '@/utils/role-utils';

// TODO IAM
export default {
  name: 'admin-users',
  components: {
    CytomineTable,
    UserModal,
    UserDetails
  },
  data() {
    return {
      loading: true,
      error: false,
      addUserModal: false,
      searchString: '',
      currentPage: 1,
      perPage: 25,
      sortField: 'username',
      sortOrder: '',
      revision: 0,
      modal: false,
      editedUser: null
    };
  },
  computed: {
    roles() {
      return rolesMapping;
    },
    userCollection() {
      let collection = new UserCollection({
        withRoles: true
      });
      if (this.searchString) {
        collection['fullName'] = {
          ilike: encodeURIComponent(this.searchString)
        };
      }

      return collection;
    }
  },
  methods: {
    displayMemberOrigin(member) {
      let key;
      if (member.origin === 'LDAP') {
        key = 'LDAP';
      }
      if (member.origin === 'BOOTSTRAP') {
        key = 'system';
      } else {
        key = 'manual';
      }

      return this.$t(key);
    },
    getRoleData(user) {
      return this.roles[user.role];
    },
    // TODO IAM
    async lock(user) {
      try {
        await user.lock();
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-user-lock')});
      }
    },
    async unlock(user) {
      try {
        await user.unlock();
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-user-unlock')});
      }
    },
    startUserCreation() {
      this.editedUser = null;
      this.modal = true;
    },
    refreshUsers() {
      this.revision++;
    },
    startUserEdition(user) {
      this.editedUser = user;
      this.modal = true;
    },
    updateUser(user) {
      this.revision++;
      this.editedUser.populate(user);
    }
  },
  async created() {
    this.revision++;
    this.loading = false;
  }
};
</script>

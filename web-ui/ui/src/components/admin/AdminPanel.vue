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
<div class="box error" v-if="!currentUser.adminByNow">
  <h2> {{ $t('access-denied') }} </h2>
  <p>{{ $t('insufficient-permission') }}</p>
</div>
<div class="content-wrapper" v-else>
  <b-field class="radio-buttons-fullwidth">
    <b-radio-button v-model="activeTab" native-value="dashboard" type="is-link">
      {{$t('dashboard')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="users" type="is-link">
      {{$t('users')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="tags" type="is-link">
      {{$t('tags')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="configuration" type="is-link">
      {{$t('configuration')}}
    </b-radio-button>
  </b-field>

  <div class="box">
    <keep-alive>
      <component :is="activeComponent" />
    </keep-alive>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import AdminDashboard from './AdminDashboard';
import AdminUsers from './AdminUsers';
import AdminConfiguration from './AdminConfiguration';
import AdminTags from './AdminTags';
const defaultTab = 'dashboard';

export default {
  name: 'admin-panel',
  data() {
    return {
      activeTab: 0,
      tabNames: [
        'dashboard',
        'users',
        'tags',
        'configuration'
      ]
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    queriedTab() {
      return this.$route.query.tab;
    },
    // eslint-disable-next-line vue/return-in-computed-property
    activeComponent() {
      switch (this.activeTab) {
        case 'dashboard':
          return AdminDashboard;
        case 'users':
          return AdminUsers;
        case 'configuration':
          return AdminConfiguration;
        case 'tags':
          return AdminTags;
      }
      throw new Error('Cannot load active tabs ' + this.activeTab);
    }
  },
  watch: {
    queriedTab() {
      this.changeTab();
    },
    activeTab() {
      if (this.activeTab !== defaultTab || this.queriedTab) {
        this.$router.push(`?tab=${this.activeTab}`);
      }
    }
  },
  methods: {
    changeTab() {
      this.activeTab = this.queriedTab || defaultTab;
      if (!this.activeComponent) {
        this.activeTab = defaultTab;
      }
    }
  },
  created() {
    this.changeTab();
  }
};
</script>

<style scoped>
.box {
  position: relative;
  min-height: 20em;
}
</style>

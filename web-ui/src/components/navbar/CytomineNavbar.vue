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
<nav class="navbar is-light" role="navigation">
  <div class="navbar-brand">
    <router-link to="/" exact class="navbar-item">
      <img src="@/assets/logo.svg" id="logo" alt="Cytomine">
    </router-link>
    <a role="" class="navbar-burger" :class="{'is-active':openedTopMenu}" @click="openedTopMenu=!openedTopMenu">
      <span></span> <span></span> <span></span>
    </a>
  </div>
  <div id="topMenu" class="navbar-menu" :class="{'is-active':openedTopMenu}">
    <div class="navbar-start">
      <navbar-dropdown
      icon="fa-folder-open"
      v-if="this.nbActiveProjects > 0"
      :title="$t('workspace')"
      :listPathes="['/project/']">
        <navigation-tree />
      </navbar-dropdown>
      <router-link to="/projects" class="navbar-item">
        <i class="fas fa-list-alt"></i>
        {{ $t('projects') }}
      </router-link>
      <router-link v-if="!currentUser.guestByNow" to="/storage" class="navbar-item">
        <i class="fas fa-download"></i>
        {{ $t('storage') }}
      </router-link>
      <router-link to="/ontology" class="navbar-item">
        <i class="fas fa-hashtag"></i>
        {{ $t('ontologies') }}
      </router-link>
      <router-link v-if="appEngineEnabled" to="/appengine" class="navbar-item">
        <i class="fas fa-code"></i>
        {{ $t('app-engine.applications') }}
      </router-link>
      <router-link v-if="currentUser.adminByNow" to="/admin" class="navbar-item">
        <i class="fas fa-wrench"></i>
        {{ $t('admin-menu') }}
      </router-link>
    </div>

    <div class="navbar-end">
      <cytomine-searcher />
      <!-- TODO IAM -->
      <navbar-dropdown
        :icon="currentUser.adminByNow ? 'fa-star' : 'fa-user'"
        :title="currentUser.fullName"
        :tag="currentUser.adminByNow ? {type: 'is-danger', text: $t('admin')} : null"
        :listPathes="['/account', '/activity']"
      >
        <router-link to="/account" class="navbar-item">
          <span class="icon"><i class="fas fa-user fa-xs"></i></span> {{$t('account')}}
        </router-link>
        <router-link to="/activity" class="navbar-item">
          <span class="icon"><i class="fas fa-history fa-xs"></i></span> {{$t('activity-history')}}
        </router-link>
        <template v-if="currentUser.admin">
          <a v-if="!currentUser.adminByNow" class="navbar-item" @click="openAdminSession()">
            <span class="icon"><i class="fas fa-star fa-xs"></i></span> {{$t('open-admin-session')}}
          </a>
          <a v-else class="navbar-item" @click="closeAdminSession()">
            <span class="icon"><i class="far fa-star fa-xs"></i></span> {{$t('close-admin-session')}}
          </a>
        </template>
        <a class="navbar-item" @click="logout()">
          <span class="icon"><i class="fas fa-power-off fa-xs"></i></span> {{ $t('logout') }}
        </a>
      </navbar-dropdown>

      <navbar-dropdown icon="fa-question-circle" :title="$t('help')" :classes="['is-right']">
        <a class="navbar-item" @click="openHotkeysModal()">
          <span class="icon"><i class="far fa-keyboard fa-xs"></i></span> {{$t('shortcuts')}}
        </a>
        <a class="navbar-item" @click="openAboutModal()">
          <span class="icon"><i class="fas fa-info-circle fa-xs"></i></span> {{$t('about-cytomine')}}
        </a>
      </navbar-dropdown>
    </div>
  </div>
  <div class="hidden" v-shortkey.once="openHotkeysModalShortcut" @shortkey="openHotkeysModal"></div>
</nav>
</template>

<script>
import {get} from '@/utils/store-helpers';
import {changeLanguageMixin} from '@/lang.js';

import NavbarDropdown from './NavbarDropdown';
import NavigationTree from './NavigationTree';
import HotkeysModal from './HotkeysModal';
import AboutCytomineModal from './AboutCytomineModal';
import CytomineSearcher from '@/components/search/CytomineSearcher';
import constants from '@/utils/constants.js';
import shortcuts from '@/utils/shortcuts.js';

export default {
  name: 'cytomine-navbar',
  components: {
    NavbarDropdown,
    NavigationTree,
    CytomineSearcher
  },
  mixins: [changeLanguageMixin],
  data() {
    return {
      openedTopMenu: false,
      hotkeysModal: null,
      appEngineEnabled: constants.APPENGINE_ENABLED,
      aboutModal: null
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    nbActiveProjects() {
      return Object.keys(this.$store.state.projects).length;
    },
    openHotkeysModalShortcut() {
      return shortcuts['general-shortcuts-modal'];
    }
  },
  watch: {
    $route() {
      this.openedTopMenu = false;
    }
  },
  methods: {
    // required to use programmatic modal for correct display in IE11
    openHotkeysModal() {
      if (!this.hotkeysModal) {
        this.hotkeysModal = this.$buefy.modal.open({
          parent: this,
          component: HotkeysModal,
          hasModalCard: true,
          onCancel: () => this.hotkeysModal = null,
        });
      }
    },
    openAboutModal() {
      this.$buefy.modal.open({
        parent: this,
        component: AboutCytomineModal,
        hasModalCard: true
      });
    },
    // ---

    async openAdminSession() {
      try {
        await this.$store.dispatch('currentUser/openAdminSession');
        this.$router.push('/admin');
      } catch (error) {
        console.log(error);
      }
    },
    async closeAdminSession() {
      try {
        await this.$store.dispatch('currentUser/closeAdminSession');
        if (this.$router.currentRoute.path === '/') {
          this.$router.push('/projects');
        } else {
          this.$router.push('/');
        }
      } catch (error) {
        console.log(error);
      }
    },

    async logout() {
      try {
        this.$store.dispatch('logout');
        this.changeLanguage();
        await this.$keycloak.logout();
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-logout')});
      }
    }
  }
};
</script>

<style lang="scss">
#logo {
  height: 2rem;
  font-family: "cytomine";
  font-size: 2em;
  font-weight: lighter;
  line-height: 1em;
}

/* Special styling for IE */
@media screen and (-ms-high-contrast: active), (-ms-high-contrast: none) {
  #logo {
    height: 40px !important;
    max-height: none !important;
  }
}

.navbar {
  font-weight: 600;
  z-index: 500 !important;

  .fas, .far {
    padding-right: 0.5rem;
  }
}
</style>

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
<div id="app" class="wrapper">
  <notifications position="top center" width="30%" :max="5">
    <template #body="props">
      <div class="notification vue-notification" :class="props.item.type">
        <button class="delete" @click="props.close"></button>
        <strong class="notification-title">
          {{props.item.title}}
        </strong>
        <div class="notification-content" v-html="props.item.text"></div>
      </div>
    </template>
  </notifications>

  <template v-if="!loading">
    <div class="box error" v-if="communicationError">
      <h2>
        {{$t('communication-error')}}
      </h2>
      {{$t('core-cannot-be-reached')}}
    </div>

    <template v-else-if="currentUser">
      <cytomine-navbar />
      <div class="bottom">
        <keep-alive include="cytomine-storage">
          <router-view v-if="currentUser" />
        </keep-alive>
      </div>
    </template>
  </template>
</div>
</template>

<script>
import axios from 'axios';
import {get} from '@/utils/store-helpers';
import {changeLanguageMixin} from '@/lang.js';

import CytomineNavbar from './components/navbar/CytomineNavbar.vue';

import {Cytomine} from 'cytomine-client';

import constants from '@/utils/constants.js';
import ifvisible from 'ifvisible';
import {updateToken} from '@/utils/token-utils';
ifvisible.setIdleDuration(constants.IDLE_DURATION);

export default {
  name: 'app',
  components: {
    CytomineNavbar,
  },
  mixins: [
    changeLanguageMixin,
  ],
  data() {
    return {
      communicationError: false,
      loading: true,
      timeout: null,
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    currentAccount: get('currentUser/account'),
    project: get('currentProject/project')
  },
  watch: {
    $route() {
      // Invoke refresh token if needed when route changes.
      updateToken();
    },
  },
  methods: {
    wakeup: async function () {
      if (!ifvisible.now()) {
        return;
      }
      await updateToken();
      await this.ping();
    },
    async ping() {
      if (!ifvisible.now()) {
        return; // window not visible or inactive user => stop pinging
      }
      try {
        // TODO IAM - still needed ?
        // await Cytomine.instance.ping(this.project ? this.project.id : null);
        if (!this.currentUser) {
          await this.fetchUser();
        }
        this.communicationError = false;
      } catch (error) {
        console.log(error);
        this.communicationError = error.toString().indexOf('401') === -1;
      }

      clearTimeout(this.timeout);
      this.timeout = setTimeout(this.ping, constants.PING_INTERVAL);
    },
    async fetchUser() {
      await this.$store.dispatch('currentUser/fetchUser');
      if (this.currentAccount) {
        this.changeLanguage(this.currentAccount.locale);
      }
    }
  },
  async created() {
    let settings;
    await axios
      .get('configuration.json')
      .then(response => (settings = response.data));

    for (let i in settings) {
      if (Object.prototype.hasOwnProperty.call(constants, i)
        || i.includes('_NAMESPACE') || i.includes('_VERSION') || i.includes('_ENABLED')) {
        constants[i] = settings[i];
      }
    }
    Object.freeze(constants);

    const authorizationHeaderInterceptor = async config => {
      const token = await updateToken();

      config.headers = config.headers || {};

      if (token !== null) {
        this.$store.commit('currentUser/setShortTermToken', token);
        config.headers.common['Authorization'] = `Bearer ${token}`;
      }
      return config;
    };
    new Cytomine(
      window.location.origin,
      '/api/', `/iam/realms/${this.$keycloak.realm}`,
      authorizationHeaderInterceptor
    );

    await this.ping();
    this.loading = false;
    ifvisible.on('wakeup', this.wakeup);
  }
};
</script>

<style lang="scss">
@import '@/assets/styles/main.scss';

@font-face {
  font-family: 'cytomine';
  src: url('assets/cytomine-font.woff') format('woff');
}

html, body {
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

body {
  font-family: "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif, sans-serif;
  color: #333;
  background: #d4d4d4;
}

.wrapper {
  display: flex;
  height: 100%;
  width: 100%;
  flex-direction: column;
  background: #d4d4d4;
}

.box.error {
  max-width: 600px;
  margin: auto;
  margin-top: 3rem;
}

.notifications {
  margin-top: 1em;
}

.notification.info {
  background: #77b1ea;
}

.bottom {
  flex: 1;
  overflow-y: auto;
  /* position: relative; */
}

h1 {
  text-transform: uppercase;
  letter-spacing: 0.1rem;
  font-size: 1.25rem;
  text-align: center;
  padding: 0.9rem;
}

h2 {
  font-size: 1rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08rem;
  margin-bottom: 1rem;
}

.in-project {
  color: grey;
  font-size: 0.8em;
}

.input[readonly] {
  background-color: whitesmoke;
}

strong, .label {
  font-weight: 600 !important;
}

.content-wrapper {
  padding: 1.5% 2.5%;
  position: relative;
  min-height: 100%;
}

/* Filters */

.filters {
  background: #f8f8f8;
  margin-top: 1.2rem;
  border-radius: 10px;
  padding: 1rem;
}

.filter-label {
  text-transform: uppercase;
  font-size: 0.8em;
  margin-bottom: 0.5em;
  margin-left: 1em;
}

.filter-label .no-uppercase {
  text-transform: none;
}

.nb-active-filters {
  display: inline-block;
  background: $primary;
  color: $primary-invert;
  min-width: 1.25rem;
  height: 1.25rem;
  font-weight: 600;
  border-radius: 0.625rem;
  margin-left: 0.5em;
  font-size: 0.9em;
  line-height: 1.25em;
  padding: 0 0.25em;
  position: absolute;
  top: -0.3em;
  right: -0.6em;
}

/* For correct display of svg images on IE > 10 */
@media screen and (-ms-high-contrast: active), (-ms-high-contrast: none) {
  img[src$=".svg"] {
    width: 100%;
  }
}
</style>

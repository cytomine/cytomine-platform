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
<div class="content-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <template v-if="!loading">
  <div class="panel">
    <p class="panel-heading">
      <i class="fas fa-user" aria-hidden="true"></i>
      {{ $t('profile') }}
    </p>
    <div class="panel-block">
      <form @submit.prevent="editDetails()" data-vv-scope="profile">
        <b-field :label="$t('id')" horizontal v-if="currentAccount.isDeveloper">
          {{currentUser.id}}
        </b-field>

        <b-field :label="$t('username')" horizontal>
          <b-input :value="updatedAccount.username" disabled />
        </b-field>

        <b-field :label="$t('role')" horizontal>
          <span class="tag" :class="role.class">{{$t(role.label)}}</span>
        </b-field>

        <b-field
          :label="$t('lastname')"
          horizontal
          :type="{'is-danger': errors.has('profile.lastname')}"
          :message="errors.first('profile.lastname')"
        >
          <b-input v-model="updatedAccount.lastName" name="lastname" v-validate="'required'" />
        </b-field>

        <b-field
          :label="$t('firstname')"
          horizontal
          :type="{'is-danger': errors.has('profile.firstname')}"
          :message="errors.first('profile.firstname')"
        >
          <b-input v-model="updatedAccount.firstName" name="firstname" v-validate="'required'" />
        </b-field>

        <b-field
          :label="$t('email')"
          horizontal
          :type="{'is-danger': errors.has('profile.email')}"
          :message="errors.first('profile.email')"
        >
          <b-input v-model="updatedAccount.email" name="email" v-validate="'required|email'" />
        </b-field>

        <b-field :label="$t('language')" horizontal>
          <b-select v-model="updatedAccount.locale">
            <option v-for="{value, name} in languages" :key="value" :value="value">
              {{name}}
            </option>
          </b-select>
        </b-field>

        <b-field :label="$t('developer-mode')" horizontal>
          <b-switch v-model="updatedAccount.isDeveloper" class="switch">
            <template v-if="updatedAccount.isDeveloper">{{$t('yes')}}</template>
            <template v-else>{{$t('no')}}</template>
          </b-switch>
        </b-field>

        <b-field grouped position="is-right">
          <div class="control">
            <button class="button is-link" :disabled="errors.any('profile')"> {{$t('button-save')}}</button>
          </div>
        </b-field>
      </form>
    </div>
  </div>

  <div class="panel">
    <p class="panel-heading">
      <i class="fas fa-briefcase" aria-hidden="true"></i>
      {{ $t('password') }}
    </p>
    <div class="panel-block">
        <b-message v-if="credentialsError || this.passwordCredentials === null" type="is-danger" has-icon icon-size="is-small">
          {{$t('failed-to-retrieve-credentials')}}
        </b-message>
        <template v-else>
          <b-field
            :label="$t('password-current')"
            horizontal
          >
            <template v-if="currentPasswordInfo">
              {{ $t('last-updated-on') }} {{Number(currentPasswordInfo.createdDate) | moment('ll LT')}}.
            </template>
            <em v-else>{{ $t('no-password') }}</em>
          </b-field>
          <b-field grouped position="is-right" v-if="passwordCredentials.updateAction">
            <div class="control">
              <button class="button is-link" @click="updatePassword()">
                {{$t('button-change-password')}}
              </button>
            </div>
          </b-field>
        </template>
    </div>
  </div>

  <div class="panel">
    <p class="panel-heading">
      <i class="fas fa-exchange-alt" aria-hidden="true"></i>
      {{ $t('api-keys') }}
    </p>
    <div class="panel-block">
      <b-message v-if="apiKeysError" type="is-danger" has-icon icon-size="is-small">
        {{$t('failed-to-retrieve-api-keys')}}
      </b-message>
      <template v-else>
        <b-field :label="$t('public-key')" horizontal>
          <b-field>
            <b-input :value="apiKeys.primaryKey" readonly expanded />
            <p class="control">
              <button class="button" @click="copy(apiKeys.primaryKey)">
                <span class="icon"><i class="far fa-clipboard"></i></span>
                <span>{{$t('button-copy')}}</span>
              </button>
            </p>
          </b-field>
        </b-field>

        <b-field :label="$t('private-key')" horizontal>
          <b-field>
            <b-input :value="apiKeys.secondaryKey" readonly expanded />
            <p class="control">
              <button class="button" @click="copy(apiKeys.secondaryKey)">
                <span class="icon"><i class="far fa-clipboard"></i></span>
                <span>{{$t('button-copy')}}</span>
              </button>
            </p>
          </b-field>
        </b-field>

        <b-field grouped position="is-right">
          <div class="control">
            <button class="button is-link" @click="regenerateKeys()">{{$t('regenerate-api-keys')}}</button>
          </div>
        </b-field>
      </template>
    </div>
  </div>
  </template>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import {changeLanguageMixin} from '@/lang.js';
import {MyAccount, User} from 'cytomine-client';
import {rolesMapping} from '@/utils/role-utils';
import copyToClipboard from 'copy-to-clipboard';

export default {
  // eslint-disable-next-line
  name: 'Account',
  $_veeValidate: {validator: 'new'},
  mixins: [changeLanguageMixin],
  data() {
    return {
      updatedAccount: this.$store.state.currentUser.account.clone(),
      credentials: null,
      credentialsError: false,
      apiKeys: null,
      apiKeysError: null,
      loading: true,
      languages: [
        {value: 'en', name:'English'},
        {value: 'fr', name:'Français'},
        {value: 'es', name:'Español'},
        {value: 'nl', name:'Nederlands'},
        {value: 'no', name:'Norsk'},
      ],
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    currentAccount: get('currentUser/account'),
    role() {
      let key = this.$keycloak.hasResourceRole('GUEST') ? 'ROLE_GUEST' : this.$keycloak.hasResourceRole('ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER';
      return rolesMapping[key];
    },

    passwordCredentials() {
      return this.credentials.find(c => c.type === 'password');
    },
    currentPasswordInfo() {
      if (this.passwordCredentials
        && this.passwordCredentials.userCredentialMetadatas
        && this.passwordCredentials.userCredentialMetadatas.length === 1) {
        return this.passwordCredentials.userCredentialMetadatas[0].credential;
      }
      return null;
    }
  },
  watch: {
  },
  methods: {
    async editDetails() {
      let result = await this.$validator.validateAll('profile');
      if (!result) {
        return;
      }

      try {
        await this.$store.dispatch('currentUser/updateAccount', this.updatedAccount);
        this.changeLanguage(this.currentAccount.locale);
        this.$notify({type: 'success', text: this.$t('notif-success-user-details-saved')});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-user-details-not-saved')});
      }
    },

    updatePassword() {
      this.$keycloak.login({action: this.passwordCredentials.updateAction});
    },


    copy(value) {
      copyToClipboard(value);
      this.$notify({type: 'success', text: this.$t('notif-success-key-copied')});
    },

    async regenerateKeys() {
      try {
        this.apiKeys = await User.regenerateKeys();
        this.$notify({type: 'success', text: this.$t('notif-success-keys-regenerated')});
      } catch (err) {
        this.$notify({type: 'error', text: this.$t('notif-error-keys-not-regenerated')});
      }
    }
  },
  async created() {
    try {
      this.credentials = await MyAccount.fetchCredentials();
    } catch (error) {
      this.credentialsError = true;
    }
    try {
      this.apiKeys = await User.fetchCurrentUserKeys();
    } catch (error) {
      this.apiKeysError = true;
    }
    this.loading = false;
  }
};
</script>

<style scoped>
.panel {
  max-width: 80em;
  margin-left: auto;
  margin-right: auto;
}

.fas {
  margin-right: 0.3em;
}

input.hidden {
  display: none;
}
</style>

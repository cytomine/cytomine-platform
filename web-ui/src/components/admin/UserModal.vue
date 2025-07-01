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
<form @submit.prevent="save()">
  <cytomine-modal :active="active" :title="title" @close="$emit('update:active', false)">
    <!-- HACK: fake fields to prevent autofill -->
    <input id="username" class="hidden" type="text">
    <input id="password" class="hidden" type="password">

    <b-field
      :key="'username'"
      :label="$t('username')"
      horizontal
      :type="{'is-danger': errors.has('username')}"
      :message="errors.first('username')"
    >
      <b-input
        v-model="internalUser['username']"
        :name="'username'"
        v-validate="'required'"
        :type="'text'"
        :disabled="internalUser.id"
      />
    </b-field>

    <b-field
      v-for="{field, validationRules} in editableFields"
      :key="field"
      :label="$t(field === 'password' && editionMode ? 'password-new' : field)"
      horizontal
      :type="{'is-danger': errors.has(field)}"
      :message="errors.first(field)"
    >
      <b-input
        v-model="internalUser[field]"
        :name="field"
        v-validate="validationRules"
        :type="field === 'password' ? 'password': 'text'"
        :password-reveal="field === 'password'"
      />
    </b-field>

    <b-field :label="$t('role')" horizontal>
      <b-select v-model="selectedRole">
        <option v-for="(value, key) in roles" :value="key" :key="key">
          {{$t(value.label)}}
        </option>
      </b-select>
    </b-field>

    <b-field horizontal v-if="isChangingRoleToAdmin()">
      <b-checkbox v-model="adminConfirm">
        {{$t('admin-warning')}}
      </b-checkbox>
    </b-field>

    <b-field :label="$t('language')" horizontal>
      <b-select v-model="internalUser['language']">
        <option v-for="{value, name} in languages" :key="value" :value="value">
          {{name}}
        </option>
      </b-select>
    </b-field>

    <b-field :label="$t('developer-mode')" horizontal>
      <b-switch v-model="internalUser.isDeveloper" class="switch">
        <template v-if="internalUser.isDeveloper">{{$t('yes')}}</template>
        <template v-else>{{$t('no')}}</template>
      </b-switch>
    </b-field>

    <template #footer>
      <button class="button" type="button" @click="$emit('update:active', false)">
        {{$t('button-cancel')}}
      </button>
      <button class="button is-link" :disabled="errors.any() || !isAdminConfirmed()">
        {{$t('button-save')}}
      </button>
    </template>
  </cytomine-modal>
</form>
</template>

<script>
import {User, RoleCollection} from 'cytomine-client';
import {rolesMapping} from '@/utils/role-utils';
const defaultRole = 'ROLE_GUEST';
const defaultLanguage = {value: 'EN', name:'English'};

import CytomineModal from '@/components/utils/CytomineModal';

// TODO IAM
export default {
  name: 'user-modal',
  props: {
    active: Boolean,
    user: Object
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      internalUser: {},
      rolesWithIds: null,
      selectedRole: defaultRole,
      displayErrors: false,
      adminConfirm: false,
      languages: [
        {value: 'EN', name:'English'},
        {value: 'FR', name:'Français'},
        {value: 'ES', name:'Español'},
        {value: 'NL', name:'Nederlands'}
      ]
    };
  },
  computed: {
    roles() {
      return rolesMapping;
    },
    editionMode() {
      return Boolean(this.user);
    },
    title() {
      return this.$t(this.editionMode ? 'update-user' : 'create-user');
    },
    editableFields() {
      return [
        {field: 'firstname', validationRules: 'required'},
        {field: 'lastname', validationRules: 'required'},
        //{field: 'username', validationRules: 'required'},
        {field: 'email', validationRules: 'required|email'},
        {field: 'password', validationRules: this.editionMode ? 'min:8' : 'required|min:8'}
      ];
    },
    idRole() {
      return this.rolesWithIds.find(role => role.authority === this.selectedRole).id;
    }
  },
  watch: {
    selectedRole() {
      this.adminConfirm = !this.isChangingRoleToAdmin();
    },
    active(val) {
      if (val) {
        if (!this.rolesWithIds) {
          this.$notify({type: 'error', text: this.$t('notif-unexpected-error')});
          this.$emit('update:active', false);
          return;
        }
        this.internalUser = this.user ? this.user.clone() : new User();
        this.selectedRole = this.user ? this.user.role : defaultRole;
        this.internalUser.language = this.user ? this.user.language : defaultLanguage.value;
        this.displayErrors = false;
        this.adminConfirm = false;
      }
    }
  },
  methods: {
    isChangingRoleToAdmin() {
      let currentRole = this.user ? this.user.role : defaultRole;
      return this.isNotAdmin(currentRole) && !this.isNotAdmin(this.selectedRole);
    },
    isNotAdmin(role) {
      return role !== 'ROLE_ADMIN' && role !== 'ROLE_SUPER_ADMIN';
    },
    isAdminConfirmed() {
      return this.adminConfirm || !this.isChangingRoleToAdmin();
    },

    async save() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      let labelTranslation = this.editionMode ? 'update' : 'creation';

      try {
        await this.internalUser.save();
        if (!this.editionMode || this.selectedRole !== this.user.role) {
          await this.internalUser.defineRole(this.idRole);
          this.internalUser.role = this.selectedRole; // for correct rendering in list
        }
        if (this.editionMode && this.internalUser.password) {
          await this.internalUser.savePassword(this.internalUser.password);
        }
        this.internalUser.password = ''; // reinitialize password so that if modal reopened, field empty
        this.$notify({type: 'success', text: this.$t('notif-success-user-' + labelTranslation)});
        this.$emit('update:active', false);
        this.$emit(this.editionMode ? 'updateUser' : 'addUser', this.internalUser);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-user-' + labelTranslation)});
      }
    },
  },
  async created() {
    try {
      this.rolesWithIds = (await RoleCollection.fetchAll()).array;
    } catch (error) {
      console.log(error);
    }
  }
};
</script>

<style scoped>
.hidden {
  display: none;
}

>>> .modal-card, >>> .modal-card-body {
  width: 100vw;
  max-width: 800px;
}
</style>

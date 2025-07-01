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
  <form @submit.prevent="createImageGroup()">
    <cytomine-modal :active="active" :title="$t('create-image-group')" @close="$emit('update:active', false)">
      <b-field :label="$t('name')" :type="{'is-danger': errors.has('name')}" :message="errors.first('name')">
        <b-input v-model="name" name="name" v-validate="'required'" />
      </b-field>

      <template #footer>
        <button class="button" type="button" @click="$emit('update:active', false)">
          {{$t('button-cancel')}}
        </button>
        <button class="button is-link" :disabled="errors.any()">
          {{$t('button-save')}}
        </button>
      </template>
    </cytomine-modal>
  </form>
</template>

<script>
import {ImageGroup} from 'cytomine-client';
import CytomineModal from '@/components/utils/CytomineModal';
import {get} from '@/utils/store-helpers';

export default {
  name: 'add-image-group-modal',
  props: {
    active: Boolean
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      name: ''
    };
  },
  computed: {
    project: get('currentProject/project'),
  },
  watch: {
    active(val) {
      if (val) {
        this.name = '';
      }
    }
  },
  methods: {
    async createImageGroup() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      try {
        let imageGroup = await new ImageGroup({name: this.name, project: this.project.id}).save();
        this.$notify({type: 'success', text: this.$t('notif-success-image-group-creation')});
        this.$emit('update:active', false);
        this.$emit('newImageGroup', imageGroup);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-image-group-creation')});
      }
    }
  }
};
</script>

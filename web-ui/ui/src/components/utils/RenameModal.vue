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
<form @submit.prevent="rename()">
  <cytomine-modal :title="title" :active="active" @close="close()">
    <b-field :label="$t('new-name')" :type="{'is-danger': errors.has('name')}" :message="errors.first('name')">
      <b-input v-model="newName" name="name" v-validate="'required'" />
    </b-field>
    <template #footer>
      <button class="button" type="button" @click="close()">
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
import CytomineModal from './CytomineModal';

export default {
  name: 'rename-modal',
  extends: CytomineModal,
  props: {
    currentName: String,
    title: String
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      newName: ''
    };
  },
  watch: {
    active(active) {
      if (active) {
        this.newName = this.currentName;
      }
    }
  },
  methods: {
    async rename() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }
      this.$emit('rename', this.newName);
      this.$emit('update:active', false);
    }
  },
  async created() {
    this.newName = this.currentName;
  }
};
</script>

<style scoped>
>>> input[type=text] {
  width: 26em;
}
</style>

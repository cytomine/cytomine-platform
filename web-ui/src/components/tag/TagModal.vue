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
    <b-field :label="$t('name')" :type="{'is-danger': errors.has('name')}" :message="errors.first('name')">
      <b-input v-model="internalTag['name']" name="name" v-validate="'required'" />
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
import {Tag} from 'cytomine-client';
import CytomineModal from '@/components/utils/CytomineModal';

export default {
  name: 'tag-modal',
  props: {
    active: Boolean,
    tag: Object
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      internalTag: {},
      displayErrors: false,
    };
  },
  computed: {
    editionMode() {
      return Boolean(this.tag);
    },
    title() {
      return this.$t(this.editionMode ? 'update-tag' : 'create-tag');
    },
  },
  watch: {
    active(val) {
      if (val) {
        this.internalTag = (this.tag) ? this.tag.clone() : new Tag();
        this.displayErrors = false;
      }
    }
  },
  methods: {
    async save() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      let labelTranslation = this.editionMode ? 'update' : 'creation';

      try {
        await this.internalTag.save();
        this.$notify({type: 'success', text: this.$t('notif-success-tag-' + labelTranslation)});
        this.$emit('update:active', false);
        this.$emit(this.editionMode ? 'updateTag' : 'addTag', this.internalTag);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-tag-' + labelTranslation)});
      }
    }
  }
};
</script>

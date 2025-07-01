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
<cytomine-modal-card :title="$t('add-attached-file')">
  <b-field>
    <b-upload v-model="selectedFile" type="is-link" drag-drop>
      <section class="section">
        <div class="content has-text-centered">
          <template v-if="!selectedFile">
            <p><i class="fas fa-upload fa-3x"></i></p>
            <p>{{$t('choose-file-or-drop')}}</p>
          </template>
          <template v-else>
            <p class="filename is-size-4"><i class="fas fa-file"></i> {{selectedFile.name}}</p>
            <p class="has-text-grey is-size-6">{{$t('click-or-drop-new-file')}}</p>
          </template>
        </div>
      </section>
    </b-upload>
  </b-field>

  <b-field :label="$t('name')" :type="{'is-danger': errors.has('name')}" :message="errors.first('name')">
    <b-input v-model="name" :disabled="!selectedFile" name="name" v-validate="'required'" />
  </b-field>

  <template #footer>
    <button class="button" @click="$parent.close()">
      {{$t('button-cancel')}}
    </button>
    <button class="button is-link" :disabled="!selectedFile || errors.any()" @click="save()">
      {{$t('button-save')}}
    </button>
  </template>
</cytomine-modal-card>
</template>

<script>
import {AttachedFile} from 'cytomine-client';
import CytomineModalCard from '@/components/utils/CytomineModalCard';

export default {
  name: 'attached-file-modal',
  props: {
    object: Object
  },
  components: {CytomineModalCard},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      selectedFile: null,
      name: ''
    };
  },
  watch: {
    selectedFile(file) {
      if (file) {
        this.name = file.name;
      }
    }
  },
  methods: {
    async save() {
      let result = await this.$validator.validateAll('password');
      if (!this.selectedFile || !result) {
        return;
      }

      try {
        let attached = await new AttachedFile({file: this.selectedFile, filename: this.name}, this.object).save();
        this.$emit('addAttachedFile', attached);
        this.$notify({type: 'success', text: this.$t('notif-success-attached-file-creation')});
        this.$parent.close();
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-attached-file-creation')});
      }
    },
  }

};
</script>

<style scoped>
.filename {
  color: rgb(50, 115, 220);
}

.filename .fas {
  margin-right: 0.5em;
}

>>> .upload-draggable {
  margin-left: 10%;
  width: 80%;
}
</style>



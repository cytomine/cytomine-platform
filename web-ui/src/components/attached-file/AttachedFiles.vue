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
<div class="attached-files-wrapper">
  <template v-if="!loading">
    <em v-if="error">{{$t('error-attached-files')}}</em>
    <template v-else-if="attachedFiles.length > 0">
      <span class="file-item" v-for="(file, index) in attachedFiles" :key="file.id">
        <a @click="downloadAttachedFile(host, file)">{{file.filename}}</a>
        <button v-if="canEdit" class="delete is-small" @click="confirmDeletion(file, index)"></button>
        <template v-if="index < attachedFiles.length - 1">,</template>
      </span>
    </template>
    <em v-else>{{$t('no-attached-file')}} </em>
    <button v-if="canEdit" class="button is-small" @click="displayModal()">{{$t('button-add')}}</button>
  </template>
</div>
</template>

<script>
import {Cytomine, AttachedFileCollection} from 'cytomine-client';
import AttachedFileModal from './AttachedFileModal';
import {appendShortTermToken} from '@/utils/token-utils.js';
import {get} from '@/utils/store-helpers.js';

export default {
  name: 'attached-files',
  props: {
    object: {type: Object},
    canEdit: {type: Boolean, default: true}
  },
  data() {
    return {
      loading: true,
      error: false,
      attachedFiles: []
    };
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    host() {
      return Cytomine.instance.host;
    }
  },
  methods: {
    appendShortTermToken,
    displayModal() {
      // required to use programmatic modal because the description is sometimes displayed in elements with a
      // CSS transform (e.g. popover) that conflict with the fixed position of the modal
      // (http://meyerweb.com/eric/thoughts/2011/09/12/un-fixing-fixed-elements-with-css-transforms/)

      this.$buefy.modal.open({
        parent: this,
        component: AttachedFileModal,
        props: {object: this.object},
        hasModalCard: true,
        events: {'addAttachedFile': this.addAttachedFile}
      });
    },
    downloadAttachedFile(host, attachedFile) {
      window.location.assign(appendShortTermToken(host + attachedFile.url, this.shortTermToken));
    },
    addAttachedFile(attachedFile) {
      this.attachedFiles.push(attachedFile);
    },
    confirmDeletion(attachedFile, idx) {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-attached-file', {filename: attachedFile.filename}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteAttachedFile(attachedFile, idx)
      });
    },
    async deleteAttachedFile(attachedFile, idx) {
      try {
        await attachedFile.delete();
        this.attachedFiles.splice(idx, 1);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-attached-file-deletion', {filename: attachedFile.filename})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-attached-file-deletion', {filename: attachedFile.filename})
        });
      }
    }
  },
  async created() {
    try {
      this.attachedFiles = (await AttachedFileCollection.fetchAll({object: this.object})).array;
    } catch (error) {
      console.log(error);
      this.error = true;
    }
    this.loading = false;
  }

};
</script>

<style scoped>
.attached-files-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.file-item {
  margin-right: 0.5em;
}

.file-item .delete {
  margin-left: 0.4rem;
  margin-right: 0.1rem;
  position: relative;
  top: 2px;
}

.button {
  margin-left: 0.5em;
}
</style>

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
<cytomine-modal-card
  :title="$t('description')"
  class="description-modal"
  :class="{expanded: expanded}"
  @close="$parent.close()"
>
  <template #controls>
    <button class="button is-small" @click="expanded = !expanded">
      <i :class="['fas', expanded ? 'fa-compress' : 'fa-expand']"></i>
    </button>
  </template>

  <div v-if="!edit" class="ql-snow">
    <div class="ql-editor preview" v-html="descriptionWithoutKeywords"></div>
  </div>

  <template v-else>
    <div class="keyword-info">
      <i class="fas fa-info-circle"></i>
      <i18n path="info-keyword-stop-preview-description">
        <span place="keyword" class="keyword"> {{ stopPreviewKeyword }} </span>
      </i18n>
    </div>

    <cytomine-quill-editor v-model="descriptionContent" :placeholder="$t('enter-description')" />
  </template>

  <template v-if="edit" #footer>
    <button class="button" @click="$parent.close()">{{$t('button-cancel')}}</button>
    <button v-if="edit" class="button is-link" @click="save()"> {{ $t('button-save') }} </button>
  </template>
</cytomine-modal-card>
</template>

<script>
import CytomineQuillEditor from '@/components/form/CytomineQuillEditor';
import CytomineModalCard from '@/components/utils/CytomineModalCard';
import constants from '@/utils/constants.js';

export default {
  name: 'cytomine-description-modal',
  props: {
    description: Object,
    edit: Boolean
  },
  components: {
    CytomineModalCard,
    CytomineQuillEditor
  },
  data() {
    return {
      descriptionContent: '',
      expanded: false
    };
  },
  computed: {
    descriptionWithoutKeywords() {
      return this.description.data.replace(new RegExp(constants.STOP_PREVIEW_KEYWORD, 'g'), '');
    },
    stopPreviewKeyword() {
      return constants.STOP_PREVIEW_KEYWORD;
    }
  },
  methods: {
    async save() {
      let updatedDesc = this.description.clone();
      try {
        if (this.descriptionContent) {
          updatedDesc.data = this.descriptionContent;
          await updatedDesc.save();
          this.$emit('change', updatedDesc);
        } else if (this.description.data) { // if description existed, and content was emptied by user, delete description model
          await updatedDesc.delete();
          this.$emit('change', null);
        }
        this.$parent.close();
      } catch (error) {
        this.descriptionContent = this.description.data;
        this.$notify({type: 'error', text: this.$t('notif-error-update-description')});
      }
    }
  },
  created() {
    this.descriptionContent = this.description.data;
  }
};
</script>

<style lang="scss">
.description-modal {
  &.expanded, &.expanded .modal-card-body {
    width: 90vw;
    height: 90vh;
    max-height: 90vh;
  }

  &:not(.expanded), &:not(.expanded) .modal-card-body {
    width: 50vw;
    height: 60vh;
    max-height: 60vh;
  }

  .ql-editor.preview {
    padding: 0 0 1em 0;
    text-align: justify;
    white-space: normal;
  }

  .modal-card-body {
    display: flex;
    flex-direction: column;
    position: relative;
  }

  .keyword-info {
    margin-bottom: 1em;

    .fas {
      margin-right: 0.75em;
    }

    .keyword {
      font-weight: 600;
    }
  }

  .cytomine-quill-editor {
    flex: 1;
  }
}
</style>

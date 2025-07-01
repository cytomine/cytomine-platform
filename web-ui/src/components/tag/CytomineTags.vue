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
<div class="tags-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <template v-if="!loading">
    <b-field grouped group-multiline>
      <em v-if="error">{{$t('error-fetch-tags')}}</em>
      <div class="control" v-else-if="associatedTags.length > 0" v-for="(association, idx) in associatedTags" :key="association.id">
        <b-taglist attached>
          <b-tag type="is-info">
            {{association.tagName.toUpperCase()}}
            <span v-if="currentAccount.isDeveloper"> ({{$t('id')}}: {{association.tag}})</span>
          </b-tag>
          <b-tag v-if="canEdit">
            <button class="delete is-small" :title="$t('button-delete')" @click="removeTag(association, idx)">
            </button>
          </b-tag>
        </b-taglist>
      </div>
      <em v-else-if="associatedTags.length === 0">{{$t('no-tag')}}</em>

      <button v-if="canEdit" class="button is-small add-tag" @click="displayModal()">
        {{$t('button-add')}}
      </button>
    </b-field>
  </template>
</div>
</template>

<script>

import {Tag, TagDomainAssociation, TagDomainAssociationCollection} from 'cytomine-client';
import AddTagDomainAssociationModal from '@/components/tag/AddTagDomainAssociationModal';
import {get} from '@/utils/store-helpers';

export default {
  name: 'cytomine-tags',
  props: {
    object: {type: Object},
    canEdit: {type: Boolean, default: true}
  },
  data() {
    return {
      loading: true,
      error: false,
      addTagModal:false,
      associatedTags: []
    };
  },
  computed: {
    currentAccount: get('currentUser/account'),
  },
  methods: {
    displayModal() {
      //this.addTagModal = true;
      // required to use programmatic modal because the description is sometimes displayed in elements with a
      // CSS transform (e.g. popover) that conflict with the fixed position of the modal
      // (http://meyerweb.com/eric/thoughts/2011/09/12/un-fixing-fixed-elements-with-css-transforms/)

      this.$buefy.modal.open({
        parent: this,
        component: AddTagDomainAssociationModal,
        props: {associatedTags: this.associatedTags},
        hasModalCard: true,
        events: {'addObjects': this.addAssociations}
      });
    },
    sortAssociatedTags() {
      this.associatedTags.sort((a, b) => a.tagName.localeCompare(b.tagName));
    },
    async addAssociations(tags) {
      if (tags.length === 0) {
        this.$notify({type: 'error', text: this.$t('notif-error-add-tag-domain-associations')});
        return;
      }

      let [existingTags, newTags] = tags.reduce(
        (result, element) => {
          result[element instanceof Tag ? 0 : 1].push(element); // Determine and push to which arr
          return result;
        },
        [[], []] // Default values
      );

      try {
        let tagPromises = [];
        for (let i = 0; i < newTags.length; i++) {
          tagPromises.push(new Tag({name : newTags[i]}, this.object).save());
        }
        newTags = await Promise.all(tagPromises).then(function (values) {
          return values;
        });
        tags = existingTags.concat(newTags);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-add-tags')});
      }

      try {
        let associationPromises = [];
        for (let i = 0; i < tags.length; i++) {
          associationPromises.push(new TagDomainAssociation({tag : tags[i].id}, this.object).save());
        }
        let newAssocations = await Promise.all(associationPromises).then(function (values) {
          return values;
        });
        this.associatedTags = this.associatedTags.concat(newAssocations);
        this.sortAssociatedTags();
        this.$notify({type: 'success', text: this.$t('notif-success-add-tag-domain-association')});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-add-tag-domain-associations')});
      }
    },
    async removeTag(association, idx) {
      try {
        await TagDomainAssociation.delete(association.id);
        this.associatedTags.splice(idx, 1);
        this.$emit('update');
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-remove-tag')});
      }
    },
  },
  async created() {
    try {
      this.associatedTags = (await new TagDomainAssociationCollection({object: this.object}).fetchAll()).array;
      this.sortAssociatedTags();
    } catch (error) {
      console.log(error);
      this.error = true;
    }
    this.loading = false;
  }
};
</script>

<style scoped>
em {
  margin-right: 0.5em;
}
</style>

<style>
.tags-wrapper .control, .tags-wrapper .tags {
  margin-bottom: 0 !important;
}

.tags-wrapper .tag {
  margin-right: 0.5em;
  margin-bottom: 0.3em !important;
  background-color: rgba(0, 0, 0, 0.04);
}

.tags-wrapper .tag.is-dark {
  background-color: rgba(0, 0, 0, 0.1);
  color: black;
}

.tags-wrapper .field.is-grouped.is-grouped-multiline {
  margin-bottom: 0 !important;
}
</style>

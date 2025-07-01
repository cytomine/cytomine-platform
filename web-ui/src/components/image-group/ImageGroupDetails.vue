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
<div class="image-details-wrapper">
  <table class="table">
    <tbody>
    <tr v-if="isPropDisplayed('overview')">
      <td class="prop-label">{{$t('overview')}}</td>
      <td class="prop-content" colspan="3">
        <router-link
            v-if="imageGroup.imageInstances.length > 0"
            :to="viewerURL(imageGroup)"
        >
          <image-group-preview :image-group="imageGroup" />
        </router-link>
      </td>
    </tr>
    <tr v-if="isPropDisplayed('id') && currentUser.isDeveloper">
      <td class="prop-label">{{$t('id')}}</td>
      <td class="prop-content" colspan="3">{{imageGroup.id}}</td>
    </tr>
    <tr v-if="isPropDisplayed('created')">
      <td class="prop-label">{{$t('created-on')}}</td>
      <td class="prop-content" colspan="3">
        {{ Number(imageGroup.created) | moment('ll') }}
      </td>
    </tr>
    <tr v-if="isPropDisplayed('description')">
      <td class="prop-label">{{$t('description')}}</td>
      <td class="prop-content" colspan="3">
        <cytomine-description :object="imageGroup" :canEdit="canEdit" />
      </td>
    </tr>
    <tr v-if="isPropDisplayed('tags')">
      <td class="prop-label">{{$t('tags')}}</td>
      <td class="prop-content" colspan="3">
        <cytomine-tags :object="imageGroup" :canEdit="canEdit" />
      </td>
    </tr>
    <tr v-if="isPropDisplayed('properties')">
      <td class="prop-label">{{$t('properties')}}</td>
      <td class="prop-content" colspan="3">
        <cytomine-properties
            :object="imageGroup"
            :can-edit="canEdit"
            :properties="internalUseFilteredProperties"
            :error="loadPropertiesError"
            @added="addProp"
            @deleted="removeProp"
        />
      </td>
    </tr>
    <tr v-if="isPropDisplayed('attachedFiles')">
      <td class="prop-label">{{$t('attached-files')}}</td>
      <td class="prop-content" colspan="3">
        <attached-files :object="imageGroup" :canEdit="canEdit" />
      </td>
    </tr>

    <tr v-if="isPropDisplayed('images')">
      <td class="prop-label">{{$t('images')}}</td>
      <td class="prop-content image-thumbs" colspan="3">
        <div class="columns" v-if="imageGroup.imageInstances.length">
          <div class="column" v-for="image in imageGroup.imageInstances" :key="image.id">
            <image-preview :image="image" :project="project">
              <div class="buttons are-small">
                <button class="button is-small is-fullwidth" @click="confirmImageGroupLinkDeletion(image)">
                  {{$t('button-remove')}}
                </button>
              </div>

            </image-preview>
          </div>
        </div>
        <em v-else>{{$t('no-image')}}</em>
      </td>
    </tr>

    <tr>
      <td class="prop-label">{{$t('actions')}}</td>
      <td class="prop-content" colspan="3">
        <div class="buttons are-small">
          <template v-if="canEdit">
            <button v-if="!blindMode || canManageProject" class="button" @click="isRenameModalActive = true">
              {{$t('button-rename')}}
            </button>
            <button class="button" @click="isAddToModalActive = true">
              {{$t('button-add-images-to-image-group')}}
            </button>
            <button class="button is-danger" @click="confirmDeletion()">
              {{$t('button-delete')}}
            </button>
          </template>
        </div>
      </td>
    </tr>
    </tbody>
  </table>

  <rename-modal
      :title="$t('rename-image-group')"
      :currentName="imageGroup.name"
      :active.sync="isRenameModalActive"
      @rename="rename"
  />

  <add-to-image-group-modal
    :active.sync="isAddToModalActive"
    :image-group="imageGroup"
    @addToImageGroup="$emit('addToImageGroup', $event)"
  />
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import CytomineDescription from '@/components/description/CytomineDescription';
import CytomineProperties from '@/components/property/CytomineProperties';
import CytomineTags from '@/components/tag/CytomineTags';
import AttachedFiles from '@/components/attached-file/AttachedFiles';
import RenameModal from '@/components/utils/RenameModal';
import ImagePreview from '../image/ImagePreview';
import ImageGroupPreview from '@/components/image-group/ImageGroupPreview';
import AddToImageGroupModal from '@/components/image-group/AddToImageGroupModal';
import constants from '@/utils/constants';

import {ImageGroup, ImageGroupImageInstance, PropertyCollection} from 'cytomine-client';

export default {
  name: 'image-group-details',
  components: {
    CytomineDescription,
    CytomineTags,
    CytomineProperties,
    AttachedFiles,
    RenameModal,
    ImagePreview,
    ImageGroupPreview,
    AddToImageGroupModal
  },
  props: {
    imageGroup: {type: Object},
    excludedProperties: {type: Array, default: () => []},
    editable: {type: Boolean, default: false}
  },
  data() {
    return {
      isRenameModalActive: false,
      isAddToModalActive: false,
      loadPropertiesError: false,
      properties: [],
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    project: get('currentProject/project'),
    blindMode() {
      return ((this.$store.state.currentProject.project || {}).blindMode) || false;
    },
    canManageProject() {
      return this.$store.getters['currentProject/canManageProject'];
    },
    canEdit() {
      return this.editable && !this.currentUser.guestByNow && (this.canManageProject || !this.project.isReadOnly);
    },
    internalUseFilteredProperties() {
      return this.properties.filter(prop => !prop.key.startsWith(constants.PREFIX_HIDDEN_PROPERTY_KEY));
    },
  },
  methods: {
    removeProp(prop) {
      this.properties = this.properties.filter(p => p.id !== prop.id);
    },
    addProp(prop) {
      this.properties.push(prop);
    },

    isPropDisplayed(prop) {
      return !this.excludedProperties.includes(prop);
    },

    viewerURL(imageGroup) {
      let ids = imageGroup.imageInstances.map(img => img.id);
      return `/project/${imageGroup.project}/image/${ids.join('-')}`;
    },

    async rename(newName) {
      let oldName = this.imageGroup.name;
      try {
        // eslint-disable-next-line
        this.imageGroup.name = newName;
        await this.imageGroup.save();
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-group-rename', {imageName: this.imageGroup.name})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-group-rename', {imageName: oldName})
        });
      }
      this.isRenameModalActive = false;
    },

    confirmDeletion() {
      this.$buefy.dialog.confirm({
        title: this.$t('delete-image-group'),
        message: this.$t('delete-image-group-confirmation-message', {imageName: this.imageGroup.name}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: this.deleteImageGroup
      });
    },
    async deleteImageGroup() {
      try {
        await ImageGroup.delete(this.imageGroup.id);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-group-deletion', {imageName: this.imageGroup.name})
        });
        this.$emit('delete');

      } catch (err) {
        console.log(err);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-group-deletion', {imageName: this.imageGroup.name})
        });
      }
    },

    imageNameNotif(image) {
      return this.blindMode ? image.blindedName : image.instanceFilename;
    },
    confirmImageGroupLinkDeletion(image) {
      this.$buefy.dialog.confirm({
        title: this.$t('delete-image-group-link'),
        message: this.$t('delete-image-group-link-confirmation-message', {imageName: this.imageNameNotif(image)}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteImageGroupLink(image)
      });
    },
    async deleteImageGroupLink(image) {
      try {
        // currently, we limit an image instance to be associated to 1 group.
        await ImageGroupImageInstance.delete(this.imageGroup.id, image.id);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-group-link-deletion', {imageName: this.imageNameNotif(image)})
        });
        this.$emit('deleteImage', image);
      } catch (err) {
        console.log(err);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-group-link-deletion', {imageName: this.imageNameNotif(image)})
        });
      }
    },
  },
  async created() {
    try {
      this.properties = (await PropertyCollection.fetchAll({object: this.imageGroup})).array;
    } catch (error) {
      this.loadPropertiesError = true;
      console.log(error);
    }
  }
};
</script>

<style scoped>
.table {
  background: none;
  position: relative;
  margin-bottom: 0 !important;
}

td.prop-label {
  white-space: nowrap;
  font-weight: 600;
}

td.prop-content {
  width: 100%;
}

td.prop-content-half {
  width: 50%;
}

.image-overview {
  max-height: 18rem;
  max-width: 50vw;
}

.image-thumbs .column:not(.is-narrow) {
  max-width: 12.5rem;
  min-width: 12.5rem;
}

.image-thumbs .columns {
  flex-wrap: wrap;
}
</style>

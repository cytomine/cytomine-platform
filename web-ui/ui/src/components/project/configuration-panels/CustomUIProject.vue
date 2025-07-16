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
<b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
  <h2> {{ $t('error') }} </h2>
  <p> {{ $t('unexpected-error-info-message') }} </p>
</b-message>
<table class="table custom-ui" v-else-if="!loading">
  <tbody v-for="category in customUITree" :key="category.label">
    <tr>
      <th colspan="3">{{$t(category.label)}}</th>
    </tr>
    <tr v-for="prop in category.props" :key="prop.key">
      <td>
        <i v-if="prop.superscript" class="superscript" :class="prop.superscript"></i>
        <span class="icon" v-if="prop.icon || prop.iconComponent">
          <i v-if="prop.icon" :class="prop.icon"></i>
          <component v-else :is="prop.iconComponent" />
        </span>
        {{$t(prop.label)}}
      </td>
      <td>
        <button
          :class="['button', customUI[prop.key] && customUI[prop.key].ADMIN_PROJECT && !isParentDisabled(prop, 'ADMIN_PROJECT') ? 'is-success' : 'is-danger']"
          @click="toggleProp(prop.key, 'ADMIN_PROJECT')"
          :disabled="isParentDisabled(prop, 'ADMIN_PROJECT') || prop.key === 'project-configuration-tab'"
        >
          {{$t('manager')}}
        </button>
      </td>
      <td>
        <button
          :class="['button', customUI[prop.key] && customUI[prop.key].CONTRIBUTOR_PROJECT && !isParentDisabled(prop, 'CONTRIBUTOR_PROJECT') ? 'is-success' : 'is-danger']"
          @click="toggleProp(prop.key, 'CONTRIBUTOR_PROJECT')"
          :disabled="isParentDisabled(prop, 'CONTRIBUTOR_PROJECT') || prop.key === 'project-configuration-tab'"
        >
            {{$t('contributor')}}
        </button>
      </td>
    </tr>
  </tbody>
</table>
</template>

<script>
import {get} from '@/utils/store-helpers';

import IconPolygonFreeHand from '@/components/icons/IconPolygonFreeHand';
import IconLineFreeHand from '@/components/icons/IconLineFreeHand';
import IconUnlinkAnnotations from '@/components/icons/IconUnlinkAnnotations';

export default {
  name: 'custom-ui-project',
  data() {
    return {
      loading: true,
      error: false,

      customUI: {},
      customUITree: [
        {
          label: 'project-side-bar',
          props: [
            {key: 'project-images-tab', label: 'images', icon: 'far fa-image'},
            {key: 'project-image-groups-tab', label: 'image-groups', icon: 'far fa-images'},
            {key: 'project-annotations-tab', label: 'annotations', icon: 'far fa-edit'},
            {key: 'project-activities-tab', label: 'activity', icon: 'fas fa-tachometer-alt'},
            {key: 'project-information-tab', label: 'information', icon: 'fas fa-info-circle'},
            {key: 'project-configuration-tab', label: 'configuration', icon: 'fas fa-cogs'}
          ]
        },
        {
          label: 'image-details',
          props: [
            {key: 'project-explore-image-overview', label: 'overview', icon: 'fas fa-image'},
            //{key: 'project-explore-image-status', label: 'status'},
            {key: 'project-explore-image-description', label: 'description', icon: 'far fa-file-alt'},
            {key: 'project-explore-image-tags', label: 'tags', icon: 'fas fa-bookmark'},
            {key: 'project-explore-image-tracks', label: 'tracks', icon: 'fas fa-route'},
            {key: 'project-explore-image-properties', label: 'properties', icon: 'fas fa-tag'},
            {key: 'project-explore-image-attached-files', label: 'attached-files', icon: 'fas fa-paperclip'},
            {key: 'project-explore-image-slide-preview', label: 'slide-preview', icon: 'fas fa-image'},
            {key: 'project-explore-image-original-filename', label: 'originalFilename', icon: 'fas fa-info'},
            {key: 'project-explore-image-metadata', label: 'image-metadata', icon: 'fas fa-asterisk'},
            /*{key: 'project-explore-image-format', label: 'format'},
            {key: 'project-explore-image-vendor', label: 'vendor'},
            {key: 'project-explore-image-size', label: 'size'},
            {key: 'project-explore-image-resolution', label: 'resolution'},
            {key: 'project-explore-image-magnification', label: 'magnification'},*/
          ]
        },
        {
          label: 'viewer-panels',
          props: [
            {key: 'project-explore-hide-tools', label: 'all-panels'}, // hide all panels
            {key: 'project-explore-overview', label: 'overview', icon: 'fas fa-image', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-info', label: 'information', icon: 'fas fa-info', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-digital-zoom', label: 'digital-zoom', icon: 'fas fa-search', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-link', label: 'link-images', icon: 'fas fa-link', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-color-manipulation', label: 'colors', icon: 'fas fa-adjust', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-image-layers', label: 'annotation-layers', icon: 'fas fa-copy', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-ontology', label: 'ontology', icon: 'fas fa-hashtag', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-property', label: 'properties', icon: 'fas fa-tag', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-follow', label: 'broadcast', icon: 'fas fa-street-view', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-review', label: 'review', icon: 'fas fa-check-circle', parentConfiguration: 'project-explore-hide-tools'},
            {key: 'project-explore-annotations-list', label: 'annotations-list', icon: 'fas fa-list', parentConfiguration: 'project-explore-hide-tools'}
          ]
        },
        {
          label: 'annotation-details',
          props: [
            // display annotation details in viewer
            {key: 'project-explore-annotation-main', label: 'annotation-details-box'},
            {key: 'project-explore-annotation-geometry-info', label: 'geometry-info',
              icon: 'fas fa-ruler-combined', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-description', label: 'description', icon: 'far fa-file-alt', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-terms', label: 'terms', icon: 'fas fa-hashtag', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-tags', label: 'tags', icon: 'fas fa-bookmark', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-properties', label: 'properties', icon: 'fas fa-tag', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-attached-files', label: 'attached-files', icon: 'fas fa-paperclip', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-linked-annotations', label: 'linked-annotations', icon: 'fas fa-link', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-creation-info', label: 'creation-info', icon: 'fas fa-info', parentConfiguration: 'project-explore-annotation-main'},
            {key: 'project-explore-annotation-comments', label: 'comments', icon: 'fas fa-comment', parentConfiguration: 'project-explore-annotation-main'}
          ]
        },
        {
          label: 'draw-tools',
          props: [
            {key: 'project-tools-main', label: 'all-draw-tools'},

            {key: 'project-tools-select', label: 'select', icon: 'fas fa-mouse-pointer', parentConfiguration: 'project-tools-main'},

            {key: 'project-tools-point', label: 'point', icon: 'fas fa-map-pin', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-line', label: 'line', icon: 'fas fa-minus', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-freehand-line', label: 'freehand-line',
              iconComponent: IconLineFreeHand, parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-rectangle', label: 'rectangle', icon: 'far fa-square', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-circle', label: 'circle', icon: 'far fa-circle', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-polygon', label: 'polygon', icon: 'fas fa-draw-polygon', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-freehand-polygon', label: 'freehand-polygon',
              iconComponent: IconPolygonFreeHand, parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-screenshot', label: 'screenshot', icon: 'fas fa-camera', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-union', label: 'freehand-correct-add', icon: 'fas fa-pencil-alt',
              superscript: 'fas fa-plus', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-diff', label: 'freehand-correct-remove', icon: 'fas fa-pencil-alt',
              superscript: 'fas fa-minus', parentConfiguration: 'project-tools-main'},

            {key: 'project-tools-fill', label: 'fill', icon: 'fas fa-fill', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-edit', label: 'modify', icon: 'fas fa-edit', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-move', label: 'move', icon: 'fas fa-arrows-alt', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-resize', label: 'rescale', icon: 'fas fa-expand', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-rotate', label: 'rotate', icon: 'fas fa-sync-alt', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-delete', label: 'delete', icon: 'fas fa-trash-alt', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-copy-paste', label: 'copy-paste', icon: 'fas fa-copy', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-link', label: 'link', icon: 'fas fa-link', parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-unlink', label: 'unlink', iconComponent: IconUnlinkAnnotations, parentConfiguration: 'project-tools-main'},
            {key: 'project-tools-undo-redo', label: 'undo-redo', icon: 'fas fa-undo', parentConfiguration: 'project-tools-main'},
          ]
        }
      ]
    };
  },
  computed: {
    project: get('currentProject/project')
  },
  methods: {
    async toggleProp(prop, userType) {
      try {
        this.customUI[prop][userType] = !this.customUI[prop][userType];
        this.customUI = await this.project.saveUIConfig(this.customUI);
        this.$store.dispatch('currentProject/fetchUIConfig');
      } catch (error) {
        console.log(error);
        this.customUI[prop][userType] = !this.customUI[prop][userType]; // revert change
        this.$notify({type: 'error', text: this.$t('notif-error-custom-ui-change')});
      }
    },
    isParentDisabled(child, role) {
      return (child.parentConfiguration && this.customUI[child.parentConfiguration][role] === false);
    }
  },
  async created() {
    try {
      this.customUITree[0].props.splice(2,1);
      this.customUI = await this.project.fetchUIConfig();
      console.log(this.customUI);
    } catch (error) {
      console.log(error);
      this.error = true;
    }
    this.loading = false;
  }
};
</script>


<style scoped>
.button {
  padding: 0 3em;
}

td, th {
  vertical-align: middle !important;
}

.icon {
  width: 20px;
  text-align: center;
  margin-right: 1em;
}

.superscript {
  position: relative;
  font-size: 0.7em;
  width: 0;
  bottom: 0.75em;
  right: 4px;
  margin-right: 0;
}
</style>

<style>
.custom-ui .icon svg {
  height: 1.15em !important;
}
</style>

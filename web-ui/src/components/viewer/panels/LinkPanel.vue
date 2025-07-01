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
<div>
  <h1>{{$t('link-images')}}</h1>

  <b-field horizontal :label="$t('link-mode')">
    <b-select v-model="linkMode" size="is-small" expanded>
      <option v-for="option in modeOptions" :key="option.label" :value="option.key">
        {{option.label}}
      </option>
    </b-select>
  </b-field>

  <template>
    <table class="table">
      <thead>
      <tr>
        <th><span class="fas fa-link"></span></th>
        <th> {{ $t('link-view-with') }}</th>
      </tr>
      </thead>
      <tbody>
      <!-- Display linked images -->
      <tr v-for="index in linkedIndexes" :key="index">
        <td>
          <b-checkbox :value="true" @input="unlink"/>
        </td>
        <td>
          {{$t('viewer-view', {number: imagesWithNum[index].number})}}
          (<image-name :image="imagesWithNum[index].image" />)
        </td>
      </tr>

      <!-- Display other groups to linked with -->
      <tr v-for="{images, index, number} in otherGroups" :key="`group${number}`">
        <td>
          <b-checkbox :value="false" @change.native="event => handleCheckboxChange(event, index)"/>
        </td>
        <td>
          {{$t('link-group', {number})}}
          <ul class="group">
            <li v-for="indexImage in images" :key="indexImage">
              <i class="fas fa-caret-right"></i>
              {{$t('viewer-view', {number: imagesWithNum[indexImage].number})}}
              (<image-name :image="imagesWithNum[indexImage].image" />) <br/>
            </li>
          </ul>
        </td>
      </tr>

      <!-- Display unlinked images -->
      <tr v-for="{image, index, number} in otherSoloImages" :key="index">
        <td>
          <b-checkbox :value="false" @change.native="event => handleCheckboxChange(event, null, index)"/>
        </td>
        <td>
          {{$t('viewer-view', {number})}} (<image-name :image="image" />)
        </td>
      </tr>
      </tbody>
    </table>
  </template>

</div>
</template>

<script>
import ImageName from '@/components/image/ImageName';

export default {
  name: 'link-panel',
  props: {
    index: String
  },
  components: {ImageName},
  data() {
    return {
      revisionNumber: 0
    };
  },
  computed: {
    modeOptions: function () {
      return [
        {key: 'ABSOLUTE', label: this.$t('absolute-link-mode')},
        {key: 'RELATIVE', label: this.$t('relative-link-mode')}
      ];
    },
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    linkMode: {
      get() {
        return this.viewerWrapper.linkMode;
      },
      set(mode) {
        this.$store.commit(this.viewerModule + 'setLinkMode', mode);
      }
    },
    images() {
      return this.viewerWrapper.images;
    },
    imagesWithNum() {
      let number = 1;
      return Object.keys(this.images).reduce((obj, index) => {
        obj[index] = {
          number,
          index,
          image: this.images[index].imageInstance
        };
        number++;
        return obj;
      }, []);
    },
    linkGroups() {
      return this.viewerWrapper.links;
    },
    linkGroupsWithNum() {
      let number = 1;
      return this.linkGroups.reduce((obj, images, index) => {
        obj[index] = {
          number,
          index,
          images
        };
        number++;
        return obj;
      }, []);
    },
    indexCurrentGroup() {
      return this.linkGroups.findIndex(group => group.includes(this.index));
    },
    linkedIndexes() {
      if (this.indexCurrentGroup === -1) {
        return null;
      }
      return this.linkGroups[this.indexCurrentGroup].filter(index => index !== this.index);
    },
    otherGroups() {
      return Object.values(this.linkGroupsWithNum).filter(group => group.index !== this.indexCurrentGroup);
    },
    inGroupsIndexes() { // list of images belonging to a link group
      let idxs = [];
      this.linkGroups.forEach(group => idxs.push(...group));
      return idxs;
    },
    otherSoloImages() {
      return Object.values(this.imagesWithNum).filter(img => {
        return img.index !== this.index && !this.inGroupsIndexes.includes(img.index);
      });
    },
    trackedUser() {
      return this.viewerWrapper.images[this.index].tracking.trackedUser;
    },
  },
  methods: {
    handleCheckboxChange(event, indexGroup, indexImage) {
      if (this.trackedUser) {
        this.$buefy.dialog.confirm({
          title: this.$t('possible-conflict'),
          message: this.$t('confirm-untrack-to-link-view'),
          confirmText: this.$t('button-confirm'),
          cancelText: this.$t('button-cancel'),
          onConfirm: () => {
            this.$store.commit(this.imageModule + 'setTrackedUser', null);
            this.link(indexGroup, indexImage);
          },
          onCancel: () => event.target.checked = false // reset state of checkbox
        });
      } else {
        this.link(indexGroup, indexImage);
      }
    },

    link(indexGroup, indexImage) {
      if (this.linkedIndexes) {
        if (indexGroup !== null) {
          this.$store.commit(this.viewerModule + 'mergeLinkGroups', [this.indexCurrentGroup, indexGroup]);
        } else {
          this.$store.commit(this.viewerModule + 'linkImageToGroup', {indexGroup: this.indexCurrentGroup, indexImage});
        }
      } else {
        if (indexGroup !== null) {
          this.$store.commit(this.viewerModule + 'linkImageToGroup', {indexGroup, indexImage: this.index});
        } else {
          this.$store.commit(this.viewerModule + 'createLinkGroup', [this.index, indexImage]);
        }
      }
    },

    unlink() {
      this.$store.commit(this.viewerModule + 'unlinkImage', {indexGroup: this.indexCurrentGroup, indexImage: this.index});
    }
  }
};
</script>

<style lang="scss" scoped>
.current-group:not(:last-child) {
  margin-bottom: 1em;
}

ul {
  color: rgba(0, 0, 0, 0.75);
  margin-left: 0.5em;
}

ul.group {
  font-size: 0.9em;
}

.fas {
  margin-right: 0.5em;
}

>>> .b-checkbox {
  align-items: flex-start !important;
  margin-top: 0.25em;
}

.table {
  margin-bottom: 1em !important;
  font-size: 0.9em;
}

.table tbody {
  display: block;
  overflow-y: scroll;
  overflow-x: hidden;
  max-height: 10em;
}

.table thead tr {
  display: block;
}
</style>

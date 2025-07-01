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
  <h1>
    {{$t('information')}}
  </h1>
  <table class="table">
    <tbody>
      <tr v-if="currentAccount.isDeveloper">
        <td><strong>{{$t('id')}}</strong></td>
        <td>{{image.id}}</td>
      </tr>
      <tr>
        <td><strong>{{$t('name')}}</strong></td>
        <td><image-name :image="image" showBothNames /></td>
      </tr>
      <tr v-if="hasGroup">
        <td><strong>{{$t('image-group')}}</strong></td>
        <td>{{imageGroupLink.groupName}}</td>
      </tr>
      <tr>
        <td><strong>{{$t('width')}}</strong></td>
        <td>{{image.width}} {{$t("pixels")}}</td>
      </tr>
      <tr>
        <td><strong>{{$t('height')}}</strong></td>
        <td>{{image.height}} {{$t("pixels")}}</td>
      </tr>
      <tr v-if="image.depth > 1">
        <td><strong>{{$t('image-depth')}}</strong></td>
        <td>{{$tc("count-slices", image.depth, {count: image.depth})}}</td>
      </tr>
      <tr v-if="image.duration > 1">
        <td><strong>{{$t('image-time')}}</strong></td>
        <td>{{$tc("count-frames", image.duration, {count: image.duration})}}</td>
      </tr>
      <tr v-if="image.channels > 1">
        <td><strong>{{$t('image-channels')}}</strong></td>
        <td>
          {{$tc("count-bands", image.apparentChannels, {count: image.apparentChannels})}}
          ({{image.channels}} x {{image.samplePerPixel}})
        </td>
      </tr>
      <tr>
        <td><strong>{{$t('resolution')}}</strong></td>
        <td v-if="image.physicalSizeX">{{image.physicalSizeX.toFixed(3)}} {{$t("um-per-pixel")}}</td>
        <td v-else>{{$t("unknown")}}</td>
      </tr>
      <tr v-if="image.depth > 1">
        <td><strong>{{$t('z-resolution')}}</strong></td>
        <td v-if="image.physicalSizeZ">{{image.physicalSizeZ.toFixed(3)}} {{$t("um-per-slice")}}</td>
        <td v-else>{{$t("unknown")}}</td>
      </tr>
      <tr v-if="image.duration > 1">
        <td><strong>{{$t('frame-rate')}}</strong></td>
        <td v-if="image.fps">{{image.fps.toFixed(3)}} {{$t("frame-per-second")}}</td>
        <td v-else>{{$t("unknown")}}</td>
      </tr>
      <tr>
        <td><strong>{{$t('magnification')}}</strong></td>
        <td v-if="image.magnification">{{image.magnification}}</td>
        <td v-else>{{$t('unknown')}}</td>
      </tr>
      <tr>
        <td><strong>{{ $t('image-metadata') }}</strong></td>
        <td>
          <button class="button is-small" @click="$emit('openMetadata')">
            {{ $t('button-metadata') }}
          </button>
        </td>
      </tr>
      <tr>
        <td colspan="2" class="buttons-wrapper">
          <div class="buttons">
            <button v-if="canEdit" class="button is-small" @click="calibrationModal = true">
              {{$t('button-set-calibration')}}
            </button>
            <router-link :to="`/project/${image.project}/image/${image.id}/information`" class="button is-small">
              {{$t('button-more-info')}}
            </router-link>
            <a class="button is-small" v-if="canDownloadImages" @click="download(image, shortTermToken)">
              {{$t('button-download')}}
            </a>
          </div>
        </td>
      </tr>
      <tr>
        <td colspan="2" class="buttons-wrapper">
          <div class="buttons navigation has-addons">
            <button class="button is-small" @click="previousImage()" :disabled="isFirstImage">
              <i class="fas fa-angle-left fa-lg"></i> {{$t('button-previous-image')}}
            </button>
            <button class="button is-small" @click="nextImage()" :disabled="isLastImage">
              {{$t('button-next-image')}} <i class="fas fa-angle-right fa-lg"></i>
            </button>
          </div>
        </td>
      </tr>
      <tr v-if="hasGroup">
        <td colspan="2" class="buttons-wrapper">
          <div class="buttons navigation has-addons">
            <button class="button is-small" @click="previousImageInGroup()">
              <i class="fas fa-angle-left fa-lg"></i> {{$t('button-previous-image-in-group')}}
            </button>
            <button class="button is-small" @click="nextImageInGroup()">
              {{$t('button-next-image-in-group')}} <i class="fas fa-angle-right fa-lg"></i>
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>

  <calibration-modal
    :image="image"
    :active.sync="calibrationModal"
    @setResolution="setResolution"
  />
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import ImageName from '@/components/image/ImageName';
import CalibrationModal from '@/components/image/CalibrationModal';
import {appendShortTermToken} from '@/utils/token-utils.js';

export default {
  name: 'information-panel',
  components: {
    ImageName,
    CalibrationModal
  },
  props: {
    index: String
  },
  data() {
    return {
      calibrationModal: false,
      isFirstImage: false,
      isLastImage: false,
    };
  },
  computed: {
    currentAccount: get('currentUser/account'),
    shortTermToken: get('currentUser/shortTermToken'),
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    image() {
      return this.viewerWrapper.images[this.index].imageInstance;
    },
    hasGroup() {
      return this.viewerWrapper.images[this.index].imageGroupLink !== null;
    },
    imageGroupLink() {
      return this.viewerWrapper.images[this.index].imageGroupLink;
    },
    canEdit() {
      return this.$store.getters['currentProject/canEditImage'](this.image);
    },
    canDownloadImages() {
      // Virtual images (null path) cannot be downloaded.
      return this.image.path !== null && (
        this.canManageProject ||
        ((this.$store.state.currentProject.project || {}).areImagesDownloadable) || false
      );
    },
    canManageProject() {
      return this.$store.getters['currentProject/canManageProject'];
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    }
  },
  methods: {
    appendShortTermToken,
    setResolution(resolution) {
      this.$store.dispatch(this.viewerModule + 'setImageResolution', {idImage: this.image.id, resolution});
      this.$eventBus.$emit('reloadAnnotations', {idImage: this.image.id}); // refresh the sources to update perimeter/area
    },
    download(image) {
      window.location.assign(appendShortTermToken(image.downloadURL, this.shortTermToken));
    },
    async previousImage() {
      try {
        let prev = await this.image.fetchPrevious();
        if (!prev.id) {
          this.$notify({type: 'error', text: this.$t('notif-error-first-image')});
          this.isFirstImage = true;
        } else {
          let slice = await prev.fetchReferenceSlice();
          await this.$store.dispatch(this.imageModule + 'setImageInstance', {image: prev, slices: [slice]});
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-fetch-previous-image')});
      }
    },
    async nextImage() {
      try {
        let next = await this.image.fetchNext();
        if (!next.id) {
          this.$notify({type: 'error', text: this.$t('notif-error-last-image')});
          this.isLastImage = true;
        } else {
          let slice = await next.fetchReferenceSlice();
          await this.$store.dispatch(this.imageModule + 'setImageInstance', {image: next, slices: [slice]});
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-fetch-next-image')});
      }
    },
    async previousImageInGroup() {
      if (!this.hasGroup) {
        return;
      }

      try {
        let prev = await this.imageGroupLink.fetchPrevious();
        if (!prev.id) {
          this.$notify({type: 'error', text: this.$t('notif-error-first-image')});
        } else {
          let slice = await prev.fetchReferenceSlice();
          await this.$store.dispatch(this.imageModule + 'setImageInstance', {image: prev, slices: [slice]});
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-fetch-previous-image')});
      }
    },
    async nextImageInGroup() {
      if (!this.hasGroup) {
        return;
      }

      try {
        let next = await this.imageGroupLink.fetchNext();
        if (!next.id) {
          this.$notify({type: 'error', text: this.$t('notif-error-last-image')});
        } else {
          let slice = await next.fetchReferenceSlice();
          await this.$store.dispatch(this.imageModule + 'setImageInstance', {image: next, slices: [slice]});
        }
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-fetch-next-image')});
      }
    },

    shortkeyHandler(key) {
      if (!this.isActiveImage) { // shortkey should only be applied to active map
        return;
      }

      if (key === 'nav-next-image') {
        this.nextImage();
      } else if (key === 'nav-previous-image') {
        this.previousImage();
      } else if (key === 'nav-next-image-in-group') {
        this.nextImageInGroup();
      } else if (key === 'nav-previous-image-in-group') {
        this.previousImageInGroup();
      }
    }
  },
  mounted() {
    this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
  }
};
</script>

<style scoped>
.table {
  margin-bottom: 0 !important;
  width: 100%;
  table-layout: fixed;
}

td {
  word-wrap: break-word;
}

td:first-child {
  width: 10em;
}

.buttons-wrapper {
  padding: 0;
}

.actions {
  padding-left: 0;
  padding-right: 0;
}

.buttons {
  justify-content: center;
}

.buttons.navigation {
  margin-top: 0.7em;
  margin-bottom: 0;
  border-width: 0;
}

.fa-angle-left {
  margin-right: 0.4em;
}

.fa-angle-right {
  margin-left: 0.4em;
}
</style>

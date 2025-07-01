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
  <b-message v-if="error" type="is-danger" has-icon icon-size="is-small" size="is-small">
    <h2> {{ $t('error') }} </h2>
    <p> {{ $t('unexpected-error-info-message') }} </p>
  </b-message>
  <table v-else class="table">
    <b-loading :is-full-page="false" :active="loading" class="small" />
    <tbody v-if="!loading">
      <tr v-if="isPropDisplayed('overview')">
        <td class="prop-label">{{$t('overview')}}</td>
        <td class="prop-content" colspan="3">
          <router-link :to="`/project/${image.project}/image/${image.id}`">
            <image-thumbnail :image="image" :size="256" :key="`${image.id}-thumb-256`" :extra-parameters="{Authorization: 'Bearer ' + shortTermToken }"/>
          </router-link>
        </td>
      </tr>
      <tr v-if="isPropDisplayed('id') && currentAccount.isDeveloper">
        <td class="prop-label">{{$t('id')}}</td>
        <td class="prop-content" colspan="3">{{image.id}}</td>
      </tr>
      <tr v-if="isPropDisplayed('status')">
        <td class="prop-label">{{$t('status')}}</td>
        <td class="prop-content" colspan="3">
          <image-status :image="image" />
        </td>
      </tr>
      <tr v-if="isPropDisplayed('numberOfAnnotations')">
        <td class="prop-label">{{$t('user-annotations')}}</td>
        <td class="prop-content" colspan="3">
          <router-link :to="`/project/${image.project}/annotations?image=${image.id}&type=user`">
            {{ image.numberOfAnnotations }}
          </router-link>
        </td>
      </tr>
      <tr v-if="isPropDisplayed('numberOfReviewedAnnotations')">
        <td class="prop-label">{{$t('reviewed-annotations')}}</td>
        <td class="prop-content" colspan="3">
          <router-link :to="`/project/${image.project}/annotations?image=${image.id}&type=reviewed`">
            {{ image.numberOfReviewedAnnotations }}
          </router-link>
        </td>
      </tr>
      <tr v-if="isPropDisplayed('description')">
        <td class="prop-label">{{$t('description')}}</td>
        <td class="prop-content" colspan="3">
          <cytomine-description :object="image" :canEdit="canEdit" />
        </td>
      </tr>
      <tr v-if="isPropDisplayed('tags')">
        <td class="prop-label">{{$t('tags')}}</td>
        <td class="prop-content" colspan="3">
          <cytomine-tags :object="image" :canEdit="canEdit" />
        </td>
      </tr>
      <tr v-if="isPropDisplayed('properties')">
        <td class="prop-label">{{$t('properties')}}</td>
        <td class="prop-content" colspan="3">
          <cytomine-properties :object="image" :canEdit="canEdit" />
        </td>
      </tr>
      <tr v-if="isPropDisplayed('attached-files')">
        <td class="prop-label">{{$t('attached-files')}}</td>
        <td class="prop-content" colspan="3">
          <attached-files :object="image" :canEdit="canEdit" />
        </td>
      </tr>
      <tr v-if="isPropDisplayed('slide-preview')">
        <td class="prop-label">{{$t('slide-preview')}}</td>
        <td class="prop-content" colspan="3">
          <a v-if="image.macroURL && !isBlindModeAndContributor" @click="isMetadataModalActive = true">
            <image-thumbnail :image="image" :macro="true" :size="256" :key="`${image.id}-macro-256`" :extra-parameters="{Authorization: 'Bearer ' + shortTermToken }"/>
          </a>
          <em v-else>
            {{$t('slide-preview-not-available')}}
          </em>
        </td>
      </tr>
      <tr v-if="isPropDisplayed('original-filename') && (!blindMode || canManageProject)">
        <td class="prop-label">{{$t('originalFilename')}}</td>
        <td class="prop-content" colspan="3">
          {{image.originalFilename}}
        </td>
      </tr>
      <tr v-if="isPropDisplayed('format')">
        <td class="prop-label">{{$t('format')}}</td>
        <td class="prop-content format" colspan="3">
          {{image.contentType}}
        </td>
      </tr>
      <tr v-if="isPropDisplayed('vendor')">
        <td class="prop-label">{{$t('vendor')}}</td>
        <td class="prop-content" colspan="3">
          <img v-if="vendor" :src="vendor.imgPath" :alt="vendor.name" :title="vendor.name" class="vendor-img">
          <template v-else>{{$t('unknown')}}</template>
        </td>
      </tr>
      <tr v-if="isPropDisplayed('width') || isPropDisplayed('physicalSizeX')">
        <template v-if="isPropDisplayed('width')">
          <td class="prop-label">{{$t("image-width")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('physicalSizeX') ? 1 : 3">
            {{image.width}} {{$t("pixels")}}
            <template v-if="image.physicalSizeX">({{(image.width * image.physicalSizeX).toFixed(3)}} {{$t("um")}})</template>
          </td>
        </template>
        <template v-if="isPropDisplayed('physicalSizeX')">
          <td class="prop-label">{{$t("x-y-resolution")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('width') ? 1 : 3">
            <template v-if="image.physicalSizeX">{{image.physicalSizeX.toFixed(3)}} {{$t("um-per-pixel")}}</template>
            <template v-else>{{$t("unknown")}}</template>
          </td>
        </template>
      </tr>
      <tr v-if="isPropDisplayed('height') || isPropDisplayed('physicalSizeY')">
        <template v-if="isPropDisplayed('height')">
          <td class="prop-label">{{$t("image-height")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('physicalSizeY') ? 1 : 3">
            {{image.height}} {{$t("pixels")}}
            <template v-if="image.physicalSizeY">({{(image.height * image.physicalSizeY).toFixed(3)}} {{$t("um")}})</template>
          </td>
        </template>
        <template v-if="isPropDisplayed('physicalSizeY')">
          <!-- We don't support diff X&Y yet in some components uncomment to bring back -->
          <!-- <td class="prop-label">{{$t("y-resolution")}}</td> -->
          <td class="prop-label"></td>
          <td class="prop-content-half" :colspan="isPropDisplayed('height') ? 1 : 3">
            <!-- <template v-if="image.physicalSizeY">{{image.physicalSizeY.toFixed(3)}} {{$t("um-per-pixel")}}</template>
            <template v-else>{{$t("unknown")}}</template> -->
          </td>
        </template>
      </tr>
      <tr v-if="isPropDisplayed('depth') || isPropDisplayed('physicalSizeZ')">
        <template v-if="isPropDisplayed('depth')">
          <td class="prop-label">{{$t("image-depth")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('physicalSizeZ') ? 1 : 3">
            {{$tc("count-slices", image.depth, {count: image.depth})}}
            <template v-if="image.physicalSizeZ">({{(image.depth * image.physicalSizeZ).toFixed(3)}} {{$t("um")}})</template>
          </td>
        </template>
        <template v-if="isPropDisplayed('physicalSizeZ')">
          <td class="prop-label">{{$t("z-resolution")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('depth') ? 1 : 3">
            <template v-if="image.physicalSizeZ">{{image.physicalSizeZ.toFixed(3)}} {{$t("um-per-slice")}}</template>
            <template v-else-if="image.depth < 2">-</template>
            <template v-else>{{$t("unknown")}}</template>
          </td>
        </template>
      </tr>
      <tr v-if="isPropDisplayed('time') || isPropDisplayed('fps')">
        <template v-if="isPropDisplayed('time')">
          <td class="prop-label">{{$t("image-time")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('fps') ? 1 : 3">
            {{$tc("count-frames", image.duration, {count: image.duration})}}
            <template v-if="image.fps && image.duration > 0">
              ({{formatMinutesSeconds(image.duration / image.fps)}})
            </template>
          </td>
        </template>
        <template v-if="isPropDisplayed('fps')">
          <td class="prop-label">{{$t("frame-rate")}}</td>
          <td class="prop-content-half" :colspan="isPropDisplayed('time') ? 1 : 3">
            <template v-if="image.fps">{{image.fps.toFixed(3)}} {{$t("frame-per-second")}}</template>
            <template v-else-if="image.time < 2">-</template>
            <template v-else>{{$t("unknown")}}</template>
          </td>
        </template>
      </tr>
      <tr v-if="isPropDisplayed('channels')">
        <td class="prop-label">{{$t("image-channels")}}</td>
        <td class="prop-content" colspan="3">
          {{$tc("count-bands", image.apparentChannels, {count: image.apparentChannels})}}
          ({{image.channels}} x {{image.samplePerPixel}})
        </td>
      </tr>
      <tr v-if="isPropDisplayed('size')">
        <td class="prop-label">{{$t('image-size')}}</td>
        <td class="prop-content" colspan="3">
          {{`${image.width} x ${image.height} ${$t('pixels')}`}}
        </td>
      </tr>
      <tr v-if="isPropDisplayed('magnification')">
        <td class="prop-label">{{$t('magnification')}}</td>
        <td class="prop-content" colspan="3">
          <template v-if="image.magnification">{{image.magnification}}</template>
          <template v-else>{{$t('unknown')}}</template>
        </td>
      </tr>
      <tr>
        <td class="prop-label">{{$t('actions')}}</td>
        <td class="prop-content" colspan="3">
          <div class="buttons are-small">
            <button v-if="isPropDisplayed('metadata') && !isBlindModeAndContributor" class="button" @click="isMetadataModalActive = true">
              {{$t('button-metadata')}}
            </button>
            <template v-if="canAddToImageGroup">
              <button class="button" v-if="!isInImageGroup" @click="isAddToImageGroupModalActive = true">
                {{$t('button-add-to-image-group')}}
              </button>
              <button class="button" v-else @click="confirmImageGroupLinkDeletion()">
                {{$t('button-remove-from-image-group')}}
              </button>
            </template>

            <template v-if="canEdit">

              <router-link
                v-if="!image.reviewed && !image.inReview"
                :to="`/project/${image.project}/image/${image.id}?action=review`"
                class="button"
              >
                {{$t('button-start-review')}}
              </router-link>

              <template v-else-if="image.reviewUser === currentUser.id">
                <button v-if="image.reviewed" class="button" @click="cancelReview()">
                  {{$t('button-unvalidate-review')}}
                </button>
                <template v-else>
                  <router-link :to="`/project/${image.project}/image/${image.id}?action=review`" class="button">
                    {{$t('button-continue-review')}}
                  </router-link>
                  <button class="button" @click="cancelReview()">
                    {{$t('button-cancel-review')}}
                  </button>
                </template>
              </template>

              <button v-if="!blindMode || canManageProject" class="button" @click="isRenameModalActive = true">
                {{$t('button-rename')}}
              </button>
              <button class="button" @click="isCalibrationModalActive = true">
                {{$t('button-set-calibration')}}
              </button>
              <button class="button" @click="isMagnificationModalActive = true">
                {{$t('button-set-magnification')}}
              </button>
            </template>
            <a class="button" v-if="canDownloadImages || canManageProject" @click="download(image)">
              {{$t('button-download')}}
            </a>
            <template v-if="canEdit">
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
    :title="$t('rename-image')"
    :currentName="image.instanceFilename"
    :active.sync="isRenameModalActive"
    @rename="rename"
  />

  <magnification-modal
    :image="image"
    :active.sync="isMagnificationModalActive"
    @setMagnification="(event) => $emit('setMagnification', event)"
  />

  <calibration-modal
    :image="image"
    :active.sync="isCalibrationModalActive"
    @setResolution="(event) => $emit('setResolution', event)"
  />

  <image-metadata-modal
    :active.sync="isMetadataModalActive"
    :image="image"
  />

  <simple-add-to-image-group-modal
    :active.sync="isAddToImageGroupModalActive"
    :image="image"
    @addToImageGroup="(event) => imageGroupLinks.push(event)"
  />
</div>

</template>

<script>
import {ImageInstance, ImageGroupImageInstanceCollection} from 'cytomine-client';

import {appendShortTermToken} from '@/utils/token-utils.js';
import {formatMinutesSeconds} from '@/utils/slice-utils.js';
import {get} from '@/utils/store-helpers';
import vendorFromFormat from '@/utils/vendor';

import AttachedFiles from '@/components/attached-file/AttachedFiles';
import CalibrationModal from '@/components/image/CalibrationModal';
import CytomineDescription from '@/components/description/CytomineDescription';
import CytomineProperties from '@/components/property/CytomineProperties';
import CytomineTags from '@/components/tag/CytomineTags';
import ImageMetadataModal from '@/components/image/ImageMetadataModal';
import ImageStatus from '@/components/image/ImageStatus';
import ImageThumbnail from '@/components/image/ImageThumbnail';
import MagnificationModal from '@/components/image/MagnificationModal';
import RenameModal from '@/components/utils/RenameModal';
import SimpleAddToImageGroupModal from '@/components/image-group/SimpleAddToImageGroupModal';

export default {
  name: 'image-details',
  components: {
    AttachedFiles,
    CalibrationModal,
    CytomineDescription,
    CytomineProperties,
    CytomineTags,
    ImageMetadataModal,
    ImageStatus,
    ImageThumbnail,
    MagnificationModal,
    RenameModal,
    SimpleAddToImageGroupModal,
  },
  props: {
    image: {type: Object},
    excludedProperties: {type: Array, default: () => []},
    editable: {type: Boolean, default: false}
  },
  data() {
    return {
      isRenameModalActive: false,
      isCalibrationModalActive: false,
      isMagnificationModalActive: false,
      isMetadataModalActive: false,
      isAddToImageGroupModalActive: false,
      loading: true,
      error: false,
      imageGroupLinks: [],
      properties: [],
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    currentAccount: get('currentUser/account'),
    configUI: get('currentProject/configUI'),
    project: get('currentProject/project'),
    shortTermToken: get('currentUser/shortTermToken'),
    blindMode() {
      return ((this.project || {}).blindMode) || false;
    },
    canDownloadImages() {
      // Virtual images (null path) cannot be downloaded.
      return this.image.path !== null && (
        this.canManageProject ||
        ((this.project || {}).areImagesDownloadable) || false
      );
    },
    canManageProject() {
      return this.$store.getters['currentProject/canManageProject'];
    },
    canEdit() {
      return this.editable && this.$store.getters['currentProject/canEditImage'](this.image);
    },
    canAddToImageGroup() {
      return !this.currentUser.guestByNow && (this.canManageProject || !this.project.isReadOnly);
    },
    imageNameNotif() {
      return this.blindMode ? this.image.blindedName : this.image.instanceFilename;
    },
    vendor() {
      return vendorFromFormat(this.image.contentType);
    },
    /**
     * BLIND   MANAGER    RESULT
     * 0       0          1
     * 0       1          1
     * 1       0          0
     * 1       1          1
     */
    isBlindModeAndContributor() {
      return this.blindMode && !this.canManageProject;
    },
    isInImageGroup() {
      return this.imageGroupLinks.length > 0;
    },
  },
  methods: {
    appendShortTermToken,
    isPropDisplayed(prop) {
      return !this.excludedProperties.includes(prop) && (this.configUI[`project-explore-image-${prop}`] === null || this.configUI[`project-explore-image-${prop}`]);
    },
    download(image) {
      window.location.assign(appendShortTermToken(image.downloadURL, this.shortTermToken), '_blank');
    },
    async cancelReview() {
      let errorLabel = this.image.reviewed ? 'notif-error-unvalidate-review' : 'notif-error-cancel-review';
      try {
        await this.image.stopReview(true);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t(errorLabel)});
      }
    },

    async rename(newName) {
      let oldName = this.image.instanceFilename;
      try {
        // eslint-disable-next-line
        this.image.instanceFilename = newName;
        await this.image.save();
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-rename', {imageName: this.image.instanceFilename})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-rename', {imageName: oldName})
        });
      }
      this.isRenameModalActive = false;
    },

    confirmDeletion() {
      this.$buefy.dialog.confirm({
        title: this.$t('delete-image'),
        message: this.$t('delete-image-confirmation-message', {imageName: this.imageNameNotif}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: this.deleteImage
      });
    },
    async deleteImage() {
      try {
        await ImageInstance.delete(this.image.id);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-deletion', {imageName: this.imageNameNotif})
        });
        this.$emit('delete');

        let updatedProject = this.project.clone();
        updatedProject.numberOfImages--;
        this.$store.dispatch('currentProject/updateProject', updatedProject);
      } catch (err) {
        console.log(err);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-deletion', {imageName: this.imageNameNotif})
        });
      }
    },
    confirmImageGroupLinkDeletion() {
      this.$buefy.dialog.confirm({
        title: this.$t('delete-image-group-link'),
        message: this.$t('delete-image-group-link-confirmation-message', {imageName: this.imageNameNotif}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: this.deleteImageGroupLink
      });
    },
    async deleteImageGroupLink() {
      try {
        // currently, we limit an image instance to be associated to 1 group.
        await this.imageGroupLinks[0].delete();
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-group-link-deletion', {imageName: this.imageNameNotif})
        });
        this.imageGroupLinks.splice(0, 1);
      } catch (err) {
        console.log(err);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-group-link-deletion', {imageName: this.imageNameNotif})
        });
      }
    },
    formatMinutesSeconds(time) {
      return formatMinutesSeconds(time);
    },
    async fetchImageGroupLinks() {
      this.imageGroupLinks = (await ImageGroupImageInstanceCollection.fetchAll({
        filterKey: 'imageinstance',
        filterValue: this.image.id
      })).array;
    },
  },
  async created() {
    try {
      await this.fetchImageGroupLinks();
    } catch (error) {
      console.log(error);
      this.error = true;
    }
    this.loading = false;
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

.format {
  text-transform: uppercase;
}

.vendor-img {
  max-height: 4rem;
  max-width: 12rem;
}

::v-deep .image-thumbnail {
  max-height: 18rem;
  max-width: 50vw;
}
</style>

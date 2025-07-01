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
<form @submit.prevent="setResolution()">
  <cytomine-modal :active="active" :title="$t('calibrate-image')" @close="$emit('update:active', false)">
    <b-message type="is-warning" has-icon icon-size="is-small">
      {{ $t('warning-change-applies-in-project-only') }}
    </b-message>

    <b-field :label="$t('resolution')" :type="{'is-danger': this.errors.has('resolution')}" :message="errors.first('resolution')">
      <b-field :type="{'is-danger': this.errors.has('resolution')}">
        <b-input v-model="calibrationFieldX" name="resolution" v-validate="'required|decimal|positive'" expanded />
        <b-select v-model="calibrationFactorX">
          <option :value="0.001"> {{ $t('nm-per-pixel') }}</option>
          <option :value="1">{{ $t('um-per-pixel') }}</option>
          <option :value="1000">{{ $t('mm-per-pixel') }}</option>
        </b-select>
      </b-field>
    </b-field>

    <b-field v-if="this.image.depth > 1" :label="$t('z-resolution')"
             :type="{'is-danger': this.errors.has('resolution-z')}" :message="errors.first('resolution-z')">
      <b-field :type="{'is-danger': this.errors.has('resolution-z')}">
        <b-input v-model="calibrationFieldZ" name="resolution-z" v-validate="'required|decimal|positive'" expanded />
        <b-select v-model="calibrationFactorZ">
          <option :value="0.001"> {{ $t('nm-per-slice') }}</option>
          <option :value="1">{{ $t('um-per-slice') }}</option>
          <option :value="1000">{{ $t('mm-per-slice') }}</option>
        </b-select>
      </b-field>
    </b-field>

    <b-field v-if="this.image.duration > 1" :label="$t('frame-rate')"
             :type="{'is-danger': this.errors.has('resolution-t')}" :message="errors.first('resolution-t')">
      <b-field :type="{'is-danger': this.errors.has('resolution-t')}">
        <b-input v-model="calibrationFieldT" name="resolution-t" v-validate="'required|decimal|positive'" expanded />
        <p class="control">
          <span class="button is-static">{{$t('frame-per-second')}}</span>
        </p>
      </b-field>
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
import CytomineModal from '@/components/utils/CytomineModal';

export default {
  name: 'calibration-modal',
  props: {
    active: {type: Boolean},
    image: {type: Object}
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      calibrationFieldX: '',
      calibrationFactorX: 1,
      calibrationFieldZ: '',
      calibrationFactorZ: 1,
      calibrationFieldT: ''
    };
  },
  computed: {
    blindMode() {
      return this.$store.state.currentProject.project.blindMode;
    },
  },
  watch: {
    active(val) {
      if (val) {
        this.calibrationFieldX = this.image.physicalSizeX;
        this.calibrationFieldZ = this.image.physicalSizeZ;
        this.calibrationFieldT = this.image.fps;
      }
    }
  },
  methods: {
    async setResolution() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      let imageName = this.blindMode ? this.image.blindedName : this.image.instanceFilename;
      try {
        let updateImage = this.image.clone();
        updateImage.physicalSizeX = Number(this.calibrationFieldX) * this.calibrationFactorX;
        updateImage.physicalSizeY = Number(this.calibrationFieldX) * this.calibrationFactorX;
        if (this.image.depth > 1) {
          updateImage.physicalSizeZ = Number(this.calibrationFieldZ) * this.calibrationFactorZ;
        }
        if (this.image.duration > 1) {
          updateImage.fps = Number(this.calibrationFieldT);
        }
        await updateImage.save();

        this.$emit('setResolution', {
          x: updateImage.physicalSizeX,
          y: updateImage.physicalSizeY,
          z: updateImage.physicalSizeZ,
          t: updateImage.fps
        });

        this.$notify({
          type: 'success',
          text: this.$t('notif-success-image-calibration', {imageName})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-image-calibration', {imageName})
        });
      }
      this.$emit('update:active', false);
    }
  }
};
</script>

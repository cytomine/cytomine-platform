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
<form @submit.prevent="setMagnification()">
  <cytomine-modal :active="active" :title="$t('set-magnification')" @close="$emit('update:active', false)">
    <b-message type="is-warning" has-icon icon-size="is-small">
      {{ $t('warning-change-applies-in-project-only') }}
    </b-message>

    <b-field :label="$t('magnification')" :type="fieldType" :message="errors.first('magnification')">
      <b-input v-model="newMagnification" name="magnification" v-validate="'decimal|positive'" />
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
  name: 'magnification-modal',
  props: {
    active: {type: Boolean},
    image: {type: Object}
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      newMagnification: ''
    };
  },
  computed: {
    blindMode() {
      return this.$store.state.currentProject.project.blindMode;
    },
    fieldType() {
      return {'is-danger': this.errors.has('magnification')};
    }
  },
  watch: {
    active(val) {
      if (val) {
        this.newMagnification = this.image.magnification;
      }
    }
  },
  methods: {
    async setMagnification() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      let imageName = this.blindMode ? this.image.blindedName : this.image.instanceFilename;
      try {
        let updateImage = this.image.clone();
        updateImage.magnification = this.newMagnification;
        await updateImage.save();

        this.$emit('setMagnification', updateImage.magnification);

        this.$notify({
          type: 'success',
          text: this.$t('notif-success-magnification-update', {imageName})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-magnification-update', {imageName})
        });
      }
      this.$emit('update:active', false);
    },
  }
};
</script>


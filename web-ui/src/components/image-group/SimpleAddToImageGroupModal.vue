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
<form @submit.prevent="addToImageGroup()">
  <cytomine-modal :active="active" :title="$t('add-to-image-group')" @close="$emit('update:active', false)">
    <b-loading :is-full-page="false" :active="loading" />
    <template v-if="!loading">
      <b-field :label="$t('image-group')">
        <b-radio v-model="imageGroup" native-value="NEW">
          {{$t('create-image-group')}}
        </b-radio>
      </b-field>
      <template v-if="imageGroup === 'NEW'">
        <b-field :type="{'is-danger': errors.has('name')}" :message="errors.first('name')">
          <b-input v-model="name" name="name" v-validate="'required'" />
        </b-field>
      </template>
      <b-field>
        <b-radio v-model="imageGroup" native-value="EXISTING">
          {{$t('use-existing-image-group')}}
        </b-radio>
      </b-field>

      <template v-if="imageGroup === 'EXISTING'">
        <b-field :type="{'is-danger': errors.has('imageGroup')}" :message="errors.first('imageGroup')">
          <b-select
              v-model="selectedImageGroup"
              :placeholder="$t('select-image-group')"
              name="imageGroup"
              v-validate="'required'"
              expanded
          >
            <option v-for="imageGroup in imageGroups" :value="imageGroup.id" :key="imageGroup.id">
              {{imageGroup.name}}
            </option>
          </b-select>
        </b-field>
      </template>

    </template>



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

import {ImageGroupCollection, ImageGroup, ImageGroupImageInstance} from 'cytomine-client';

export default {
  name: 'add-to-image-group-modal',
  props: {
    active: {type: Boolean},
    image: {type: Object}
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      name: '',
      imageGroup: 'NEW',
      selectedImageGroup: null,
      imageGroups: [],
      loading: true
    };
  },
  computed: {
    blindMode() {
      return this.$store.state.currentProject.project.blindMode;
    },
    imageNameNotif() {
      return this.blindMode ? this.image.blindedName : this.image.instanceFilename;
    },
  },
  watch: {
    active(val) {
      if (val) {
        this.name = '';
        this.imageGroup = 'NEW';
        this.selectedImageGroup = null;
        this.fetchImageGroups(); // TODO: improve
      }
    }
  },
  methods: {
    async addToImageGroup() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      try {
        let idImageGroup;
        if (this.imageGroup === 'NEW') {
          let imageGroup = await new ImageGroup({name: this.name, project: this.image.project}).save();
          idImageGroup = imageGroup.id;
        } else if (this.imageGroup === 'EXISTING') {
          idImageGroup = this.selectedImageGroup;
        }

        let link = await new ImageGroupImageInstance({image: this.image.id, group: idImageGroup}).save();
        this.$emit('addToImageGroup', link);
        this.$notify({type: 'success', text: this.$t('notif-success-image-group-link-creation', {imageName: this.imageNameNotif})});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-image-group-link-creation', {imageName: this.imageNameNotif})});
      }
      this.$emit('update:active', false);
    },
    async fetchImageGroups() {
      try {
        this.imageGroups = (await ImageGroupCollection.fetchAll({
          filterKey: 'project',
          filterValue: this.image.project,
        })).array;
      } catch (error) {
        console.log(error);
        this.error = true;
      }
    }
  },
  async created() {
    await this.fetchImageGroups();
    this.loading = false;
  }
};
</script>


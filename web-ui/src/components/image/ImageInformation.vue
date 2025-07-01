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
<div class="box error" v-if="permissionError || notFoundError">
  <h2> {{ $t(permissionError ? 'access-denied' : 'not-found') }} </h2>
  <p> {{ $t(permissionError ? 'insufficient-permission' : 'not-found-error') }} </p>
</div>
<div class="content-wrapper" v-else>
  <b-loading :is-full-page="false" :active.sync="loading" />
  <div class="box" v-if="!loading">
    <div class="box-title">
      <i18n path="detailed-image-information" tag="h1">
        <router-link place="imageName" :to="`/project/${image.project}/image/${image.id}`">
          <image-name :image="image" />
        </router-link>
      </i18n>
      <router-link place="imageName" :to="`/project/${image.project}/image/${image.id}`">
        <button class="button is-link">
          <span class="icon-text">
            <span class="icon">
              <i class="fas fa-image"></i>
            </span>
            <span class="is-align-content-baseline">
              {{ $t('open-in-viewer')}}
            </span>
          </span>
        </button>
      </router-link>
      <router-link place="imageName" :to="prevRoute">
        <button class="button is-link">
          <span class="icon-text">
            <span class="icon">
              <i class="fas fa-arrow-left"></i>
            </span>
            <span class="is-align-content-baseline">
              {{ $t('go-back')}}
            </span>
          </span>
        </button>
      </router-link>
    </div>

    <image-details
      v-if="image"
      :image="image"
      editable
      @setResolution="resolution => setResolution(resolution)"
      @setMagnification="magnification => image.magnification = magnification"
      @delete="deleteImage()"
    />
  </div>
</div>
</template>

<script>
import ImageName from './ImageName';
import ImageDetails from './ImageDetails';

import {ImageInstance} from 'cytomine-client';
import vendorFromFormat from '@/utils/vendor';

export default {
  name: 'image-information',
  components: {
    ImageName,
    ImageDetails
  },
  data() {
    return {
      loading: true,
      image: null,
      permissionError: false,
      notFoundError: false,
      prevRoute: null
    };
  },
  // https://v3.router.vuejs.org/guide/advanced/navigation-guards.html#in-component-guards
  beforeRouteEnter(to, from, next) {
    next(vm => {
      vm.prevRoute = from;
    });
  },
  computed: {
    idImage() {
      return this.$route.params.idImage;
    }
  },
  watch: {
    idImage() {
      this.loadImage();
    }
  },
  methods: {
    async loadImage() {
      this.loading = true;
      this.permissionError = false;
      this.notFoundError = false;
      try {
        let image = await ImageInstance.fetch(this.idImage);
        image.vendor = vendorFromFormat(image.contentType);
        this.image = image;
      } catch (error) {
        console.log(error);
        if (error.response.status === 403) {
          this.permissionError = true;
        } else {
          this.notFoundError = true;
        }
      }
      this.loading = false;
    },
    deleteImage() {
      this.$router.push(`/project/${this.image.project}`);
    },
    setResolution(resolution) {
      this.image.physicalSizeX = resolution.x;
      this.image.physicalSizeY = resolution.y;
      this.image.physicalSizeZ = resolution.z;
      this.image.fps = resolution.t;
    }
  },
  created() {
    this.loadImage();
  }
};
</script>

<style scoped lang="scss">
  .box-title {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    gap: 10px;

    h1 {
      flex: auto
    }
  }
</style>

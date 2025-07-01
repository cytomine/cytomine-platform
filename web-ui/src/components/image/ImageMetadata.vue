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
    <b-loading :is-full-page="false" :active="loading" />
    <template v-if="!loading">
      <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
        <h2> {{ $t('error') }} </h2>
        <p> {{ $t('unexpected-error-info-message') }} </p>
      </b-message>
      <div v-else>
        <template v-if="showMacro && image && image.macroURL">
          <p :style="styleImagePreview" class="image-preview">
            <img :class="'rotate-' + rotationAngle" :src="appendShortTermToken(image.associatedImageURL('macro', 256, 'jpg'), shortTermToken)" ref="image">
          </p>
          <div class="buttons is-centered are-small">
            <button class="button" @click="rotate(-90)"><i class="fas fa-undo"></i></button>
            <button class="button" @click="rotate(90)"><i class="fas fa-undo mirror"></i></button>
          </div>
        </template>

        <b-input
          v-model="searchString"
          :placeholder="$t('search-placeholder')"
          type="search"
          icon="search"
          size="is-small"
        />
        <ul>
          <li v-for="metadata in filteredMetadata" :key="metadata.id">
            <strong>{{metadata.fullKey}}</strong>: {{metadata.value}}
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script>

import {getWildcardRegexp} from '@/utils/string-utils';
import {get} from '@/utils/store-helpers';
import {appendShortTermToken} from '@/utils/token-utils.js';
import {Cytomine} from 'cytomine-client';

export default {
  name: 'image-metadata',
  props: {
    image: Object,
    showMacro: {type: Boolean, default: true}
  },
  data() {
    return {
      searchString: '',
      rotationAngle: 0,

      loading: true,
      error: false,
      metadata: []
    };
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    filteredMetadata() {
      if (!this.searchString) {
        return this.metadata;
      }
      let regexp = getWildcardRegexp(this.searchString);
      return this.metadata.filter(prop => regexp.test(prop.fullKey) || regexp.test(prop.value));
    },
    styleImagePreview() {
      this.rotationAngle; // to force re-evaluation each time rotationAngle changes
      if (!this.$refs.image) {
        return;
      }

      let reverse = (this.rotationAngle === 90 || this.rotationAngle === 270);
      let width = this.$refs.image.clientWidth + 'px';
      let height = this.$refs.image.clientHeight + 'px';
      return {
        width: reverse ? height : width,
        height: reverse ? width : height
      };
    },
    uriCytomineImageType() {
      return this.image.class.includes('AbstractImage') ? 'abstractimage' : 'imageinstance';
    }
  },
  methods: {
    appendShortTermToken,
    async rotate(val) {
      this.rotationAngle = (this.rotationAngle + val + 360) % 360;
    },
    async fetchMetadata() {
      this.metadata = (await Cytomine.instance.api.get(
        `${this.uriCytomineImageType}/${this.image.id}/metadata.json`
      )).data.collection.map(md => ({fullKey: `${md.namespace}.${md.key}`, ...md}));
      this.metadata.sort((a, b) => a.fullKey.localeCompare(b.fullKey));
    }
  },
  created() {
    try {
      this.fetchMetadata();
      this.loading = false;
    } catch (error) {
      console.log(error);
      this.error = true;
    }
  }
};
</script>

<style lang="scss" scoped>
>>> .modal-card, >>> .modal-card-body {
  width: 800px;
  max-width: 100%;
}

.image-preview {
  margin: auto;
  overflow: hidden;
  margin-bottom: 0.5em;
  text-align: center;
}

img {
  transform-origin: top left;
  max-width: initial;

  &.rotate-90 {
    transform: rotate(90deg) translateY(-100%);
  }

  &.rotate-180 {
    transform: rotate(180deg) translate(-100%,-100%);
  }

  &.rotate-270 {
    transform: rotate(270deg) translateX(-100%);
  }
}

.mirror {
  transform: scale(-1, 1);
}

input {
  max-width: 30em;
  margin-bottom: 1em;
}

li {
  overflow-wrap: break-word;
}
</style>

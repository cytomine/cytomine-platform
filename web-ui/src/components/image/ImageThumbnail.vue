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
  <picture>
    <source :srcset="webpSrc" type="image/webp">
    <source :srcset="jpegSrc" type="image/jpeg">
    <img :src="jpegSrc" alt="" class="image-thumbnail">
  </picture>
</template>

<script>
import {combineImageUrl, splitImageUrl} from '@/utils/image-utils';

export default {
  name: 'ImageThumbnail',
  props: {
    image: {type: Object, default: () => {}},
    macro: {type: Boolean, default: false}, // Ignored if URL is used
    url: {type: String, default: null}, // Cannot be null if image is null or empty
    size: {type: Number},
    extraParameters: {type: Object, default: () => {}}
  },
  computed: {
    jpegSrc() {
      return this.formatSrc('jpg');
    },
    webpSrc() {
      return this.formatSrc('webp');
    },
    splitURL() {
      if (this.url === null) {
        return [];
      }

      let urlParts = splitImageUrl(this.url);
      urlParts.params.set('maxSize', this.size.toString());
      if (this.extraParameters) {
        for (const [key, value] of Object.entries(this.extraParameters)) {
          urlParts.params.set(key, value.toString());
        }
      }
      return urlParts;
    }
  },
  methods: {
    combineURL({host, pathname, format, params}) {
      return combineImageUrl({host, pathname, format, params});
    },
    formatSrc(format) {
      if (this.url !== null) {
        return this.combineURL({format, ...this.splitURL});
      }
      if (this.macro) {
        return this.image.associatedImageURL('macro', this.size, format, this.extraParameters);
      }
      return this.image.thumbURL(this.size, format, this.extraParameters);
    }
  }
};
</script>

<style scoped>

</style>

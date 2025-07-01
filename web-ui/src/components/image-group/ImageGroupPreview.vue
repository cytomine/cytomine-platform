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
  <b-carousel
      autoplay repeat
      :arrow-hover="false" :pause-hover="false" :pause-info="false" :indicator="false"
      :interval="7500"
      animated="fade"
      icon-size="small"
      icon-next="angle-right"
      icon-prev="angle-left"
      class="has-background-light">
    <b-carousel-item v-for="image in imageGroup.imageInstances" :key="`${imageGroup.id}-${image.id}`">
      <div class="has-text-centered">
        <image-thumbnail
            :extra-parameters="{Authorization: 'Bearer ' + shortTermToken}"
            :key="`${imageGroup.id}-${image.thumb}`"
            :size="128"
            :url="image.thumb"
        />
      </div>
    </b-carousel-item>
  </b-carousel>
</template>

<script>
import {get} from '@/utils/store-helpers';

import ImageThumbnail from '@/components/image/ImageThumbnail';

export default {
  name: 'image-group-preview',
  components: {ImageThumbnail},
  props: {
    imageGroup: {type: Object},
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
  }
};
</script>

<style scoped>
>>> .image-thumbnail {
  max-height: 4rem;
  max-width: 10rem;
}

.carousel {
  min-height: auto;
  height: 4rem;
  width: 10rem;
}

>>> .carousel-arrow .icon.has-icons-right {
  color: #3273dc;
  right: 0.5rem;
}

>>> .carousel-arrow .icon.has-icons-left {
  color: #3273dc;
  left: 0.5rem;
}
</style>

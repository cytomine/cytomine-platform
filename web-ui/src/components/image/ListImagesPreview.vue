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
<div class="columns" v-if="images && images.length">
  <div class="column" v-for="image in images" :key="image.image">
    <image-preview :image="image" :blindMode="project.blindMode" />
  </div>
  <div class="column is-narrow vertical-center">
    <router-link class="button" :to="`/project/${this.project.id}/images`">{{$t('button-view-all')}}</router-link>
  </div>
</div>
<div v-else>
  {{$t('no-image-recently-opened')}}
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import {ImageInstanceCollection} from 'cytomine-client';

import ImagePreview from './ImagePreview';

export default {
  name: 'list-images-preview',
  components: {ImagePreview},
  props: {
    project: Object,
    nbRecent: {
      type: Number,
      default: 3
    }
  },
  data() {
    return {
      images: []
    };
  },
  computed: {
    currentUser: get('currentUser/user')
  },
  async created() {
    this.images = await ImageInstanceCollection.fetchLastOpened({
      max: this.nbRecent,
      project: this.project.id,
      user: this.currentUser.id
    });
  }
};
</script>

<style scoped>
.column:not(.is-narrow) {
  max-width: 12.5rem;
  min-width: 12.5rem;
}

.vertical-center {
  display: flex;
  flex-direction: row;
  align-items: center;
}
</style>

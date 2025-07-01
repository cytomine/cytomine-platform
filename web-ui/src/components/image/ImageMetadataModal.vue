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
<cytomine-modal :active="active" :title="$t('image-metadata')" @close="$emit('update:active', false)">
  <image-metadata :image="image" :error="error" :show-macro="true" />
  <template #footer>
    <button class="button" type="button" @click="$emit('update:active', false)">
      {{$t('button-close')}}
    </button>
  </template>
</cytomine-modal>
</template>

<script>
import CytomineModal from '@/components/utils/CytomineModal';
import ImageMetadata from '@/components/image/ImageMetadata.vue';

export default {
  name: 'image-metadata-modal',
  props: {
    active: Boolean,
    image: Object,
    error: {type: Boolean, default: false}
  },
  components: {ImageMetadata, CytomineModal},
};
</script>

<style lang="scss" scoped>
>>> .modal-card, >>> .modal-card-body {
  width: 800px;
  max-width: 100%;
}
</style>

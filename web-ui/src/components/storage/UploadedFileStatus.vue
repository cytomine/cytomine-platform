<!-- Copyright (c) 2009-2019. Authors: see NOTICE file.

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
<div v-if="!iconOnly">
  <span v-if="!isSuccessful || file.nbChildren === undefined" class="tag" :class="tagClass">
    {{$t(labels[file.status])}}
  </span>
  <div v-else class="tags has-addons">
    <span class="tag" :class="tagClass">{{$t(labels[file.status])}}</span>
    <span class="tag is-light">{{$tc("count-files", file.nbChildren + 1, {count: file.nbChildren + 1})}}</span>
  </div>
</div>
<span v-else :class="['icon', textClass]">
  <i :class="['fas', iconClass]" :title="$t(labels[file.status])"></i>
</span>
</template>

<script>
import {UploadedFileStatus} from 'cytomine-client';

export default {
  name: 'uploaded-file-status',
  props: {
    file: Object,
    iconOnly: {type: Boolean, default: false}
  },
  computed: {
    isSuccessful() {
      return this.file.status === UploadedFileStatus.CONVERTED || this.file.status === UploadedFileStatus.DEPLOYED;
    },
    labels() {
      return {
        [UploadedFileStatus.UPLOADED]: 'uploaded',
        [UploadedFileStatus.CONVERTED]: 'converted',
        [UploadedFileStatus.DEPLOYED]: 'deployed',
        [UploadedFileStatus.ERROR_FORMAT]: 'error-format',
        [UploadedFileStatus.ERROR_CONVERSION]: 'error-convert',
        [UploadedFileStatus.EXTRACTED]: 'uncompressed',
        [UploadedFileStatus.ERROR_DEPLOYMENT]: 'error-deployment',
        [UploadedFileStatus.DETECTING_FORMAT]: 'detecting-format',
        [UploadedFileStatus.EXTRACTING_DATA]: 'extracting-data',
        [UploadedFileStatus.ERROR_EXTRACTION]: 'error-extraction',
        [UploadedFileStatus.CONVERTING]: 'converting',
        [UploadedFileStatus.DEPLOYING]: 'deploying',
        50: 'unpacking',
        51: 'error-unpacking',
        60: 'checking-integrity',
        61: 'error-integrity',
        106: 'unpacked'
      };
    },
    // eslint-disable-next-line vue/return-in-computed-property
    result() {
      switch (this.file.status) {
        case UploadedFileStatus.UPLOADED:
        case UploadedFileStatus.DETECTING_FORMAT:
        case UploadedFileStatus.EXTRACTING_DATA:
        case UploadedFileStatus.CONVERTING:
        case UploadedFileStatus.DEPLOYING:
        case UploadedFileStatus.EXTRACTED:
        case 50:
        case 60:
          return 'info';
        case UploadedFileStatus.CONVERTED:
        case UploadedFileStatus.DEPLOYED:
        case 106:
          return 'success';
        case UploadedFileStatus.ERROR_FORMAT:
        case UploadedFileStatus.ERROR_CONVERSION:
        case UploadedFileStatus.ERROR_DEPLOYMENT:
        case UploadedFileStatus.ERROR_EXTRACTION:
        case 51:
        case 61:
          return 'danger';
        default:
          return null;
      }
    },
    tagClass() {
      return 'is-' + this.result;
    },
    textClass() {
      return 'has-text-' + this.result;
    },
    // eslint-disable-next-line vue/return-in-computed-property
    iconClass() {
      switch (this.result) {
        case 'info':
          return 'fa-spinner';
        case 'success':
          return 'fa-check-square';
        case 'danger':
          return 'fa-times-circle';
      }
    }
  }
};
</script>

<style scoped>
.fas {
  font-size: 1.3em;
}
</style>

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
<span class="tag" :class="tagClass">
  {{$t(label)}}
</span>
</template>

<script>
export default {
  props: {
    image: Object
  },
  computed: {
    tagClass() {
      if (this.image.reviewed) {
        return 'is-success';
      }
      if (this.image.inReview) {
        return 'is-info';
      }
      return 'is-light';
    },
    label() {
      if (this.image.reviewed) {
        return 'reviewed';
      }
      if (this.image.inReview) {
        return 'in-review';
      }
      return 'none';
    }
  }
};
</script>

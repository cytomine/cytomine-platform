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
<span v-if="image">
  <template v-if="image.blindedName">
    <span class="blind-indication">[{{this.$t('blinded-name-indication')}}]</span>
    {{image.blindedName}}
    <template v-if="image.instanceFilename && showBothNames">
      <br>
      <span class="true-name">
        {{image.instanceFilename}} <!-- backend will return this prop iff user is allowed to view it (admin or project manager) -->
      </span>
    </template>
  </template>
  <template v-else>{{image.instanceFilename}}</template>
</span>
</template>

<script>
export default {
  name: 'image-name',
  props: {
    image: Object,
    showBothNames: {type: Boolean, default: false}
  }
};
</script>

<style scoped>
.blind-indication {
  font-size: 0.9em;
  text-transform: uppercase;
}

.true-name {
  margin-top: 0.2em;
  font-size: 0.9em;
  color: #888;
}
</style>

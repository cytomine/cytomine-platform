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
<span v-if="term">
  <div class="color-preview" v-if="term.color" :style="{background: term.color}"></div>
  {{term.name}}
  <span v-if="currentAccount.isDeveloper && term.id > 0"> ({{$t('id')}}: {{term.id}})</span>
</span>
</template>

<script>
import {get} from '@/utils/store-helpers';

export default {
  name: 'cytomine-term',
  props: {
    term: Object
  },
  computed: {
    currentAccount: get('currentUser/account'),
  }
};
</script>

<style scoped>
.color-preview {
  width: 1em;
  height: 1em;
  display: inline-block;
  margin-right: 0.25em;
  border-radius: 0.25em;
  box-shadow: 0 0 1px #777;
  position: relative;
  top: 0.2em;
}
</style>

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
<div class="modal-card">
  <header class="modal-card-head">
    <p class="modal-card-title">{{title}}</p>
    <slot name="controls"></slot>
  </header>
  <section class="modal-card-body">
    <slot></slot>
  </section>
  <footer class="modal-card-foot" v-if="footer">
    <slot name="footer">
      <button class="button" type="button" @click="$emit('close')">
        {{$t('button-close')}}
      </button>
    </slot>
  </footer>
</div>
</template>

<script>
export default {
  name: 'cytomine-modal-card',
  props: {
    title: String,
    footer: {type: Boolean, default: true}
  }
};
</script>

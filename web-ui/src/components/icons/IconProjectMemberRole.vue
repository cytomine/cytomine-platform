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
    <i
      class="fas fa-user-cog"
      :class="{disabled: !isManager, editable: editable}"
      :title="$t(label)"
      @click="$emit('toggleManager')"
    >
    </i>
    <i v-if="(!editable && isRepresentative)" class="superscript fas fa-flag"></i>
    <i v-else-if="(editable && isManager)"
       class="superscript fas fa-flag editable"
       :class="{disabled: !isRepresentative}"
       @click="$emit('toggleRepresentative')"
    ></i>
  </div>
</template>

<script>
export default {
  name: 'IconProjectMemberRole',
  props: {
    isManager: {type: Boolean, default: false},
    isRepresentative: {type: Boolean, default: false},
    editable: {type: Boolean, default: false}
  },
  computed: {
    label() {
      if (this.editable) {
        return '';
      }
      if (this.isRepresentative) {
        return 'representative-icon-label';
      }
      if (this.isManager) {
        return 'manager-icon-label';
      }
      return 'contributor-icon-label';
    }
  }
};
</script>

<style scoped>
  .fas.fa-user-cog {
    width: 20px;
    position: relative;
    text-align: center;
  }

  .superscript {
    font-size: 8px;
    position: relative;
    top: -8px;
    left: -8px;
    right: 0;
  }

  .fas.disabled, .disabled .fas {
    color: rgba(0, 0, 0, 0.1);
  }

  .editable:hover {
    color: #2778ad !important;
    cursor: pointer;
  }

  div {
    width: 30px;
    text-align: left;
    display: inline-block;
  }
</style>

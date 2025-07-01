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
<div class="navbar-item has-dropdown is-hoverable">
  <span class="navbar-link" :class="{'is-active': isActive, ...linkClasses}" tabindex="0">
    <i v-if="icon" :class="[iconPack, icon]"></i>
    {{title}}
    <b-tag v-if="tag" :type="tag.type">{{tag.text}}</b-tag>
  </span>
  <div class="navbar-dropdown" :class="classes">
    <slot></slot>
  </div>
</div>
</template>

<script>
export default {
  name: 'navbar-dropdown',
  props: {
    icon: String,
    iconPack: {type: String, default: 'fas'},
    title: String,
    tag: Object,
    classes: Array,
    linkClasses: Object,
    listPathes: Array
  },
  data() {
    return {
      isActive: false
    };
  },
  watch: {
    '$route.path': {
      handler() {
        if (this.listPathes) {
          this.isActive = !!this.listPathes.find(p => this.$route.path.match(p));
        }
        // required so dropdown doesn't remain open on route change.
        document.activeElement.blur();
      },
      immediate: true
    }
  }
};
</script>

<style>
.navbar-item .tag {
  margin-left: 0.5rem;
}
@media screen and (min-width: 1024px) {
  .navbar-item.is-hoverable:hover .navbar-dropdown {
    display: block;
  }
  .navbar-item.is-hoverable:focus-within:not(:hover) .navbar-dropdown {
    display: none;
  }
}
</style>

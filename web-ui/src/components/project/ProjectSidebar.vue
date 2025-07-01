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
<div class="sidebar-wrapper" :class="{expanded: expanded}" ref="sidebar">
  <nav class="sidebar" @click="clickHandler">
    <h1 class="project-name">{{`${$t('project')}: ${project.name}`}}</h1>
    <ul>
      <template v-if="isTabDisplayed('images')">
        <router-link tag="li" :to="`/project/${project.id}/images`">
          <a>
            <i class="far fa-image"></i>
            {{ $t('images') }}
          </a>
        </router-link>
      </template>
      <router-link v-if="isTabDisplayed('image-groups')" tag="li" :to="`/project/${project.id}/image-groups`">
        <a>
          <i class="far fa-images"></i>
          {{ $t('image-groups') }}
        </a>
      </router-link>
      <router-link v-if="isTabDisplayed('annotations')" tag="li" :to="`/project/${project.id}/annotations`">
        <a>
          <i class="far fa-edit"></i>
          {{ $t('annotations') }}
        </a>
      </router-link>
      <router-link v-if="isTabDisplayed('activities')" tag="li" :to="`/project/${project.id}/activity`">
        <a>
          <i class="fas fa-tachometer-alt"></i>
          {{ $t('activity') }}
        </a>
      </router-link>
      <router-link v-if="isTabDisplayed('information')" tag="li" :to="`/project/${project.id}/information`">
        <a>
          <i class="fas fa-info-circle"></i>
          {{ $t('information') }}
        </a>
        </router-link>
    </ul>
    <ul class="bottom-menu">
      <router-link v-if="isTabDisplayed('configuration')" tag="li" :to="`/project/${project.id}/configuration`">
        <a>
          <i class="fas fa-cogs"></i>
          {{ $t('configuration') }}
        </a>
      </router-link>
    </ul>
  </nav>

  <div class="arrow-sidebar" @click="expanded = false">
    <i></i>
  </div>
</div>
</template>

<script>
import {get, sync} from '@/utils/store-helpers';

export default {
  name: 'project-sidebar',
  data() {
    return { };
  },
  computed: {
    project: get('currentProject/project'),
    configUI: get('currentProject/configUI'),
    expanded: sync('currentUser/expandedSidebar')
  },
  methods: {
    clickHandler(event) {
      let el = event.target;
      if (!this.expanded && el.tagName !== 'I' && el.tagName !== 'A') { // if user clicked on icon, navigation purpose => do not expand the nav bar
        this.expanded = true;
      }
    },
    isTabDisplayed(tab) {
      return this.configUI[`project-${tab}-tab`];
    },
    transitionEndHandler() { // led to a change of the size of the content div => need to reload OL map
      this.$eventBus.$emit('updateMapSize');
    }
  },
  mounted() {
    this.$refs.sidebar.addEventListener('transitionend', this.transitionEndHandler);
  },
  beforeDestroy() {
    this.$refs.sidebar.removeEventListener('transitionend', this.transitionEndHandler);
  }
};
</script>

<style lang="scss" scoped>
$background: #444;
$activeBackground: #4f4f4f;
$hoverBackground: #82aad8;
$color: #ccc;
$activeColor: #eee;
$hoverColor: white;
$arrowBackground: #484848;
$border: #383838;
$arrowColor: #888;

.sidebar-wrapper {
  display: flex;
  height: 100%;
  background: $background;
  overflow: hidden;
  transition: width .2s linear;
  width: 4rem;
}

.sidebar-wrapper.expanded {
  width:16rem;
}

.sidebar {
  width: 16rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.project-name {
  padding: 0.8em 0em 0.8em;
  width: 14.5rem !important;
  line-height: 1.8rem;
  text-align: center;
  color: $background;
  font-size: 1rem;
  margin: 0;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  text-align: center;
  font-weight: 600;
}

.sidebar-wrapper.expanded .project-name {
  color: $activeColor !important;
}

ul {
  width: 100%;
}

li {
  height: 3.5rem;
}

.bottom-menu {
  position: absolute;
  bottom: 0;
  left: 0;
}

a {
  position: relative;
  display: block;
  font-size: 0.85rem;
  height: 100%;
  font-weight: 600;
  color: $color;
  text-decoration: none;
  display: flex;
  align-items: center;
}

.fas, .far {
  font-size: 1.25rem;
  width: 4rem;
  text-align: center;
  flex-shrink: 0;
}

li a:hover {
  color: $hoverColor;
  background: $hoverBackground!important;
}

li.is-active a {
  box-shadow: inset 5px 0 0 $hoverBackground;
  color: $activeColor;
  background: $activeBackground;
}

.arrow-sidebar {
  width: 1.5rem;
  border-left: 1px solid $border;
  background: $arrowBackground;
  justify-content: center;
  display:flex;
  align-items: center;
}

.arrow-sidebar i {
  border: solid $arrowColor;
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 0.25rem;
  transform: rotate(135deg);
  margin-left: 0.25rem;
}
</style>

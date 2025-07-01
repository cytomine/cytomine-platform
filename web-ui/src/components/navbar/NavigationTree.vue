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
<div class="navigation-tree-wrapper">
  <div v-for="(project, _, index) in projects" :key="project.id">

    <router-link
      class="navbar-item project-item"
      :to="`/project/${project.id}`"
    >
      {{project.name}}
      <a class="delete is-small" @click.stop.prevent="closeProject(project)"></a>
    </router-link>

    <template v-for="(viewer, idViewer) in project.viewers">
      <router-link
        v-if="nbImages(viewer)"
        class="navbar-item viewer-item"
        :to="viewerPath(project.id, idViewer)"
        :key="idViewer"
        exact
      >
        <div class="viewer-name">
          <span>
            <i class="fas fa-caret-right"></i>
            <template v-if="nbImages(viewer) === 1">
              <image-name :image="Object.values(viewer.images)[0].imageInstance" />
            </template>
            <template v-else>
              {{$t('viewer-group', {nbImages: nbImages(viewer)})}}
            </template>
            {{viewer.name}}
          </span>
          <ul class="viewer-details" v-if="nbImages(viewer) > 1">
            <li v-for="(image, idx) in viewer.images" :key="idx">
              <image-name :image="image.imageInstance" />
            </li>
          </ul>
        </div>
        <a class="delete is-small" @click.stop.prevent="closeViewer(project.id, idViewer)"></a>
      </router-link>
    </template>
    <hr v-if="index < projects.length - 1" class="navbar-divider">
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import ImageName from '@/components/image/ImageName';

export default {
  name: 'navigation-tree',
  components: {ImageName},
  computed: {
    projects: get('projects')
  },
  methods: {
    nbImages(viewer) {
      return Object.keys(viewer.images).length;
    },
    viewerPath(idProject, idViewer) {
      return this.$store.getters[`projects/${idProject}/viewers/${idViewer}/pathViewer`]();
    },
    closeProject(project) {
      let nbViewers = Object.keys(project.viewers).length;
      if (nbViewers) {
        this.$buefy.dialog.confirm({
          title: this.$t('confirm-close-project'),
          message: this.$t(
            'confirm-close-project-message',
            {viewers: this.$tc('count-viewers', nbViewers, {count: nbViewers})}
          ),
          type: 'is-danger',
          confirmText: this.$t('button-confirm'),
          cancelText: this.$t('button-cancel'),
          onConfirm: () => this.doCloseProject(project)
        });
        return;
      }
      this.doCloseProject(project);
    },
    doCloseProject(project) {
      this.$store.unregisterModule(['projects', project.id]);
    },
    closeViewer(idProject, idViewer) {
      this.$store.unregisterModule(['projects', idProject, 'viewers', idViewer]);
    }
  }
};
</script>

<style lang="scss" scoped>
@import '~bulma/sass/utilities/mixins.sass';

.project-item {
  color: #333;
}

.viewer-item {
  padding-left: 1.5rem !important;
  padding-right: 1.5rem !important;
  position: relative;
}

.viewer-item ul {
  color: #888;
  font-weight: normal;
  margin-left: 1rem;
  font-size: 0.85em;
}

.viewer-name {
  padding-right: 2rem;
}

.delete {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}

.navigation-tree-wrapper {
  @include desktop {
    max-height: 70vh;
    overflow: auto;
  }
}
</style>

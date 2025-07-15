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
<div class="box error" v-if="!configUI['project-configuration-tab']">
  <h2> {{ $t('access-denied') }} </h2>
  <p>{{ $t('insufficient-permission') }}</p>
</div>
<div class="content-wrapper" v-else>
  <b-field class="radio-buttons-fullwidth">
    <b-radio-button v-model="activeTab" native-value="general" type="is-link">
      {{$t('general')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="members" type="is-link">
      {{$t('members')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="customUI" type="is-link">
      {{$t('custom-ui')}}
    </b-radio-button>

    <b-radio-button v-model="activeTab" native-value="imageFilters" type="is-link">
      {{$t('image-filters')}}
    </b-radio-button>
  </b-field>

  <div class="box tab-content">
    <keep-alive>
      <component :is="activeComponent" />
    </keep-alive>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import GeneralConfiguration from './configuration-panels/GeneralConfiguration';
import ProjectMembers from './configuration-panels/ProjectMembers';
import CustomUIProject from './configuration-panels/CustomUIProject';
import ProjectImageFilters from './configuration-panels/ProjectImageFilters';

const defaultTab = 'general';

export default {
  name: 'project-configuration',
  data() {
    return {
      activeTab: defaultTab
    };
  },
  computed: {
    configUI: get('currentProject/configUI'),
    queriedTab() {
      return this.$route.query.tab;
    },
    // eslint-disable-next-line vue/return-in-computed-property
    activeComponent() {
      switch (this.activeTab) {
        case 'general':
          return GeneralConfiguration;
        case 'members':
          return ProjectMembers;
        case 'customUI':
          return CustomUIProject;
        case 'imageFilters':
          return ProjectImageFilters;
      }
      throw new Error('Cannot load active tabs ' + this.activeTab);
    }
  },
  watch: {
    queriedTab() {
      this.changeTab();
    },
    activeTab() {
      if (this.activeTab !== defaultTab || this.queriedTab) {
        this.$router.push(`?tab=${this.activeTab}`);
      }
    }
  },
  methods: {
    changeTab() {
      this.activeTab = this.$route.query.tab || defaultTab;
      if (!this.activeComponent) {
        this.activeTab = defaultTab;
      }
    }
  },
  async created() {
    this.changeTab();
  }
};
</script>

<style scoped>
.tab-content {
  position: relative;
  min-height: 20em;
}
</style>

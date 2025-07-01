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
  <h1>{{ $t('terms') }}</h1>
  <div class="ontology-tree-wrapper">
    <div class="header-tree">
      <b-input v-model="searchString" :placeholder="$t('search-placeholder')" size="is-small" expanded />

      <div class="sidebar-tree">
        <div class="visibility">
          <i class="far fa-eye"></i>
        </div>
        <div class="opacity">{{$t('opacity')}}</div>
      </div>
    </div>
    <ontology-tree
      :ontology="ontology"
      :allowSelection="false"
      :searchString="searchString"
      :additionalNodes="additionalNodes"
    >
      <template #custom-sidebar="{term}">
        <div class="sidebar-tree">
          <div class="visibility">
            <b-checkbox
              v-if="term.id"
              size="is-small"
              :value="terms[termsMapping[term.id]].visible"
              @input="toggleTermVisibility(termsMapping[term.id])"
            />

            <b-checkbox v-else size="is-small" v-model="displayNoTerm" />
          </div>

          <div class="opacity">
            <input
              v-if="term.id"
              class="slider is-fullwidth is-small" step="0.05" min="0" max="1" type="range"
              :disabled="!terms[termsMapping[term.id]].visible"
              :value="terms[termsMapping[term.id]].opacity"
              @change="event => changeOpacity(termsMapping[term.id], event)"
              @input="event => changeOpacity(termsMapping[term.id], event)"
            >

            <input
              v-else
              class="slider is-fullwidth is-small" step="0.05" min="0" max="1" type="range"
              v-model="noTermOpacity"
            >
          </div>
        </div>
      </template>
    </ontology-tree>
  </div>
  <div class="has-text-right">
    <button class="button is-small" @click="resetOpacities()">{{$t('button-reset-opacities')}}</button>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import OntologyTree from '@/components/ontology/OntologyTree';

export default {
  name: 'ontology-panel',
  components: {OntologyTree},
  props: {
    index: String
  },
  data() {
    return {
      searchString: ''
    };
  },
  computed: {
    ontology: get('currentProject/ontology'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    terms() {
      return this.imageWrapper.style.terms;
    },
    termsMapping() {
      let mapping = {};
      this.terms.forEach((term, idx) => mapping[term.id] = idx);
      return mapping;
    },
    additionalNodes() {
      return [{id: 0, name: this.$t('no-term')}];
    },
    displayNoTerm: {
      get() {
        return this.imageWrapper.style.displayNoTerm;
      },
      set(value) {
        this.$store.dispatch(this.imageModule + 'setDisplayNoTerm', value);
      }
    },
    noTermOpacity: {
      get() {
        return this.imageWrapper.style.noTermOpacity;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setNoTermOpacity', Number(value));
      }
    },
  },
  methods: {
    toggleTermVisibility(index) {
      this.$store.dispatch(this.imageModule + 'toggleTermVisibility', index);
    },
    changeOpacity(index, event) {
      let opacity = Number(event.target.value);
      this.$store.commit(this.imageModule + 'setTermOpacity', {indexTerm: index, opacity});
    },
    resetOpacities() {
      this.$store.commit(this.imageModule + 'resetTermOpacities');
    }
  }
};
</script>

<style scoped>
.ontology-tree-wrapper {
  max-height: 17em;
  overflow: auto;
  margin-bottom: 0.4em !important;
}


input[type="range"].slider {
  margin: 0;
  padding: 0;
}

.header-tree {
  display: flex;
  justify-content: right;
  position: sticky;
  top: 0;
  z-index: 5;
  padding-bottom: 0.3em;
  background: #f2f2f2;
  border: 2px solid #DBDBDB;
  border-width: 0 0 2px !important;
}

.header-tree .opacity {
  text-align: center;
  text-transform: uppercase;
  font-size: 0.8em;
}

.sidebar-tree {
  padding-right: 0.4em;
  display: flex;
  align-items: center;
}

.visibility {
  width: 2.8em;
  height: 2.1em;
  display: flex;
  justify-content: center;
}

.header-tree .visibility {
  height: auto;
}

.opacity {
  width: 6em;
  display: block;
}

>>> .checkbox .control-label {
  padding: 0 !important;
}

>>> .ontology-tree .sl-vue-tree-node-item, >>> .ontology-tree .no-result {
  line-height: 2;
  font-size: 0.9em;
}
</style>

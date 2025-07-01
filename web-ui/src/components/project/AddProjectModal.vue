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
<form @submit.prevent="createProject(); loading = true">
  <b-loading :active="loading" :is-full-page="false" />

  <template v-if="!loading">
    <cytomine-modal :active="active" :title="$t('create-project')" @close="$emit('update:active', false)">
      <b-field :label="$t('name')" :type="{ 'is-danger': errors.has('name') }" :message="errors.first('name')">
        <b-input v-model="name" name="name" v-validate="'required'" />
      </b-field>

      <b-field :label="$t('ontology')">
        <b-radio v-model="ontology" native-value="NEW">
          {{ $t('create-ontology-for-project') }}
        </b-radio>
      </b-field>
      <b-field>
        <b-radio v-model="ontology" native-value="EXISTING">
          {{ $t('use-existing-ontology') }}
        </b-radio>
      </b-field>
      <b-field>
        <b-radio v-model="ontology" native-value="NO">
          {{ $t('no-ontology') }}
        </b-radio>
      </b-field>

      <template v-if="ontology === 'EXISTING'">
        <b-field :type="{ 'is-danger': errors.has('ontology') }" :message="errors.first('ontology')">
          <b-select
            size="is-small"
            v-model="selectedOntology"
            :placeholder="$t('select-ontology')"
            name="ontology"
            v-validate="'required'"
          >
            <option v-for="ontology in ontologies" :value="ontology.id" :key="ontology.id">
              {{ ontology.name }}
            </option>
          </b-select>
        </b-field>
      </template>

      <template #footer>
        <button class="button" type="button" @click="$emit('update:active', false)">
          {{ $t('button-cancel') }}
        </button>
        <button class="button is-link" :disabled="errors.any()">
          {{ $t('button-save') }}
        </button>
      </template>
    </cytomine-modal>
  </template>
</form>
</template>

<script>
import {Project, Ontology} from 'cytomine-client';

import CytomineModal from '@/components/utils/CytomineModal';

export default {
  name: 'add-project-modal',
  props: {
    active: Boolean,
    ontologies: Array
  },
  components: {CytomineModal},
  $_veeValidate: {validator: 'new'},
  data() {
    return {
      loading: false,
      name: '',
      ontology: 'NEW',
      selectedOntology: null,
    };
  },
  watch: {
    active(val) {
      if (val) {
        this.name = '';
        this.ontology = 'NEW';
        this.selectedOntology = null;
      }
    }
  },
  methods: {
    async createProject() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      try {
        let idOntology;
        if (this.ontology === 'NEW') {
          let ontology = await new Ontology({name: this.name}).save();
          idOntology = ontology.id;
        } else if (this.ontology === 'EXISTING') {
          idOntology = this.selectedOntology;
        }

        let project = await new Project({name: this.name, ontology: idOntology}).save();

        this.loading = false;
        this.$notify({type: 'success', text: this.$t('notif-success-project-creation')});
        this.$emit('update:active', false);
        await this.$router.push(`/project/${project.id}/configuration`);
      } catch (error) {
        if (error.response.status === 409) {
          this.$notify({type: 'error', text: this.$t('notif-error-project-already-exists')});
        } else {
          this.$notify({type: 'error', text: this.$t('notif-error-project-creation')});
        }
      }
    }
  },
};
</script>

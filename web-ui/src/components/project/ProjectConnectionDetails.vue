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
<table class="table">
  <b-loading :is-full-page="false" :active.sync="loading" />
  <tbody v-if="!loading">
    <tr>
      <td>{{$t('operating-system')}}</td>
      <td>{{connection.os}}</td>
    </tr>
    <tr>
      <td>{{$t('browser')}}</td>
      <td class="is-capitalized">
        {{connection.browser}} <span v-if="connection.browserVersion">{{connection.browserVersion}}</span>
      </td>
    </tr>
    <tr v-if="consultations">
      <td>{{$t('image-consultations')}}</td>
      <td>
        <div v-if="consultations.length > 0" class="columns is-multiline">
          <div class="column" v-for="consultation in consultations" :key="consultation.id">
            <image-preview :image="consultation" :blindMode="blindMode">
              <div class="details">
                <p>
                  <strong>{{$t('duration')}}:</strong>
                  {{ consultation.time | duration('humanize') }}
                  <br>
                  <strong>{{$t('annotation-creations')}}:</strong>
                  {{consultation.countCreatedAnnotations || 0}}
                </p>
              </div>
            </image-preview>
          </div>
        </div>
        <em v-else class="has-text-grey">{{$t('no-image-consultation')}}</em>
      </td>
    </tr>
  </tbody>
</table>
</template>

<script>
import {ImageConsultationCollection} from 'cytomine-client';
import ImagePreview from '@/components/image/ImagePreview';

export default {
  name: 'project-connection-details',
  components: {ImagePreview},
  props: {
    connection: Object
  },
  data() {
    return {
      loading: true,
      consultations: null
    };
  },
  computed: {
    blindMode() {
      return this.$store.state.currentProject.project.blindMode;
    }
  },
  async created() {
    try {
      this.consultations = (await ImageConsultationCollection.fetchAll({projectConnection: this.connection.id})).array;
    } catch (error) {
      console.log(error);
      this.$notify({type: 'error', text: this.$t('error-failed-to-fetch-image-consultations')});
    }
    this.loading = false;
  }
};
</script>

<style scoped>
.table {
  background: none;
  position: relative;
}

td:first-child {
  white-space: nowrap;
  font-weight: 600;
}

td:last-child {
  width: 100%;
}

.column {
  min-width: 18em;
  max-width: 18em;
}

.details {
  font-size: 0.85em;
}
</style>

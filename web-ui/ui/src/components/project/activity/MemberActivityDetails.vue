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
<div class="user-activity-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <div class="content-wrapper" v-if="!loading">
    <nav class="breadcrumb" aria-label="breadcrumbs">
      <ul>
        <li><router-link :to="`/project/${project.id}`">{{project.name}}</router-link></li>
        <li><router-link :to="`/project/${project.id}/activity?tab=members`">{{$t('members-activity')}}</router-link></li>
        <li class="is-active"><a href="#" aria-current="page">{{$t('activity-of-user', {username: user.fullName})}}</a></li>
      </ul>
    </nav>

    <div class="box">
      <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
        <h2> {{ $t('error') }} </h2>
        <p> {{ $t('unexpected-error-info-message') }} </p>
      </b-message>

      <template v-else>
        <p class="last-update is-size-7 has-text-grey">
          {{$t('data-last-updated-on', {time: $options.filters.moment(lastUpdate, 'LTS')})}}
        </p>

        <h1>{{$t('activity-of-user', {username: user.fullName})}}</h1>

        <ul>
          <li><strong>{{$t('registration-date')}}:</strong> {{Number(user.created) | moment('ll LTS')}}</li>
          <li><strong>{{$t('first-project-connection')}}:</strong> {{Number(resumeActivity.firstConnection) | moment('ll LTS')}}</li>
          <li><strong>{{$t('last-project-connection')}}:</strong> {{Number(resumeActivity.lastConnection) | moment('ll LTS')}}</li>
        </ul>

        <hr>

        <div class="level">
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">{{$t('project-connections')}}</p>
              <p class="title">{{resumeActivity.totalConnections}}</p>
            </div>
          </div>
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">{{$t('image-consultations')}}</p>
              <p class="title">{{resumeActivity.totalConsultations}}</p>
            </div>
          </div>
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">{{$t('annotation-selections')}}</p>
              <p class="title">{{resumeActivity.totalAnnotationSelections}}</p>
            </div>
          </div>
          <div class="level-item has-text-centered">
            <div>
              <p class="heading">{{$t('annotation-creations')}}</p>
              <p class="title">{{resumeActivity.totalAnnotations}}</p>
            </div>
          </div>
        </div>

        <hr>

        <h2>{{$t('detailed-project-connections')}}</h2>

        <b-table
          :data="connections.array"
          default-sort="created"
          default-sort-direction="desc"
          :paginated="true"
          :per-page="connectionsPerPage"
          pagination-size="is-small"
          detailed detail-key="id"
        >
          <template #default="{row: connection}">
            <b-table-column :label="$t('date')" field="created" sortable>
              {{ Number(connection.created) | moment('ll LT') }}
            </b-table-column>

            <b-table-column :label="$t('duration')" field="time" sortable>
              {{ connection.time | duration('humanize') }}
              <span class="tag is-success" v-if="connection.online">{{$t('ongoing')}}</span>
            </b-table-column>

            <b-table-column :label="$t('number-viewed-images')" field="countViewedImages" centered sortable>
              {{ connection.countViewedImages }}
            </b-table-column>

            <b-table-column :label="$t('number-annotation-creations')" field="countCreatedAnnotations" centered sortable>
              {{ connection.countCreatedAnnotations }}
            </b-table-column>
          </template>

          <template #empty>
            <div class="content has-text-grey has-text-centered">
              <p>{{$t('no-project-connection')}}</p>
            </div>
          </template>

          <template #detail="{row: connection}">
            <project-connection-details :connection="connection" :key="connection.id" />
          </template>

          <template #footer>
            <p class="has-text-centered">
              <a class="button is-link" @click="downloadConnections(connections)">
                {{$t('button-export-as-csv')}}
              </a>
            </p>
          </template>

          <template #bottom-left>
            <b-select v-model="connectionsPerPage" size="is-small">
              <option value="10">{{$t('count-per-page', {count: 10})}}</option>
              <option value="25">{{$t('count-per-page', {count: 25})}}</option>
              <option value="50">{{$t('count-per-page', {count: 50})}}</option>
              <option value="100">{{$t('count-per-page', {count: 100})}}</option>
            </b-select>
          </template>
        </b-table>

        <hr>

        <h2>{{$t('connections-chart')}}</h2>

        <div class="columns">
          <div class="column is-narrow">
            <b-field :label="$t('by')" horizontal>
              <b-select size="is-small" v-model="period">
                <option value="hour">{{$t('hour')}}</option>
                <option value="day">{{$t('day')}}</option>
                <option value="week">{{$t('week')}}</option>
              </b-select>
            </b-field>
          </div>
          <div class="column is-narrow">
            <b-field :label="$t('from')" horizontal>
              <cytomine-datepicker v-model="startDate" :resetButton="false" :maxDate="endDate || new Date()" />
            </b-field>
          </div>
          <div class="column is-narrow">
            <b-field :label="$t('to')" horizontal>
              <cytomine-datepicker v-model="endDate" :minDate="startDate" :maxDate="new Date()" />
            </b-field>
          </div>
        </div>

        <div class="chart-container">
          <last-connections-chart
            css-classes="chart"
            :project="project.id"
            :user="idUser"
            :period="period"
            :startDate="startDate.getTime()"
            :endDate="endDate ? endDate.setHours(23, 59, 59, 999) : null"
            :showDates="true"
            :revision="chartRevision"
          />
        </div>

        <hr>

        <h2>{{$t('detailed-image-consultations')}}</h2>

        <b-table :data="consultations.array" :paginated="true" :per-page="consultationsPerPage" pagination-size="is-small">
          <template #default="{row: consultation}">
            <b-table-column :label="$t('overview')" field="created">
              <router-link :to="`/project/${project.id}/image/${consultation.image}`">
                <image-thumbnail :url="consultation.imageThumb" :size="128" :key="consultation.imageThumb" :extra-parameters="{Authorization: 'Bearer ' + shortTermToken }"/>
              </router-link>
            </b-table-column>

            <b-table-column :label="$t('image')" field="imageName" sortable>
              <router-link :to="`/project/${project.id}/image/${consultation.image}`">
                {{ consultation.imageName }}
              </router-link>
            </b-table-column>

            <b-table-column :label="$t('duration')" field="time" sortable>
              {{ consultation.time | duration('humanize') }}
            </b-table-column>

            <b-table-column :label="$t('number-consultations')" field="frequency" centered sortable>
              {{ consultation.frequency }}
            </b-table-column>

            <b-table-column :label="$t('first-consultation')" field="first" centered sortable>
              {{ Number(consultation.first) | moment('ll LT')}}
            </b-table-column>

            <b-table-column :label="$t('last-consultation')" field="last" centered sortable>
              {{ Number(consultation.last) | moment('ll LT')}}
            </b-table-column>

            <b-table-column :label="$t('number-annotation-creations')" field="countCreatedAnnotations" centered sortable>
              {{ consultation.countCreatedAnnotations }}
            </b-table-column>
          </template>

          <template #empty>
            <div class="content has-text-grey has-text-centered">
              <p>{{$t('no-image-consultation')}}</p>
            </div>
          </template>

          <template #footer>
            <p class="has-text-centered">
              <a class="button is-link" @click="downloadConnections(consultations)">
                {{$t('button-export-as-csv')}}
              </a>
            </p>
          </template>

          <template #bottom-left>
            <b-select v-model="consultationsPerPage" size="is-small">
              <option value="10">{{$t('count-per-page', {count: 10})}}</option>
              <option value="25">{{$t('count-per-page', {count: 25})}}</option>
              <option value="50">{{$t('count-per-page', {count: 50})}}</option>
              <option value="100">{{$t('count-per-page', {count: 100})}}</option>
            </b-select>
          </template>
        </b-table>
      </template>
    </div>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import {User, ProjectConnectionCollection, ImageConsultationCollection} from 'cytomine-client';

import CytomineDatepicker from '@/components/form/CytomineDatepicker';
import ProjectConnectionDetails from '@/components/project/ProjectConnectionDetails';
import LastConnectionsChart from '@/components/charts/LastConnectionsChart.js';

import constants from '@/utils/constants.js';

import moment from 'moment';
import ImageThumbnail from '@/components/image/ImageThumbnail';
import {appendShortTermToken} from '@/utils/token-utils.js';


export default {
  name: 'member-activity-details',
  components: {
    ImageThumbnail,
    CytomineDatepicker,
    ProjectConnectionDetails,
    LastConnectionsChart
  },
  data() {
    return {
      loading: true,
      error: false,
      user: null,
      resumeActivity: null,

      connections: null,
      connectionsPerPage: 10,

      consultations: null,
      consultationsPerPage: 10,

      lastUpdate: null,
      timeout: null,

      chartRevision: 0,
      period: 'day',
      startDate: moment().subtract(1, 'month').toDate(),
      endDate: null
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    idUser() {
      return Number(this.$route.params.idUser);
    },
    project: get('currentProject/project'),
    shortTermToken: get('currentUser/shortTermToken')
  },
  methods: {
    appendShortTermToken,
    async fetchData() {
      try {
        await Promise.all([
          this.fetchUser(),
          this.fetchResumeActivity(),
          this.fetchConnections(),
          this.fetchConsultations()
        ]);
      } catch (error) {
        console.log(error);
        this.error = true;
        return;
      }

      this.chartRevision++;
      this.lastUpdate = new Date();
      clearTimeout(this.timeout);
      this.timeout = setTimeout(this.fetchData, constants.MEMBERS_ACTIVITY_REFRESH_INTERVAL);
    },
    async fetchUser() {
      await this.user.fetch();
    },
    async fetchResumeActivity() {
      this.resumeActivity = await this.user.fetchResumeActivity(this.project.id);
    },
    async fetchConnections() {
      this.connections = await ProjectConnectionCollection.fetchAll({project: this.project.id, user: this.idUser});
    },
    async fetchConsultations() {
      this.consultations = await ImageConsultationCollection.fetchAll({project: this.project.id, user: this.idUser, resume: true});
    },
    downloadConnections(connections) {
      window.location.assign(appendShortTermToken(connections.downloadURL, this.shortTermToken), '_blank');
    }
  },
  async created() {
    this.user = new User({id: this.idUser});
    await this.fetchData();
    this.loading = false;
  },
  destroyed() {
    clearTimeout(this.timeout);
  }
};
</script>

<style scoped>
h1 {
  text-transform: initial;
  letter-spacing: initial;
  font-weight: 400;
  font-size: 1.8rem;
  margin-bottom: 0.7rem;
}

li {
  margin-bottom: 0.5em;
}

.image-thumbnail {
  max-height: 4rem;
  max-width: 10rem;
}

.box {
  position: relative;
}

.last-update {
  position: absolute;
  top: 1.5em;
  right: 2em;
}

.tag {
  margin-left: 0.5em;
}

.chart-container {
  margin-top: 2em;
  height: 18em;
  position: relative;
}
</style>

<style lang="scss">
.user-activity-wrapper {
  .table-wrapper {
    margin-bottom: 0;
  }

  .chart {
    position: absolute;
    width: 100%;
    height: 100%;
  }

  .field.is-horizontal {
    align-items: center;
  }

  .field-label {
    margin-right: 1em !important;
    padding-top: 0 !important;
  }
}
</style>

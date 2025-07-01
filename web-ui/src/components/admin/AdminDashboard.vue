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
<div class="admin-dashboard-wrapper">
  <b-loading :is-full-page="false" :active="loading" />
  <template v-if="!loading">
    <h2>{{$t('currently')}}</h2>
    <b-message v-if="!currentStats" type="is-danger" has-icon icon-size="is-small">
      {{$t('failed-fetch-current-stats')}}
    </b-message>
    <template v-else>
      <div class="level">
        <div class="level-item has-text-centered">
          <div>
          <p class="heading">{{$t('online-users')}}</p>
          <p class="title">{{currentStats.users}}</p>
          </div>
        </div>
        <div class="level-item has-text-centered">
          <div>
          <p class="heading">{{$t('active-projects')}}</p>
          <p class="title">{{currentStats.projects}}</p>
          </div>
        </div>
        <div class="level-item has-text-centered">
          <div>
          <p class="heading">{{$t('used-storage-space')}}</p>
          <p class="title">
            <template v-if="storageStats">{{Math.round(storageStats.usedP * 100)}}%</template>
            <template v-else>?</template>
          </p>
          </div>
        </div>
      </div>

      <p v-if="currentStats.mostActiveProject">
        <strong>{{$t('most-active-project')}}</strong>:
        <router-link :to="`/project/${currentStats.mostActiveProject.project.id}`">
          {{currentStats.mostActiveProject.project.name}}
        </router-link>
        ({{$tc("count-active-users", currentStats.mostActiveProject.users, {count: currentStats.mostActiveProject.users})}})
      </p>
    </template>

    <hr>

    <h2>{{$t('total')}}</h2>
    <b-message v-if="!totalCounts" type="is-danger" has-icon icon-size="is-small">
      {{$t('failed-fetch-total-counts')}}
    </b-message>
    <div v-else class="columns">
      <div class="column">
        <table class="table is-fullwidth">
          <tbody>
            <tr>
              <td>{{totalCounts.users}}</td>
              <td>{{$t('users')}}</td>
            </tr>
            <tr>
              <td>{{totalCounts.projects}}</td>
              <td>{{$t('projects')}}</td>
            </tr>
            <tr>
              <td>{{totalCounts.images}}</td>
              <td>{{$t('images-in-projects')}}</td>
            </tr>
            <tr>
              <td>{{totalCounts.abstractImages}}</td>
              <td>{{$t('uploaded-images')}}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="column">
        <table class="table is-fullwidth">
          <tbody>
            <tr>
              <td>{{totalCounts.ontologies}}</td>
              <td>{{$t('ontologies')}}</td>
            </tr>
            <tr>
              <td>{{totalCounts.terms}}</td>
              <td>{{$t('terms')}}</td>
            </tr>
            <tr>
              <td>{{totalCounts.userAnnotations}}</td>
              <td>{{$t('user-annotations')}}</td>
            </tr>
            <tr>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <hr>
    <h2>{{$t('last-connections')}}</h2>

    <div class="is-flex">
      <strong class="has-margin-right">{{$t('period')}}</strong>
      <b-select v-model="selectedChartOption">
        <option v-for="option in chartOptions" :key="option.label" :value="option">
          {{option.label}}
        </option>
      </b-select>
    </div>

    <div class="chart-container">
      <last-connections-chart
        css-classes="chart"
        :startDate="selectedChartOption.startDate"
        :period="selectedChartOption.period"
      />
    </div>

  </template>
</div>
</template>

<script>
import {Cytomine} from 'cytomine-client';
import LastConnectionsChart from '@/components/charts/LastConnectionsChart.js';
import moment from 'moment';

export default {
  name: 'admin-dashboard',
  components: {LastConnectionsChart},
  data() {
    return {
      loading: true,
      currentStats: null,
      totalCounts: null,
      storageStats: null,
      chartOptions: [],
      selectedChartOption: null
    };
  },
  methods: {
    async fetchCurrentStats() {
      this.currentStats = null; // reset to null so that we know if an error occurred (if so, variable remains null)
      this.currentStats = await Cytomine.instance.fetchCurrentStats();
    },
    async fetchTotalCounts() {
      this.totalCounts = null;
      this.totalCounts = await Cytomine.instance.fetchTotalCounts();
    },
    async fetchStorageStats() {
      this.storageStats = null;
      this.storageStats = await Cytomine.instance.fetchStorageStats();
    }
  },
  created() {
    let chartOptions = [
      {interval: 'day', period: 'hour'},
      {interval: 'week', period: 'day'},
      {interval: 'month', period: 'week'},
      {interval: 'year', period: 'week'}
    ];
    this.chartOptions = chartOptions.map(option => {
      let startDate = moment()
        .add(1, option.period + 's')
        .subtract(1, option.interval + 's')
        .startOf(option.period)
        .valueOf();
      return {label: this.$t(`last-${option.interval}`), period: option.period, startDate};
    });
    this.selectedChartOption = this.chartOptions[0];
  },
  async activated() {
    this.loading = true;
    await Promise.all([
      this.fetchCurrentStats(),
      this.fetchTotalCounts(),
      this.fetchStorageStats()
    ].map(p => p.catch(e => console.log(e)))); // ignore errors (handled in template) and ensure all promises finish, even if some errors occur in the process
    this.loading = false;
  }
};
</script>

<style scoped>
.admin-dashboard-wrapper {
  position: relative;
  min-height: 10em;
}

td:first-child {
  font-weight: 600;
  text-align: right;
  padding-right: 1em;
}

.is-flex {
  align-items: center;
}

.has-margin-right {
  margin-right: 1em;
}

.chart-container {
  margin-top: 2em;
  height: 20em;
  position: relative;
}

>>> .chart {
  position: absolute;
  height: 100%;
  width: 100%;
}
</style>

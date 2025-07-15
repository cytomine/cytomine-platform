/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import {Bar} from 'vue-chartjs';

export default {
  name: 'activity-overview-chart',
  extends: Bar,
  props: {
    project: Object,
    startDate: Number,
    endDate: Number,
    daysRange: Number
  },
  data() {
    return {
      projectConnections: [],
      imageConsultations: [],
      annotationSelections: [],
      chartData: null
    };
  },
  computed: {
    locale() {
      return this.$i18n.locale;
    },
    queryParams() {
      return {
        daysRange: this.daysRange,
        startDate: this.startDate,
        endDate: this.endDate,
        accumulate: false
      };
    }
  },
  watch: {
    async queryParams() {
      await this.fetchData();
      this.updateChart();
    },
    locale() {
      this.updateLabels();
      this.updateChart();
    }
  },
  methods: {
    async fetchProjectConnections() {
      this.projectConnections = await this.project.fetchConnectionsEvolution(this.queryParams);
    },
    async fetchImageConsultations() {
      this.imageConsultations = await this.project.fetchImageConsultationsEvolution(this.queryParams);
    },
    async fetchAnnotationSelections() {
      this.annotationSelections = await this.project.fetchAnnotationActionsEvolution({action: 'select', ...this.queryParams});
    },
    async fetchData() {
      await Promise.all([
        this.fetchProjectConnections(),
        this.fetchImageConsultations(),
        this.fetchAnnotationSelections()
      ]);

      this.chartData.datasets[0].data = this.projectConnections.map(item => item.size);
      this.chartData.datasets[1].data = this.imageConsultations.map(item => item.size);
      this.chartData.datasets[2].data = this.annotationSelections.map(item => item.size);
      this.updateLabels();
    },
    updateLabels() {
      this.chartData.labels = this.projectConnections.map(item => {
        let moment = this.$options.filters.moment;
        return this.daysRange === 1 ? moment(Number(item.date), 'll')
          : [moment(Number(item.date), 'll') + ' - ',  moment(Number(item.endDate), 'll')];
      });
    },
    async updateChart() {
      this.$data._chart.update();
    }
  },
  async mounted() {
    this.chartData = {
      labels: [],
      datasets: [
        {
          label: this.$t('project-connections'),
          data: [],
          backgroundColor: '#4480c4',
          borderWidth: 0
        },
        {
          label: this.$t('image-consultations'),
          data: [],
          backgroundColor: '#f2418e',
          borderWidth: 0
        },
        {
          label: this.$t('annotation-selections'),
          data: [],
          backgroundColor: '#ffa500',
          borderWidth: 0
        }
      ]
    };

    await this.fetchData();

    this.renderChart(this.chartData, {
      maintainAspectRatio: false,
      scales: {
        yAxes: [{
          ticks: {
            min: 0,
          }
        }],
        xAxes: [{
          categoryPercentage: 0.6,
          gridLines: {
            display: false // TODO when possible in chart.js: display all grid lines, even when ticks hidden (see https://stackoverflow.com/questions/44361991/show-only-nth-tick-line-on-x-axis-for-chart-js-diagram and https://stackoverflow.com/questions/43604396/how-do-i-persist-the-gridlines-for-ticks-that-are-not-displayed-chart-js and )
          }
        }]
      }
    });
  }
};

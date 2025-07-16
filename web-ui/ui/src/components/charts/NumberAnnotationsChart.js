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

import {AnnotationType} from 'cytomine-client';
export default {
  name: 'number-annotations-chart',
  extends: Bar,
  props: {
    project: Object,
    term: Number,
    startDate: Number,
    endDate: Number,
    daysRange: Number
  },
  data() {
    return {
      annotationsEvolution: {
        [AnnotationType.USER]: [],
        [AnnotationType.REVIEWED]: []
      },
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
        accumulate: false,
        reverseOrder: false,
        term: this.term
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
    async fetchAnnotationsEvolution(type) {
      this.annotationsEvolution[type] = await this.project.fetchAnnotationsEvolution({annotationType: type, ...this.queryParams});
    },
    async fetchData() {
      await Promise.all([
        this.fetchAnnotationsEvolution(AnnotationType.USER),
        this.fetchAnnotationsEvolution(AnnotationType.REVIEWED)
      ]);

      this.chartData.datasets[0].data = this.annotationsEvolution[AnnotationType.USER].map(item => item.size);
      this.chartData.datasets[1].data = this.annotationsEvolution[AnnotationType.REVIEWED].map(item => item.size);
      this.updateLabels();
    },
    updateLabels() {
      this.chartData.labels = this.annotationsEvolution[AnnotationType.USER].map(item => {
        let moment = this.$options.filters.moment;
        return this.daysRange === 1 ? moment(Number(item.date), 'll')
          : [moment(Number(item.date), 'll') + ' - ',  moment(Number(item.endDate), 'll')];
      });
    },
    updateChart() {
      this.$data._chart.update();
    }
  },
  async mounted() {
    this.chartData = {
      labels: [],
      datasets: [
        {
          label: this.$t('user-annotations'),
          data: [],
          backgroundColor: '#4480c4',
          borderWidth: 0
        },
        {
          label: this.$t('reviewed-annotations'),
          data: [],
          backgroundColor: '#42ce77',
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
            min: 0
          }
        }],
        xAxes: [{
          categoryPercentage: 0.6,
          gridLines: {
            display: false
          }
        }]
      }
    });
  }
};

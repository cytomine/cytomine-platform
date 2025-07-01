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

import {HorizontalBar} from 'vue-chartjs';
import ChartDataLabels from 'chartjs-plugin-datalabels';

import {asArray as hexToRgb} from 'ol/color';

const defaultColor = '#eee';

export default {
  name: 'annotated-images-by-term-chart',
  extends: HorizontalBar,
  props: {
    project: Object,
    startDate: Number,
    endDate: Number
  },
  data() {
    return {
      chartData: null
    };
  },
  computed: {
    queryParams() {
      return {
        startDate: this.startDate,
        endDate: this.endDate
      };
    }
  },
  watch: {
    async queryParams() {
      this.doRenderChart();
    }
  },
  methods: {
    async doRenderChart() {
      let terms = await this.project.fetchStatsAnnotatedImagesByTerm(this.queryParams);
      let data = terms.map(term => term.value);
      let borderColors = terms.map(term => {
        let [r, g, b] = hexToRgb(term.color || defaultColor);
        const factor = 0.8;
        return `rgba(${r * factor}, ${g * factor}, ${b * factor}, 1)`;
      });

      this.$emit('nbElems', data.length);

      this.renderChart({
        labels: terms.map(term => term.key || this.$t('no-term')),
        datasets: [
          {
            data,
            backgroundColor: terms.map(term => term.color || defaultColor),
            borderColor: borderColors,
            borderWidth: 1,
            hoverBorderColor: borderColors,
          }
        ]
      }, {
        maintainAspectRatio: false,
        legend: {display: false},
        scales: {
          xAxes: [{
            ticks: {
              min: 0,
              suggestedMax: Math.round(Math.max(...data) * 1.2) + 1
            }
          }],
          yAxes: [{
            gridLines: {
              display: false
            },
            categoryPercentage: 0.6
          }]
        }
      });
    }
  },
  async mounted() {
    this.addPlugin(ChartDataLabels);
    this.doRenderChart();
  }
};

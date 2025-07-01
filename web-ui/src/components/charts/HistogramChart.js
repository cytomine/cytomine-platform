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

import {Line} from 'vue-chartjs';
import _ from 'lodash';

export default {
  name: 'histogram-chart',
  extends: Line,
  props: {
    logScale: Boolean,
    color: String,

    histogram: Array,
    nBins: Number,
    firstBin: Number,
    lastBin: Number,

    defaultBounds: Object,
    imageBounds: Object,
    currentBounds: Object,
    gamma: Number,
    inverted: Boolean,
  },
  data() {
    return {
      chartData: null,
    };
  },
  computed: {
    extendedHistogram() {
      const missingLeft = new Array(this.firstBin).fill(0);
      const missingRight = new Array(this.nBins - this.lastBin - 1).fill(0);
      return missingLeft.concat(this.histogram).concat(missingRight);
    },
    logHistogram() {
      return this.extendedHistogram.map(v => Math.log(v));
    },
    scaledHistogram() {
      return this.logScale ? this.logHistogram : this.extendedHistogram;
    },

    binSize() {
      return (this.defaultBounds.max + 1) / this.nBins;
    },
    integerBinSize() {
      return Math.round(this.binSize);
    },
    labels() {
      return this.extendedHistogram.map((_, idx) => Math.round(idx * this.binSize));
    },
    imageBoundsLabel() {
      return {
        min: this.findLabel(this.imageBounds.min),
        max: this.findLabel(this.imageBounds.max)
      };
    },
    currentBoundsLabel() {
      return {
        min: Math.max(this.defaultBounds.min, this.findLabel(this.currentBounds.min)),
        max: Math.min(this.defaultBounds.max, this.findLabel(this.currentBounds.max))
      };
    },
    boundsLabel() {
      return {
        min: Math.min(this.imageBoundsLabel.min, this.currentBoundsLabel.min),
        max: Math.max(this.imageBoundsLabel.max, this.currentBoundsLabel.max)
      };
    },

    currentLabels() {
      return this.labels.filter(label =>
        label >= this.currentBoundsLabel.min && label <= this.currentBoundsLabel.max
      );
    },

    systemResponse() {
      if (this.currentLabels.length === 1) {
        return [
          {x: this.currentLabels[0], y: 0},
          {x: this.currentLabels[0], y: 255}
        ];
      }

      let nbPoints = 100;
      let step = (this.currentLabels.length - 1) / nbPoints;
      let range = _.range(0, this.currentLabels.length - 1 + step, step);

      let ymin = (this.inverted) ? 1 : 0;
      let ymax = (this.inverted) ? 0 : 1;
      let m = (ymin - ymax) / (this.currentBoundsLabel.min - this.currentBoundsLabel.max);
      let p = ymin - m * this.currentBoundsLabel.min;
      let gamma = (this.inverted) ? 1 / this.gamma : this.gamma;
      let response = range.map(idx => {
        let label = this.currentLabels[Math.round(idx)];
        return {
          x: label,
          y: Math.pow((m * label + p), gamma) * 255.0
        };
      });
      return _.uniqBy(response, 'x');
    },

    backgroundColor() {
      if (this.color !== null) {
        return this.color;
      }
      return '#fff';
    },
    datasets() {
      return [
        {
          data: this.systemResponse,
          fill: false,
          pointRadius: 0,
          pointHoverRadius: 0,
          borderColor: '#333',
          borderWidth: 1,
          type: 'line',
          order: 2,
          cubicInterpolationMode: 'monotone',
          yAxisID: 'yResponse'
        },
        {
          data: this.scaledHistogram,
          backgroundColor: this.backgroundColor,
          pointRadius: 1,
          order: 1,
          yAxisID: 'yHistogram'
        },
      ];
    }
  },
  watch: {
    currentBoundsLabel() {
      this.doRenderChart();
    },
    logScale() {
      this.doRenderChart();
    },
    systemResponse() {
      this.doRenderChart();
    },
    backgroundColor() {
      this.doRenderChart();
    }
  },
  methods: {
    findBin(value) {
      return Math.floor(value / this.binSize);
    },
    findLabel(value) {
      return Math.floor(this.findBin(value) * this.binSize);
    },
    doRenderChart() {
      try {
        this.renderChart({
          labels: this.labels,
          datasets: this.datasets,

          // Additional data for tooltips
          logScale: this.logScale,
          binSize: this.integerBinSize,
        }, {
          tooltips: {
            filter: function (tooltipItem) {
              return tooltipItem.datasetIndex !== 0;
            },
            callbacks: {
              label: function (tooltipItem, data) {
                if (data.logScale) {
                  return Math.round(Math.exp(tooltipItem.value));
                }
                return tooltipItem.value;
              },
              title: function (tooltipItems, data) {
                if (tooltipItems.length > 0) {
                  let left = Number(tooltipItems[0].label);
                  if (data.binSize === 1) {
                    return left;
                  }
                  let right = left + data.binSize;
                  return `[${left} - ${right}[`;
                }
              }
            }
          },
          maintainAspectRatio: false,
          responsive: true,
          title: {
            display: false
          },
          legend: {
            display: false
          },
          animation: {
            duration: 0,
          },
          scales: {
            xAxes: [{
              display: true,
              scaleLabel: {
                display: false
              },
              ticks: {
                ...this.boundsLabel,
                fontSize: 10,
              }
            }],
            yAxes: [{
              id: 'yHistogram',
              display: false,
            }, {
              id: 'yResponse',
              display: false
            }
            ]
          },
        });
      } catch (error) {
        console.log(error);
        this.$emit('error', true);
      }
    }
  },
  async mounted() {
    this.doRenderChart();
  }
};

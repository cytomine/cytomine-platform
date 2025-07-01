import {Line} from 'vue-chartjs';
import ChartZoom from 'chartjs-plugin-zoom';

export default {
  name: 'annotation-profile-projection-chart',
  extends: Line,
  props: {
    annotation: Object,
    data: Array,
    spatialAxis: Boolean,
    dimension: String,
    slices: Array
  },
  data() {
    return {
      chartData: null
    };
  },
  computed: {
    queryParams() {
      return {};
    },
    sortedData() {
      if (this.spatialAxis) {
        return this.data;
      }
      return this.data.sort((a, b) => {
        if (a.x === b.x) {
          return a.y - b.y;
        }
        return a.x - b.x;
      });
    },
    labels() {
      if (this.spatialAxis && this.dimension === 'channels') {
        return this.sortedData.map(item => this.channelName(item.channel));
      } else if (this.spatialAxis && this.dimension === 'depth') {
        return this.sortedData.map(item => item.zStack);
      } else if (this.spatialAxis && this.dimension === 'duration') {
        return this.sortedData.map(item => item.time);
      } else {
        return this.sortedData.map(item => `(${item.x}, ${item.y})`);
      }
    }
  },
  watch: {
    async queryParams() {
      this.doRenderChart();
    }
  },
  methods: {
    resetZoom() {
      this.$data._chart.resetZoom();
    },
    async doRenderChart() {
      try {
        let data = this.sortedData;

        this.renderChart({
          labels: this.labels,
          datasets: [
            {
              data: data.map(item => item.average),
              fill: false,
              label: this.$t('average'),
              borderColor: '#2778ad',
              backgroundColor: '#61b2e8'
            },
          ]
        }, {
          maintainAspectRatio: false,
          responsive: true,
          legend: {
            display: true
          },
          scales: {
            yAxes: [{
              scaleLabel: {
                display: true,
                labelString: this.$t('pixel-intensity')
              },
              ticks: {
                beginAtZero: true,
              }
            }]
          },
          plugins: {
            zoom: {
              pan: {
                enabled: true,
                mode: 'xy',
              },

              zoom: {
                enabled: true,
                drag: false,
                mode: 'xy',
              }
            }
          }
        });
      } catch (error) {
        console.log(error);
        this.$emit('error', true);
      }
    },
    channelName(value) {
      if (!this.slices || this.slices.length === 0) {
        return value;
      }

      let slice = this.slices.find(slice => slice.channel === value);
      if (!slice) {
        return value;
      }

      return slice.channelName;
    },
  },
  async mounted() {
    this.addPlugin(ChartZoom);
    this.doRenderChart();
  }
};

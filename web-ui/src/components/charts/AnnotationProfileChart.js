import {Line} from 'vue-chartjs';
import ChartZoom from 'chartjs-plugin-zoom';

export default {
  name: 'annotation-profile-chart',
  extends: Line,
  props: {
    annotation: Object,
    bpc: {type: Number, default: 8},
  },
  data() {
    return {
      chartData: null
    };
  },
  computed: {
    queryParams() {
      return {
        bpc: this.bpc
      };
    },
    label() {
      return `(${Math.round(this.annotation.centroid.x)}, ${Math.round(this.annotation.centroid.y)})`;
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
        let data = (await this.annotation.fetchProfile())['profile'];

        this.renderChart({
          labels: [...Array(data.length).keys()],
          datasets: [
            {
              data,
              fill: false,
              label: this.label,
              borderColor: '#2778ad',
              backgroundColor: '#61b2e8'
            }
          ]
        }, {
          maintainAspectRatio: false,
          responsive: true,
          legend: {
            display: false
          },
          scales: {
            xAxes: [{
              scaleLabel: {
                display: true,
                labelString: this.$t('slice')
              }
            }],
            yAxes: [{
              scaleLabel: {
                display: true,
                labelString: this.$t('pixel-intensity')
              },
              ticks: {
                beginAtZero: true,
                max: Math.pow(2, this.bpc) - 1
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
    }
  },
  async mounted() {
    this.addPlugin(ChartZoom);
    this.doRenderChart();
  }
};

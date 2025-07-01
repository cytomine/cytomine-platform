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
<div class="tile is-ancestor is-vertical project-activity-charts-wrapper">
  <div class="tile">
    <div class="tile is-parent">
      <div class="tile is-child box chart-box">
        <div class="columns">
          <h2 class="column">{{$t("activity")}}</h2>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("activity-chart-info-message")}}</p>
                <p>{{$t("barchart-hover-mouse-for-details")}}</p>
                <p>{{$t("barchart-click-label-to-toggle-visibility")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container big">
          <activity-overview-chart css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
            :daysRange="daysRange"
          />
        </div>
      </div>
    </div>

    <div class="tile is-vertical single-metrics">
      <div class="tile is-parent">
        <div class="tile is-child box single-metric">
          <div class="absolute-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("project-connections-info-message")}}</p>
              </template>
            </v-popover>
          </div>
          <strong class="metric">{{nbProjectVisits != null ? nbProjectVisits : "?"}}</strong>
          <strong>{{$t("project-connections")}}</strong>
        </div>
      </div>
      <div class="tile is-parent">
        <div class="tile is-child box single-metric">
          <div class="absolute-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("image-consultations-info-message")}}</p>
              </template>
            </v-popover>
          </div>
          <strong class="metric">{{nbImageConsultations != null ? nbImageConsultations : "?"}}</strong>
          <strong>{{$t("image-consultations")}}</strong>
        </div>
      </div>
      <div class="tile is-parent">
        <div class="tile is-child box single-metric">
          <div class="absolute-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("annotation-selections-info-message")}}</p>
              </template>
            </v-popover>
          </div>
          <strong class="metric">{{nbAnnotationSelections != null ? nbAnnotationSelections : "?"}}</strong>
          <strong>{{$t("annotation-selections")}}</strong>
        </div>
      </div>
    </div>
  </div>

  <div class="tile">
    <div class="tile is-vertical single-metrics">
      <div class="tile is-parent">
        <div class="tile is-child box single-metric">
          <div class="absolute-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("manual-annotations-info-message")}}</p>
              </template>
            </v-popover>
          </div>
          <strong class="metric">
            {{ nbAnnotations[annotationTypes.USER] != null ? nbAnnotations[annotationTypes.USER] : "?" }}
          </strong>
          <strong>{{$t("user-annotations")}}</strong>
        </div>
      </div>
      <div class="tile is-parent">
        <div class="tile is-child box single-metric">
          <div class="absolute-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("reviewed-annotations-info-message")}}</p>
              </template>
            </v-popover>
          </div>
          <strong class="metric">
            {{ nbAnnotations[annotationTypes.REVIEWED] != null ? nbAnnotations[annotationTypes.REVIEWED] : "?" }}
          </strong>
          <strong>{{$t("reviewed-annotations")}}</strong>
        </div>
      </div>
    </div>

    <div class="tile is-parent">
      <div class="tile is-child box chart-box">
        <div class="columns">
          <h2 class="column">{{$t("number-annotations")}}</h2>
          <template v-if="ontology">
            <div class="column is-narrow has-text-right">
              <div class="filter-label">{{$t("term")}}</div>
            </div>
            <div class="column is-one-third">
              <ontology-tree-multiselect
                :ontology="ontology"
                :additionalNodes="[{id: 0, name: this.$t('all')}]"
                :startWithAdditionalNodes="true"
                :multiple="false"
                v-model="selectedTerms"
              />
            </div>
          </template>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("number-annotations-chart-info-message")}}</p>
                <p>{{$t("barchart-hover-mouse-for-details")}}</p>
                <p>{{$t("barchart-click-label-to-toggle-visibility")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container big">
          <number-annotations-chart css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
            :daysRange="daysRange"
            :term="selectedTerms[0] || null"
          /> <!-- If option "all term" (id 0) was selected, return null instead -->
        </div>
      </div>
    </div>
  </div>

  <div class="tile" v-if="ontology">
    <div class="tile is-6 is-parent is-vertical">
      <div class="tile is-child box chart-box no-grow">
        <div class="columns">
          <h2 class="column">{{$t("manual-annotations-vs-term")}}</h2>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("annotations-vs-terms-chart-info-message")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container" :style="styleAnnotationTermChart">
          <annotation-term-chart
            css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
            @nbElems="val => nbElemsAnnotationTermChart = val"
          />
        </div>
      </div>
    </div>
    <div class="tile is-6 is-parent is-vertical">
      <div class="tile is-child box chart-box no-grow">
        <div class="columns">
          <h2 class="column">{{$t("annotated-images-vs-term")}}</h2>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("annotated-images-vs-terms-chart-info-message")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container" :style="styleAnnotationTermChart">
          <annotated-images-by-term-chart
            css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
          />
        </div>
      </div>
    </div>
  </div>

  <div class="tile">
    <div class="tile is-6 is-parent is-vertical">
      <div class="tile is-child box chart-box no-grow">
        <div class="columns">
          <h2 class="column">{{$t("manual-annotations-vs-contributor")}}</h2>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("annotations-vs-contributors-chart-info-message")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container" :style="styleAnnotationContributorChart">
          <annotation-contributor-chart
            css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
            @nbElems="val => nbElemsAnnotationContributorChart = val"
          />
        </div>
      </div>
    </div>
    <div class="tile is-6 is-parent is-vertical">
      <div class="tile is-child box chart-box no-grow">
        <div class="columns">
          <h2 class="column">{{$t("annotated-images-vs-contributor")}}</h2>
          <div class="column is-narrow is-info-circle">
            <v-popover>
              <i class="fas fa-info-circle"></i>
              <template #popover>
                <p>{{$t("annotated-images-vs-contributors-chart-info-message")}}</p>
              </template>
            </v-popover>
          </div>
        </div>
        <div class="chart-container" :style="styleAnnotationContributorChart">
          <annotated-images-by-contributor-chart
            css-classes="chart"
            :project="project"
            :startDate="startDate"
            :endDate="endDate"
          />
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import NumberAnnotationsChart from '@/components/charts/NumberAnnotationsChart.js';
import AnnotationTermChart from '@/components/charts/AnnotationTermChart.js';
import AnnotatedImagesByTermChart from '@/components/charts/AnnotatedImagesByTermChart.js';
import AnnotationContributorChart from '@/components/charts/AnnotationContributorChart.js';
import AnnotatedImagesByContributorChart from '@/components/charts/AnnotatedImagesByContributorChart.js';
import ActivityOverviewChart from '@/components/charts/ActivityOverviewChart.js';
import OntologyTreeMultiselect from '@/components/ontology/OntologyTreeMultiselect';

import {AnnotationType} from 'cytomine-client';

export default {
  name: 'project-activity-charts',
  props: {
    startDate: Number,
    endDate: Number
  },
  components: {
    NumberAnnotationsChart,
    AnnotationTermChart,
    AnnotatedImagesByTermChart,
    AnnotationContributorChart,
    AnnotatedImagesByContributorChart,
    ActivityOverviewChart,
    OntologyTreeMultiselect
  },
  data() {
    return {
      loading: true,
      nbProjectVisits: null,
      nbImageConsultations: null,
      nbAnnotationSelections: null,
      nbAnnotations: {
        [AnnotationType.USER]: null,
        [AnnotationType.REVIEWED]: null
      },
      selectedTerms: [0],
      nbElemsAnnotationTermChart: 0,
      nbElemsAnnotationContributorChart: 0
    };
  },
  computed: {
    project: get('currentProject/project'),
    ontology: get('currentProject/ontology'),
    queryParams() {
      return {
        startDate: this.startDate,
        endDate: this.endDate
      };
    },
    annotationTypes() {
      return AnnotationType;
    },
    daysRange() { // duration of the intervals in evolution plots (depends on the length of the considered period)
      let dayDuration = 24 * 60 * 60 * 1000;
      let intervalDuration = (this.endDate || new Date().getTime()) - (this.startDate || this.project.created);

      if (intervalDuration > 365 * 2 * dayDuration) {
        // large period => find a daysRange resulting in around 30 points
        return Math.round(intervalDuration / dayDuration / 30);
      } else if (intervalDuration > 365 * dayDuration) {
        return 28;
      } else if (intervalDuration > 6 * 31 * dayDuration) {
        return 14;
      } else if (intervalDuration > 31 * dayDuration) {
        return 7;
      } else {
        return 1;
      }
    },
    styleAnnotationTermChart() {
      return {
        height: (30 + this.nbElemsAnnotationTermChart * 25) + 'px'
      };
    },
    styleAnnotationContributorChart() {
      return {
        height: (30 + this.nbElemsAnnotationContributorChart * 25) + 'px'
      };
    }
  },
  watch: {
    startDate() {
      this.loadData();
    },
    endDate() {
      this.loadData();
    }
  },
  methods: {
    async fetchNbConnections() {
      this.nbProjectVisits = null;
      this.nbProjectVisits = await this.project.fetchNbConnections(this.queryParams);
    },
    async fetchNbImageConsultations() {
      this.nbImageConsultations = null;
      this.nbImageConsultations = await this.project.fetchNbImageConsultations(this.queryParams);
    },
    async fetchNbAnnotationSelections() {
      this.nbAnnotationSelections = null;
      this.nbAnnotationSelections = await this.project.fetchNbAnnotationActions({type: 'select', ...this.queryParams});
    },
    async fetchNbAnnotations(type) {
      this.nbAnnotations[type] = null;
      this.nbAnnotations[type] = await this.project.fetchNbAnnotations({annotationType: type, ...this.queryParams});
    },
    async loadData() {
      await Promise.all([
        this.fetchNbConnections(),
        this.fetchNbImageConsultations(),
        this.fetchNbAnnotationSelections(),
        this.fetchNbAnnotations(AnnotationType.USER),
        this.fetchNbAnnotations(AnnotationType.REVIEWED)
      ].map(p => p.catch(e => console.log(e)))); // ignore errors (handled in template) and ensure all promises finish, even if some errors occur in the process);
    }
  },
  async created() {
    await this.loadData();
    this.loading = false;
  }
};
</script>

<style scoped>
h2 {
  font-size: 1.2rem;
  margin-bottom: 0.5em;
}

.single-metrics {
  flex-grow: 0;
  min-width: 20em;
}

.single-metric strong {
  display: block;
  text-align: center;
}

.single-metric .metric {
  font-size: 1.5em;
}

.chart-box {
  display: flex;
  flex-direction: column;
}

.chart-container {
  position: relative;
}

.no-grow {
  flex-grow: 0 !important;
}

.chart-container.big {
  min-height: 20em;
}

.columns {
  align-items: center;
}

.filter-label {
  margin-bottom: 0 !important;
}

.column.is-info-circle {
  padding-top: 0;
  margin-top: -1em;
  margin-left: 1em;
}

.fas.fa-info-circle {
  font-size: 1.25em;
  cursor: pointer;
}

.tile {
  position: relative;
}

.tile > .columns {
  margin-bottom: -0.5rem !important;
}

.absolute-info-circle {
  position: absolute;
  top: 0.75em;
  right: 1em;
}
</style>

<style>
.project-activity-charts-wrapper .chart {
  position: absolute;
  height: 100%;
  width: 100%;
}
</style>

<template>
  <div>
    <b-loading :is-full-page="false" :active="loading" />
    <template v-if="!loading">
      <annotation-profile-projection-chart
          v-if="isLine || spatialAxis"
          @error="val => error = val"
          :annotation="annotation"
          :data="projections"
          :spatial-axis="spatialAxis"
          :dimension="dimension"
          :slices="slices"
          :css-classes="'profile-chart-container'"
          ref="chart"
      />

      <b-table
          :data="projections"
          :paginated="true"
          :per-page="perPage"
          pagination-size="is-small"
      >
        <template #default="{row: item}">
          <b-table-column field="x" :label="'X'" sortable :visible="!spatialAxis">
            {{item.x}}
          </b-table-column>
          <b-table-column field="y" :label="'Y'" sortable :visible="!spatialAxis">
            {{item.y}}
          </b-table-column>
          <b-table-column field="channel" label="C" sortable :visible="spatialAxis && dimension === 'channels'">
            <template v-if="hasChannelName">
              {{channelName(item.channel) || "?"}}
            </template>
            <template v-else>
              {{item.channel}}
            </template>
          </b-table-column>
          <b-table-column field="zStack" label="Z" sortable :visible="spatialAxis && dimension === 'depth'">
            {{item.zStack}}
          </b-table-column>
          <b-table-column field="time" label="T" sortable :visible="spatialAxis && dimension === 'duration'">
            <template v-if="image.fps">
              {{formatMinutesSeconds(item.time / image.fps)}} | {{item.time}}
            </template>
            <template v-else>
              {{item.time}}
            </template>
          </b-table-column>
          <b-table-column field="average" :label="$t('average')" sortable>
            {{item.average}}
          </b-table-column>
          <b-table-column field="min" :label="$t('minimum')" sortable>
            {{item.min}}
          </b-table-column>
          <b-table-column field="max" :label="$t('maximum')" sortable>
            {{item.max}}
          </b-table-column>
        </template>

        <template #footer>
          <p class="has-text-centered">
            <a class="button is-link" :href="downloadURL" target="_self">
              {{$t('button-export-as-csv')}}
            </a>
          </p>
        </template>

        <template #bottom-left>
          <b-select v-model="perPage" size="is-small">
            <option value="10">{{$t('count-per-page', {count: 10})}}</option>
            <option value="25">{{$t('count-per-page', {count: 25})}}</option>
            <option value="50">{{$t('count-per-page', {count: 50})}}</option>
            <option value="100">{{$t('count-per-page', {count: 100})}}</option>
          </b-select>
        </template>
      </b-table>
    </template>
  </div>

</template>

<script>
import constants from '@/utils/constants';
import AnnotationProfileProjectionChart from '@/components/charts/AnnotationProfileProjectionChart';
import {SliceInstanceCollection} from 'cytomine-client';
import {formatMinutesSeconds} from '@/utils/slice-utils';

export default {
  name: 'AnnotationProfileProjectionTable',
  components: {AnnotationProfileProjectionChart},
  props: {
    annotation: Object,
    image: Object,
    spatialAxis: {type: Boolean, default: false}
  },
  data() {
    return {
      projections: [],
      slices: [],
      dimension: null,
      loading: true,
      error: false,
      perPage: 25,
    };
  },
  computed: {
    downloadURL() {
      let axis = (this.spatialAxis) ? '?axis=xy' : '';
      return `${constants.CYTOMINE_CORE_HOST}/api/annotation/${this.annotation.id}/profile/projections.csv${axis}`;
    },
    isLine() {
      return this.annotation.location && this.annotation.location.includes('LINESTRING');
    },
    hasChannelName() {
      return this.slices && this.slices.length > 0 && this.slices[0].channelName !== null;
    }
  },
  methods: {
    async fetchProfileProjection() {
      this.loading = true;
      let axis = (this.spatialAxis) ? 'xy' : null;
      this.projections = (await this.annotation.fetchProfileProjections(axis))['collection'].map(item => {
        let projection = {
          min: item.min,
          max: item.max,
          average: item.average
        };

        if (this.spatialAxis) {
          if ('channel' in item) {
            this.dimension = 'channels';
            projection['channel'] = item.channel;
          } else if ('zStack' in item) {
            this.dimension = 'depth';
            projection['zStack'] = item.zStack;
          } else if ('time' in item) {
            this.dimension = 'duration';
            projection['time'] = item.time;
          } else {
            this.dimension = 'slices';
            projection['slice'] = item.slice;
          }
        } else {
          projection['x'] = item.point[0];
          projection['y'] = item.point[1];
        }

        return projection;
      });

      if (this.dimension === 'channels') {
        this.slices = (await new SliceInstanceCollection({
          filterKey: 'imageinstance',
          filterValue: this.image.id
        }).fetchAll()).array;
      }
      this.loading = false;
    },
    channelName(value) {
      if (!this.slices || this.slices.length === 0) {
        return null;
      }

      let slice = this.slices.find(slice => slice.channel === value);
      if (!slice) {
        return null;
      }

      return slice.channelName;
    },
    formatMinutesSeconds(time) {
      return formatMinutesSeconds(time);
    },
  },
  async mounted() {
    this.fetchProfileProjection();
  }
};
</script>

<style scoped>

</style>

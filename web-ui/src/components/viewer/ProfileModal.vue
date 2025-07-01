<template>
  <cytomine-modal-card
    :title="title"
    class="profile-modal"
    :class="{expanded: expanded}"
    @close="$parent.close()"
  >
    <template #controls>
      <button class="button is-small" @click="expanded = !expanded">
        <i :class="['fas', expanded ? 'fa-compress' : 'fa-expand']"></i>
      </button>
    </template>

    <b-loading :is-full-page="false" :active="loading" />
    <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
      {{$t('failed-fetch-profile')}}
    </b-message>
    <template v-else>

      <div class="media" v-if="isPoint">
        <div class="media-left">
          <p class="image is-64x64">
            <img :src="appendShortTermToken(thumbUrl, shortTermToken)" />
          </p>
        </div>
        <div class="media-content" >
          <p><strong>X: </strong> {{x}}</p>
          <p><strong>Y: </strong> {{y}}</p>
        </div>
        <div class="media-right">
          <button class="button is-small" @click="resetZoom()">{{$t('button-reset-zoom')}}</button>
        </div>
      </div>

      <template v-if="!loading">
        <div class="profile-box" v-if="isPoint">
          <annotation-profile-chart
              @error="val => error = val"
              :annotation="annotation"
              :bpc="bpc"
              :css-classes="'profile-chart-container'"
              ref="chart"
          />
        </div>
        <annotation-profile-projection-table v-else :annotation="annotation" :spatial-axis="spatialAxis" :image="image" />
      </template>
    </template>
  </cytomine-modal-card>
</template>

<script>
import CytomineModalCard from '@/components/utils/CytomineModalCard';
import AnnotationProfileChart from '@/components/charts/AnnotationProfileChart';
import AnnotationProfileProjectionTable from '@/components/viewer/AnnotationProfileProjectionTable';
import {appendShortTermToken} from '@/utils/token-utils.js';
import {get} from '@/utils/store-helpers';

export default {
  name: 'profile-modal',
  components: {
    AnnotationProfileProjectionTable,
    AnnotationProfileChart,
    CytomineModalCard
  },
  props: {
    annotation: Object,
    image: {type: Object, default: null},
    spatialAxis: {type: Boolean, default: false}
  },
  data() {
    return {
      thumbSize: 64,
      expanded: false,
      loading: true,
      error: false,
    };
  },
  computed: {
    shortTermToken: get('currentUser/shortTermToken'),
    x() {
      return Math.round(this.annotation.centroid.x);
    },
    y() {
      return Math.round(this.annotation.centroid.y);
    },
    thumbParams() {
      return {
        square: true,
        complete: true,
        thickness: 2,
        increaseArea: 1.25,
        draw: true
      };
    },
    thumbUrl() {
      return this.annotation.annotationCropURL(this.thumbSize, 'jpg', this.thumbParams);
    },
    bpc() {
      return (this.image && this.image.bitPerSample) ? this.image.bitPerSample : 8;
    },
    isPoint() {
      return this.annotation.location && this.annotation.location.includes('POINT');
    },
    title() {
      if (this.isPoint) {
        return this.$t('profile');
      } else if (this.spatialAxis && this.image.channels > 1) {
        return this.$t('fluorescence-spectra');
      } else if (this.spatialAxis && this.image.depth > 1) {
        return this.$t('depth-spectra');
      } else if (this.spatialAxis && this.image.duration > 1) {
        return this.$t('temporal-spectra');
      }
      return this.$t('profile-projection');
    }
  },
  methods: {
    appendShortTermToken,
    resetZoom() {
      this.$refs.chart.resetZoom();
    }
  },
  created() {
    this.loading = false;
  }
};
</script>

<style lang="scss">
.profile-modal {
  &.expanded, &.expanded .modal-card-body {
    width: 90vw;
    height: 90vh;
    max-height: 90vh;
  }

  &:not(.expanded), &:not(.expanded) .modal-card-body {
    width: 50vw;
    height: 60vh;
    max-height: 60vh;
  }

  .profile-chart-container {
    position: relative;
    height: 100%;
  }

  .profile-box {
    display: flex;
    flex-direction: column;
    height: calc(100% - 79px);
    margin-top: 15px;
  }
}
</style>

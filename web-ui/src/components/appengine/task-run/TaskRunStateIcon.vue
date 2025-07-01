<template>
  <b-tooltip :label="state" :type="stateType">
    <div class="block">
      <b-icon :icon="stateIcon" size="is-small" :type="stateType" />
    </div>
  </b-tooltip>
</template>

<script>
export default {
  name: 'TaskRunStateIcon',
  props: {
    state: {
      type: String
    },
  },
  computed: {
    stateIconography() {
      switch (this.state) {
        case 'CREATED':
          return {'type': 'is-primary', 'icon': 'plus-circle'};
        case 'PROVISIONED':
          return {'type': 'is-primary', 'icon': 'boxes'};
        case 'QUEUING':
          return {'type': 'is-warning', 'icon': 'route'};
        case 'QUEUED':
          return {'type': 'is-warning', 'icon': 'route'};
        case 'PENDING':
          return {'type': 'is-warning', 'icon': 'pause'};
        case 'RUNNING':
          return {'type': 'is-primary', 'icon': 'running'};
        case 'FAILED':
          return {'type': 'is-danger', 'icon': 'times'};
        case 'FINISHED':
          return {'type': 'is-success', 'icon': 'check-circle'};
        default:
          return {'type': 'is-primary', 'icon': 'question-circle'};
      }
    },
    stateIcon() {
      return this.stateIconography.icon;
    },
    stateType() {
      return this.stateIconography.type;
    }
  }
};
</script>

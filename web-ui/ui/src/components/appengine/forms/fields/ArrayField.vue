<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        <span class="text-label">
          {{ parameter.display_name }}
        </span>
      </template>

      <b-button @click="selectParameters = true">
        {{ $t('select') }}
      </b-button>

      <div class="state-container" v-if="value">
        {{ $t('provisioned') }}
      </div>
    </b-field>

    <div class="info">
      <b-tooltip :label="tooltip" type="is-primary" position="is-right">
        <b-icon pack="fas" icon="info-circle"/>
      </b-tooltip>
    </div>

    <array-modal
      :active.sync="selectParameters"
      :max-size="maxSize"
      :min-size="minSize"
      :type="subType"
      @create-inputs="input = $event"
    />
  </div>
</template>

<script>
import ArrayModal from '@/components/appengine/forms/fields/array/ArrayModal';

export default {
  name: 'ArrayField',
  components: {
    ArrayModal,
  },
  props: {
    parameter: {type: Object, required: true},
    value: {},
  },
  data() {
    return {
      selectParameters: false,
    };
  },
  computed: {
    input: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit('input', value);
      }
    },
    type() {
      return this.parameter.type;
    },
    subType() {
      return this.type.subType;
    },
    maxSize() {
      let {maxSize} = this.type;
      return maxSize;
    },
    minSize() {
      let {minSize} = this.type;
      return minSize;
    },
    hasConstraints() {
      let {minSize, maxSize} = this.type;
      return minSize !== null || maxSize !== null;
    },
    constraintsSummary() {
      let {minSize, maxSize} = this.type;
      let summary = '';

      if (!!minSize || minSize === 0) {
        summary += `${minSize} ≤ `;
      }

      summary += 'items';

      if (!!maxSize || maxSize === 0) {
        summary += ` ≤ ${maxSize}`;
      }

      return summary;
    },
    tooltip() {
      let tooltip = this.parameter.description;
      if (this.hasConstraints) {
        tooltip += `, ${this.constraintsSummary}`;
      }
      return tooltip;
    },
  },
};
</script>

<style scoped>
.container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field {
  margin-left: auto;
  margin-right: auto;
}

.info {
  text-align: right;
  margin-left: 5px;
}

.state-container {
  margin-top: 5px;
  margin-left: 10px;
}

.text-label {
  display: block;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

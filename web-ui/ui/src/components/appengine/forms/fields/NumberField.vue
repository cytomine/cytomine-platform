<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        {{ parameter.display_name }}
      </template>
      <b-numberinput
        v-model="input"
        :placeholder="constraintsSummary"
        :controls="false"
        :max="max"
        :min="min"
        :step="0.0001"
      />
    </b-field>
    <div class="info">
      <b-tooltip v-if="tooltip" :label="tooltip" type="is-primary" position="is-right">
        <b-icon pack="fas" icon="info-circle"/>
      </b-tooltip>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NumberField',
  props: {
    parameter: {type: Object, required: true},
    value: {},
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
    hasConstraints() {
      let {gt, lt, geq, leq, infinityAllowed, nanAllowed} = this.type;
      return gt !== null || lt !== null || geq !== null || leq !== null || infinityAllowed || nanAllowed;
    },
    constraintsSummary() {
      let {gt, lt, geq, leq, infinityAllowed, nanAllowed} = this.type;
      let summary = '';

      if (!!geq || geq === 0) {
        summary += `${geq} ≤ `;
      } else if (!!gt || gt === 0) {
        summary += `${gt} < `;
      }

      summary += this.parameter.display_name;
      if (!!leq || leq === 0) {
        summary += ` ≤ ${leq}`;
      } else if (!!lt || lt === 0) {
        summary += ` < ${lt}`;
      }

      if (infinityAllowed) {
        summary += ', ∞';
      }

      if (nanAllowed) {
        summary += ', NaN allowed';
      }

      return summary;
    },
    tooltip() {
      if (this.parameter.description === null) {
        return null;
      }
      if (this.hasConstraints) {
        return `${this.parameter.description}, ${this.constraintsSummary}`;
      }
      return this.parameter.description;
    },
    min() {
      let min = null;
      let {gt, geq} = this.type;
      if (!!geq || geq === 0) {
        min = geq;
      }
      if (!!gt || gt === 0) {
        min = gt + 1;
      }
      return min;
    },
    max() {
      let max = null;
      let {lt, leq} = this.type;
      if (!!leq || leq === 0) {
        max = leq;
      }
      if (!!lt || lt === 0) {
        max = lt - 1;
      }
      return max;
    }
  }
};
</script>

<style scoped>
.container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field {
  flex: 1;
}

.info {
  text-align: right;
  margin-left: 5px;
}
</style>

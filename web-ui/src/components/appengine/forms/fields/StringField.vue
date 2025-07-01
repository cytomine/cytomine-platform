<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        {{ parameter.display_name }}
      </template>
      <b-input
        v-model="input"
        :has-counter="true"
        :maxlength="max"
        :minlength="min"
        :placeholder="constraintsSummary"
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
  name: 'StringField',
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
      let {minLength, maxLength} = this.type;
      return minLength !== null || maxLength !== null;
    },
    constraintsSummary() {
      let {minLength, maxLength} = this.type;
      let summary = '';
      if (!!minLength || minLength === 0) {
        summary += `${minLength} ≤ `;
      }

      summary += this.parameter.display_name + '.length';

      if (!!maxLength || maxLength === 0) {
        summary += ` ≤ ${maxLength}`;
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
      let {minLength} = this.type;
      if (!!minLength || minLength === 0) {
        return minLength;
      }

      return 0;
    },
    max() {
      let {maxLength} = this.type;
      if (!!maxLength || maxLength === 0) {
        return maxLength;
      }

      return null;
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

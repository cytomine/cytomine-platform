<template>
  <div class="container">
    <b-field class="field" position="is-centered" expanded>
      <span class="display-name">{{ parameter.display_name }}</span>
      <b-radio v-model="input" :native-value="true">True</b-radio>
      <b-radio v-model="input" :native-value="false">False</b-radio>
      <b-button v-if="optional" type="is-text" @click="input = null">
        <i class="fas fa-times"/>
      </b-button>
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
  name: 'BooleanField',
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
    optional() {
      return this.parameter.optional;
    },
    tooltip() {
      return this.parameter.description;
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

.display-name {
  font-weight: bold;
  margin-right: 10px;
}

.field {
  margin-left: auto;
  margin-right: auto;
}

.info {
  text-align: right;
  margin-left: 5px;
}
</style>

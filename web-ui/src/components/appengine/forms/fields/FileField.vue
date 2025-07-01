<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        {{ parameter.display_name }}
      </template>
      <div class="file has-name is-boxed is-small">
        <label class="file-label">
          <input class="file-input" type="file" name="resume" @change="selectFile"/>
          <span class="file-cta">
            <span class="file-label">{{ $t("add-attached-file") }}</span>
          </span>
          <span class="file-name">{{ value ? value.name : $t("no-attached-file") }}</span>
        </label>
      </div>
    </b-field>

    <b-button
      v-if="value"
      @click="input = null"
      class="clear-button"
    >
      {{ $t("button-clear") }}
    </b-button>

    <div class="info">
      <b-tooltip :label="parameter.description" type="is-primary" position="is-right">
        <b-icon pack="fas" icon="info-circle"/>
      </b-tooltip>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FileField',
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
  },
  methods: {
    selectFile(event) {
      this.input = event.target.files[0];
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
  margin-left: auto;
  margin-right: auto;
}

.info {
  text-align: right;
  margin-left: 5px;
}
</style>

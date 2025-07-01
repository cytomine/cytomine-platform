<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        <span class="text-label">
          {{ parameter.display_name }}
        </span>
      </template>
      <b-button @click="selectAnnotation = true">
        {{ $t('select') }}
      </b-button>

      <div class="annotation-container" v-if="value">
        {{ $t('annotation') }} {{ value }}
      </div>
    </b-field>

    <div class="info">
      <b-tooltip :label="parameter.description" type="is-primary" position="is-right">
        <b-icon pack="fas" icon="info-circle"/>
      </b-tooltip>
    </div>

    <annotation-selection :active.sync="selectAnnotation" @select-annotation="input = $event"/>
  </div>
</template>

<script>
import AnnotationSelection from '@/components/annotations/AnnotationSelection';

export default {
  name: 'GeometryField',
  components: {
    AnnotationSelection,
  },
  props: {
    parameter: {type: Object, required: true},
    value: {},
  },
  data() {
    return {
      selectAnnotation: false,
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
  }
};
</script>

<style scoped>
.annotation-container {
  margin-top: 5px;
  margin-left: 10px;
}

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

.text-label {
  display: block;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

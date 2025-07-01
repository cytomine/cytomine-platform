<template>
  <div class="container">
    <b-field label-position="on-border" expanded class="field">
      <template #label>
        <span class="text-label">
          {{ parameter.display_name }}
        </span>
      </template>

      <div class="buttons">
        <b-button @click="selectAnnotation = true">
          <i class="fas fa-draw-polygon"/>
        </b-button>
        <b-button @click="selectImage = true">
          <i class="fas fa-image"/>
        </b-button>
      </div>

      <div class="value-container" v-if="value">
        {{ $t(type) }} {{ value.id }}
      </div>
    </b-field>

    <div class="info">
      <b-tooltip :label="parameter.description" type="is-primary" position="is-right">
        <b-icon pack="fas" icon="info-circle"/>
      </b-tooltip>
    </div>

    <annotation-selection :active.sync="selectAnnotation" @select-annotation="selectInput($event, 'annotation')"/>

    <image-selection
      :active.sync="selectImage"
      :formats="['JPEG', 'PNG', 'PLANARTIFF']"
      @select-image="selectInput($event, 'image')"
    />
  </div>
</template>

<script>
import AnnotationSelection from '@/components/annotations/AnnotationSelection';
import ImageSelection from '@/components/image/ImageSelection';

export default {
  name: 'ImageField',
  components: {
    AnnotationSelection,
    ImageSelection,
  },
  props: {
    parameter: {type: Object, required: true},
    value: {},
  },
  data() {
    return {
      selectAnnotation: false,
      selectImage: false,
      type: null,
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
  },
  methods: {
    selectInput(input, type) {
      this.input = {type, id: input};
      this.type = type;
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

.text-label {
  display: block;
  width: 100%;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value-container {
  margin-top: 5px;
  margin-left: 10px;
}
</style>

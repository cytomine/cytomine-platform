<template>
  <form @submit.prevent="select">
    <cytomine-modal :active="active" :title="$t('add-inputs')" @close="cancel">
      <div
        v-if="isPrimitive"
        :class="{'max-h-64 overflow-y-auto space-y-2 pr-2': items.length <= 5, 'space-y-2 pr-2': items.length > 5}"
        style="overflow-y: auto; max-height: 16rem;"
        @scroll="onScroll"
      >
        <b-field class="field-container" v-for="(value, index) in items" :key="index">
          <b-field position="is-centered" grouped>
            <component
              v-model="items[index]"
              :is="selectedPrimitiveField"
              :parameter="parameter"
            />
            <b-button @click="remove(index)">
              <i class="fas fa-times"/>
            </b-button>
          </b-field>
        </b-field>
      </div>

      <div v-else>
        <component
          v-model="items"
          :is="selectedComplexField"
        />
      </div>

      <template #footer>
        <b-button
          v-if="isPrimitive"
          :disabled="maxSize && items.length >= maxSize"
          @click="add"
        >
          <i class="fas fa-plus"/>
        </b-button>
        <button class="button" type="button" @click="cancel">
          {{ $t('button-cancel') }}
        </button>
        <button class="button is-link">
          {{ $t('select') }}
        </button>
      </template>
    </cytomine-modal>
  </form>
</template>

<script>
import AnnotationMultiSelect from '@/components/appengine/forms/fields/array/AnnotationMultiSelect';
import BooleanField from '@/components/appengine/forms/fields/BooleanField';
import CytomineModal from '@/components/utils/CytomineModal';
import EnumerationField from '@/components/appengine/forms/fields/EnumerationField';
import ImageMultiSelect from '@/components/appengine/forms/fields/array/ImageMultiSelect';
import IntegerField from '@/components/appengine/forms/fields/IntegerField';
import NumberField from '@/components/appengine/forms/fields/NumberField';
import StringField from '@/components/appengine/forms/fields/StringField';

export default {
  name: 'ArrayModal',
  components: {
    CytomineModal,
  },
  props: {
    active: {type: Boolean, default: false},
    maxSize: {type: Number, default: null},
    minSize: {type: Number, default: null},
    type: {type: Object, required: true},
  },
  data() {
    return {
      items: [],
    };
  },
  computed: {
    isPrimitive() {
      const PRIMITIVES = ['boolean', 'integer', 'number', 'string', 'enumeration'];
      return PRIMITIVES.includes(this.type.id);
    },
    parameter() {
      return {default: null, description: null, type: this.type};
    },
    selectedPrimitiveField() {
      switch (this.type.id) {
        case 'boolean':
          return BooleanField;
        case 'enumeration':
          return EnumerationField;
        case 'integer':
          return IntegerField;
        case 'number':
          return NumberField;
        case 'string':
          return StringField;
        default:
          return null;
      }
    },
    selectedComplexField() {
      switch (this.type.id) {
        case 'geometry':
        case 'image':
          return AnnotationMultiSelect;
        case 'wsi':
          return ImageMultiSelect;
        default:
          return null;
      }
    },
  },
  methods: {
    cancel() {
      this.$emit('update:active', false);
    },
    select() {
      if (this.items.length === 0) {
        this.$notify({type: 'error', text: this.$t('notify-error-empty-list')});
        return;
      }

      if (this.minSize !== null && this.items.length < this.minSize) {
        this.$notify({type: 'error', text: this.$t('notify-error-not-enough-item')});
        return;
      }

      this.$emit('create-inputs', this.items);
      this.$emit('update:active', false);

      this.items = [];

      this.$notify({type: 'success', text: this.$t('notify-success-create-array-inputs')});
    },
    add() {
      this.items.push(null);
    },
    remove(index) {
      this.items.splice(index, 1);
    },
    onScroll(e) {
      const el = e.target;
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 10) {
        if (this.maxVisible < this.list.length) {
          this.maxVisible += 5;
        }
      }
    },
  },
};
</script>

<style scoped>
.field-container {
  margin-bottom: 0 !important;
}
</style>

<template>
  <div>
    (
    <span v-if="!editing" @click="editing = true; $emit('editing');">{{ bounds.min }}</span>
    <span v-else>
      <input class="input" v-model="minVal" v-on:keyup.enter="setBounds(minVal, maxVal)"/>
    </span>
    -
    <span v-if="!editing" @click="editing = true; $emit('editing');">{{ bounds.max }}</span>
    <span v-else>
      <input class="input" v-model="maxVal" v-on:keyup.enter="setBounds(minVal, maxVal)"/>
    </span>
    )
  </div>
</template>

<script>
export default {
  name: 'editable-text-bound',
  props: {
    bounds: Object,
    defaultBounds: Object,
  },
  data() {
    return {
      editing: false,
      minVal: this.bounds.min,
      maxVal: this.bounds.max,
    };
  },
  methods: {
    setBounds(min, max) {
      this.editing = false;

      min = Math.max(this.defaultBounds.min, Math.min(Number(min), this.defaultBounds.max));
      max = Math.min(this.defaultBounds.max, Math.max(Number(max), this.defaultBounds.min));

      if (min > max || isNaN(min) || isNaN(max)) {
        this.minVal = this.bounds.min;
        this.maxVal = this.bounds.max;
        return;
      }

      this.minVal = min;
      this.maxVal = max;

      this.$emit('setBounds', {min, max});
    },
  },
};
</script>

<style scoped>
.input {
  font-size: 0.7rem;
  width: 2.8rem;
}
</style>

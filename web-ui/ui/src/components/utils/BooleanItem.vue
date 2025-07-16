<template>
  <span v-if="value" class="tag is-success">
    <i class="fas fa-check"></i>&nbsp;{{$t('yes')}}
  </span>
  <span v-else class="tag is-danger">
    <i class="fas fa-times"></i>&nbsp;{{$t('no')}}
  </span>
</template>

<script>
export default {
  name: 'boolean-item',
  props: {
    value: Boolean
  }
};
</script>

<style scoped>

</style>

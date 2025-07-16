<template>
<b-taginput
  :value="value"
  @input="$emit('input', $event)"
  :data="filteredUsers"
  autocomplete
  :open-on-focus="true"
  field="fullName"
  :placeholder="$t('search-user')"
  @typing="val => searchString = val"
  @add="searchString = ''"
  :allow-duplicates="false"
/>
</template>

<script>
import {getWildcardRegexp} from '@/utils/string-utils';

export default {
  name: 'user-taginput',
  props: {
    value: Array,
    users: Array
  },
  data() {
    return {
      searchString: ''
    };
  },
  computed: {
    filteredUsers() {
      let selectedIds = this.value.map(v => v.id);
      let filtered = this.users.filter(user => !selectedIds.includes(user.id));
      if (this.searchString === '') {
        return filtered;
      }

      let regexp = getWildcardRegexp(this.searchString);
      return filtered.filter(user => regexp.test(user.fullName));
    }
  }
};
</script>

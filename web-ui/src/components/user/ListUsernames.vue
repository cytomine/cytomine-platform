<!-- Copyright (c) 2009-2022. Authors: see NOTICE file.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.-->

<template>
<span v-if="filteredUsers.length">
  <span v-for="(user, index) in usersToDisplay" :key="user.id">
    <username :user="user" :online="isOnline(user.id)" /><template v-if="index < usersToDisplay.length - 1">, </template>
  </span>
  <button class="button is-small" @click="expanded=!expanded" v-if="tooManyUsers">
    {{ expanded ? $t('button-less') : $t('button-more')}}
  </button>
</span>
<span v-else>
  -
</span>
</template>

<script>
import Username from './Username';
export default {
  name: 'list-usernames',
  components: {Username},
  props: {
    users: Array,
    onlines: Array,
    nbDisplayed: {
      type: Number,
      default: 5
    }
  },
  data() {
    return {
      expanded: false
    };
  },
  computed: {
    filteredUsers() {
      return this.users.filter(user => user); // Filter null users
    },
    tooManyUsers() {
      return this.filteredUsers && this.filteredUsers.length > this.nbDisplayed;
    },
    usersToDisplay() {
      if (!this.tooManyUsers || this.expanded || this.nbDisplayed === 0) {
        return this.filteredUsers;
      } else {
        return this.filteredUsers.slice(0, this.nbDisplayed);
      }
    }
  },
  methods: {
    isOnline(id) {
      if (!this.onlines) {
        return null;
      }

      return this.onlines.some(online => online.id === id);
    }
  }
};
</script>

<style scoped>
.button {
  line-height: initial;
  height: initial;
  margin-left: 0.5em;
}
</style>

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
<span v-if="resolvedUser">
  <span :class="[online ? 'online-dot' : 'offline-dot']" v-if="online !== null"></span>
  {{displayFullName ? resolvedUser.fullName : resolvedUser.username}}</span></template>

<script>
import {User} from 'cytomine-client';

export default {
  // eslint-disable-next-line
  name: 'username',
  props: {
    user: Object,
    id: Number, // if user not set, the user corresponding to the provided id will be fetched
    online: {type: Boolean, default: null},
    displayFullName: {type: Boolean, default: true}
  },
  data() {
    return {
      fetchedUser: {}
    };
  },
  computed: {
    resolvedUser() {
      return this.user || this.fetchedUser;
    },
  },
  async created() {
    if (!this.user && this.id) {
      this.fetchedUser = await User.fetch(this.id);
    }
  }
};
</script>

<style scoped>
.online-dot, .offline-dot {
  height: 0.8em;
  width: 0.8em;
  border-radius: 50%;
  display: inline-block;
  margin-right: 0.2em;
}

.online-dot {
  background: limegreen;
}

.offline-dot {
  background: lightgrey;
}
</style>

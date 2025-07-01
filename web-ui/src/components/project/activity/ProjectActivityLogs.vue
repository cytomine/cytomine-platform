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
<div class="tile is-child box last-actions">
  <h2>{{$t('activity-logs')}}</h2>

  <b-select v-model="selectedUser">
    <option :value="null">{{$t('all-users-analyses')}}</option>
    <optgroup :label="$t('members')">
      <option v-for="member in members" :value="member.id" :key="member.id">{{member.fullName}}</option>
    </optgroup>
  </b-select>

  <activity-logs :idUser="selectedUser" :startDate="startDate" :endDate="endDate" :project="project" />
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import ActivityLogs from '@/components/utils/ActivityLogs';

export default {
  name: 'project-activity-logs',
  props: {
    startDate: Number,
    endDate: Number
  },
  components: {ActivityLogs},
  data() {
    return {
      selectedUser: null,
    };
  },
  computed: {
    project: get('currentProject/project'),
    members: get('currentProject/members'),
  },
  async created() {

  }
};
</script>

<style scoped>
.last-actions {
  height: 100%;
  display: flex;
  flex-direction: column;
  word-wrap: break-word;
}
</style>

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
<div class="content-wrapper">
  <div class="box">
    <h2>{{$t('activity-logs')}}</h2>
    <activity-logs :idUser="currentUser.id" />
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import ActivityLogs from '@/components/utils/ActivityLogs';

export default {
  name: 'user-activity',
  components: {ActivityLogs},
  computed: {
    currentUser: get('currentUser/user')
  }
};
</script>

<style scoped>
.content-wrapper {
  height: 100%;
}

.box {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>

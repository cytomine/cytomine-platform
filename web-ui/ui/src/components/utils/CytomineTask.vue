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
<div v-if="task">
  <progress class="progress is-info" :value="task.progress" max="100">
    {{task.progress}}%
  </progress>
  <p v-if="lastComment">{{lastComment}}</p> <!-- WARNING: not translated -->
</div>
</template>

<script>
import constants from '@/utils/constants.js';
import {Task} from 'cytomine-client';

export default {
  name: 'cytomine-task',
  props: {
    task: Object,
    refreshInterval: {
      type: Number,
      default: constants.TASK_REFRESH_INTERVAL
    }
  },
  data() {
    return {
      timeout: null
    };
  },
  computed: {
    lastComment() {
      if (this.task && this.task.comments && this.task.comments.length > 0) {
        return this.task.comments[0];
      }
      return '';
    }
  },
  watch: {
    task() {
      this.refresh();
    }
  },
  methods: {
    async refresh() {
      if (this.task) {
        let updatedTask = await Task.fetch(this.task.id);
        this.$emit('update:task', updatedTask);
        clearTimeout(this.timeout);
        if (updatedTask.progress < 100) {
          this.timeout = setTimeout(this.refresh, this.refreshInterval);
        }
      }
    }
  },
  created() {
    this.refresh();
  }
};
</script>

<style scoped>
.progress {
  margin: 0.5em 0 !important;
}
</style>

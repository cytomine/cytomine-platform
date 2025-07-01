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
<b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
  <h2> {{ $t('error') }} </h2>
  <p> {{ $t('unexpected-error-info-message') }} </p>
</b-message>
<div class="list-actions" @scroll="scrollHandler" ref="listActions" v-else-if="actions.length > 0">
  <div v-for="(month, idx) in formattedActions" :key="idx">
    <h3>{{month.refDate | moment('MMMM YYYY')}}</h3>
    <div v-for="(day, idx) in month.days" :key="idx">
      <h4>{{day.refDate | moment('ll')}}</h4>
      <ul>
        <activity-logs-item v-for="action in day.actions" :action="action" :key="action.id" />
      </ul>
    </div>
  </div>

  <button class="button" :class="{'is-loading': loading}" v-if="!loadedAllActions" @click="loadActions()">
    {{$t('button-load-more')}}
  </button>
</div>
<div class="no-result" v-else>
  <em class="has-text-grey">{{$t('no-log-to-display')}}</em>
</div>
</template>

<script>
import {Project} from 'cytomine-client';
import moment from 'moment';
import _ from 'lodash';

import ActivityLogsItem from './ActivityLogsItem';

export default {
  name: 'activity-logs',
  props: {
    startDate: Number,
    endDate: Number,
    idUser: Number,
    project: Object
  },
  components: {ActivityLogsItem},
  data() {
    return {
      loading: true,
      error: false,

      offset: 0,
      nbActionsPerPage: 50,
      loadedAllActions: false,

      actions: []
    };
  },
  computed: {
    params() {
      return {
        user: this.idUser,
        startDate: this.startDate,
        endDate: this.endDate,
        project: this.project ? this.project.id : null,
        fullData: true
      };
    },
    formattedActions() {
      let results = [];
      let lastDate = null;
      let idxMonths = -1;
      let idxDays = -1;
      for (let i = 0; i < this.actions.length; i++) {
        let action = this.actions[i];
        let date = moment(Number(action.created));
        if (!lastDate || !date.isSame(lastDate, 'day')) {
          if (!lastDate || !date.isSame(lastDate, 'month')) {
            results.push({
              refDate: Number(action.created),
              days: []
            });
            idxMonths++;
            idxDays = -1;
          }

          results[idxMonths].days.push({refDate: Number(action.created), actions: [action]});
          idxDays++;
        } else {
          results[idxMonths].days[idxDays].actions.push(action);
        }
        lastDate = date;
      }

      return results;
    }
  },
  watch: {
    params() {
      this.loadActions(false);
    }
  },
  methods: {
    scrollHandler: _.debounce(function () {
      let scrollBlock = this.$refs.listActions;
      let bottom = (scrollBlock.scrollTop + scrollBlock.clientHeight === scrollBlock.scrollHeight);

      if (bottom && !this.loadedAllActions) {
        this.loadActions();
      }
    }, 100),

    async loadActions(append = true) {
      this.loading = true;
      if (!append) {
        this.actions = [];
        this.offset = 0;
        this.loadedAllActions = false;
      }

      try {
        let data = await Project.fetchCommandHistory({
          max: this.nbActionsPerPage,
          offset: this.offset,
          ...this.params
        });

        this.actions.push(...data);
        this.offset += this.nbActionsPerPage;

        if (data.length < this.nbActionsPerPage) {
          this.loadedAllActions = true;
        }
      } catch (error) {
        console.log(error);
        this.error = true;
      }

      this.loading = false;
    }
  },
  async created() {
    try {
      await this.loadActions();
    } catch (error) {
      console.log(error);
      this.error = true;
    }

    this.loading = false;
  }
};
</script>

<style scoped>
.message {
  margin-top: 1em;
}

.list-actions {
  overflow: auto;
  margin-top: 1em;
}

.no-result {
  margin-top: 1em;
}

h3 {
  font-weight: bold;
  color: #555;
}

.list-actions div:not(:first-child) h3 {
  padding: 0.5em 0;
  margin-top: 1em;
  border-top: rgba(0, 0, 0, 0.1) 2px solid;
}

h4 {
  padding: 0.9em 1.2em 0.9em;
  color: #aaa;
  text-transform: uppercase;
  font-weight: bold;
  font-size: 0.9rem;
}

.button {
  display: block;
  margin: auto;
}
</style>

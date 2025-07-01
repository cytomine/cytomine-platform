<template>
  <b-table
    :current-page.sync="currentPage"
    :data="taskRuns"
    :paginated="taskRuns.length > 5"
    :show-table="false"
    detailed
    narrowed
    per-page="5"
  >
    <template #default="props">
      <b-table-column :label="$t('app-engine.task-run.state.title')">
        <task-run-state-icon :state="props.row.state"/>
      </b-table-column>

      <b-table-column :label="$t('app-engine.task.name')">
        {{ props.row.task.name }} ({{ props.row.task.version }})
      </b-table-column>
    </template>

    <template #detail="props">
      <section v-if="props.row.inputs">
        <h5 class="subtitle">{{ $t('app-engine.inputs.title') }}</h5>
        <task-run-parameters-table :parameters="props.row.inputs"/>
      </section>
      <section v-if="props.row.outputs" class="output-section">
        <h5 class="subtitle">{{ $t('app-engine.outputs.title') }}</h5>
        <task-run-parameters-table :parameters="props.row.outputs"/>
      </section>
    </template>

  </b-table>
</template>

<script>
import TaskRunStateIcon from '@/components/appengine/task-run/TaskRunStateIcon';
import TaskRunParametersTable from '@/components/appengine/task-run/TaskRunParametersTable';

export default {
  name: 'TaskRunTable',
  components: {
    TaskRunStateIcon,
    TaskRunParametersTable
  },
  props: {
    taskRuns: {type: Array, required: true},
  },
  data() {
    return {
      currentPage: 1,
    };
  },
};
</script>

<style scoped>
.output-section {
  margin-top: 5px;
}
</style>

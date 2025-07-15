<template>
  <b-table :data="parameters" narrowed>
    <template #default="props">
      <b-table-column field="name" :label="$t('app-engine.parameter.name')">
        {{ props.row.param_name }}
      </b-table-column>
      <b-table-column field="type" :label="$t('app-engine.parameter.type')">
        {{ props.row.type }}
      </b-table-column>
      <b-table-column
        v-if="!['FILE', 'IMAGE', 'WSI'].includes(props.row.type)"
        field="value"
        :label="$t('app-engine.parameter.value')"
      >
        {{ props.row.value }}
      </b-table-column>
      <b-table-column v-else field="value" :label="$t('app-engine.parameter.value')">
        <button class="button is-link is-small" @click="downloadFile(props.row)">{{ $t('button-download') }}</button>
      </b-table-column>
    </template>
  </b-table>
</template>

<script>
export default {
  name: 'TaskRunParametersTable',
  props: {
    parameters: {type: Array, required: true},
  },
  methods: {
    downloadFile(output) {
      let blob = new Blob([new Uint8Array(output.value)], {type: 'application/octet-stream'});
      let url = URL.createObjectURL(blob);

      let link = document.createElement('a');
      link.href = url;
      link.download = output.param_name;
      link.style.display = 'none';

      document.body.appendChild(link);
      link.click();

      URL.revokeObjectURL(url);
      document.body.removeChild(link);
    }
  },
};
</script>

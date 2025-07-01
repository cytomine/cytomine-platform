<template>
  <div>
    <input type="file" ref="fileInput" style="display: none;" @change="handleFileChange"/>
    <button id="app-upload-btn" class="button is-link" @click="uploadTask">{{ $t(btnFunc) }}</button>
  </div>
</template>

<script>
import Task from '@/utils/appengine/task';

export default {
  name: 'UploadAppButton',
  props: {
    btnFunc: {type: String, required: true},
  },
  data() {
    return {
      task: null,
    };
  },
  methods: {
    handleFileChange(event) {
      const file = event.target.files[0];
      this.upload(file);
    },
    async uploadTask() {
      this.$refs.fileInput.click();
    },
    async upload(file) {
      const formData = new FormData();
      formData.append('task', file);

      try {
        const response = await Task.uploadTask(formData);

        if (response.status >= 200 && response.status < 300) {
          // trigger success so we fetch all tasks (will re-render so we see new task)
          this.$emit('taskUploadSuccess');
          this.$notify({type: 'success', text: this.$t('notify-success-task-upload')});
        }
      } catch (error) {
        if (error.response && error.response.status === 409) {
          const errorMessage = error.response.data.message || 'Task already exists!';

          this.$notify({type: 'warn', text: errorMessage});
        } else {
          const errorMessage = error.response.data.message || 'Unknown Error!';

          this.$notify({type: 'error', text: errorMessage});
        }
      }
    },
  },
};
</script>

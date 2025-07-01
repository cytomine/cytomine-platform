<template>
  <div class="profile-status">
    <em v-if="!eligible">{{$t('not-available')}}</em>
    <template v-else-if="!profile">
      {{$t('no-profile')}}
      <div class="buttons">
        <button class="button is-small" @click="confirmProfileComputation">{{$t('button-compute-profile')}}</button>
      </div>

    </template>
    <template v-else>
      <uploaded-file-status :file="profile" />
      <div v-if="isConverted" class="buttons are-small">
          <a class="button is-link" :href="profile.downloadURL">{{$t('button-download')}}</a>
          <button class="button is-danger" @click="confirmDeletion">{{$t('button-delete')}}</button>
      </div>
      <div v-else-if="isAborted" class="buttons are-small">
        <button class="button" @click="retryComputeProfile">{{$t('button-retry')}}</button>
      </div>
      <div v-else-if="isConverting" class="progress-bar">
        <progress class="progress is-info" :value="profile.progress" max="100">
          {{profile.progress}}%
        </progress>
      </div>
    </template>
  </div>
</template>

<script>
import UploadedFileStatus from '@/components/storage/UploadedFileStatus';
import {UploadedFileStatus as UFStatus, CompanionFileCollection, CompanionFile} from 'cytomine-client';

export default {
  name: 'profile-status',
  components: {UploadedFileStatus},
  props: {
    image: Object
  },
  data() {
    return {
      companionFiles: [],
      error: false,
    };
  },
  computed: {
    eligible() {
      return this.image.dimensions.length === 3;
    },
    profile() {
      return this.companionFiles.find(cf => cf.type === 'HDF5');
    },
    isConverted() {
      return (this.profile) ? this.profile.status === UFStatus.CONVERTED : false;
    },
    isAborted() {
      return (this.profile) ? [UFStatus.ERROR_CONVERSION, UFStatus.ERROR_DEPLOYMENT, UFStatus.ERROR_EXTRACTION].includes(this.profile.status) : false;
    },
    isConverting() {
      return (this.profile) ? this.profile.status === UFStatus.CONVERTING : false;
    }
  },
  watch: {
    async image() {
      await this.fetchCompanionFiles();
    }
  },
  methods: {
    confirmProfileComputation() {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-profile-computation'),
        message: this.$t('confirm-profile-computation-message'),
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.computeProfile()
      });
    },
    async computeProfile() {
      try {
        let cf = await new CompanionFile({image: this.image.id, type: 'HDF5'}).save();
        cf.status = UFStatus.UPLOADED; //TODO: Issue in core: no status returned
        this.companionFiles.push(cf);
        this.$emit('update');
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-profile-computation')});
      }
    },

    confirmDeletion() {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-file'),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteFile()
      });
    },
    async deleteFile() {
      try {
        await CompanionFile.delete(this.profile.id);
        this.$emit('update');
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-profile-deletion')});
      }
    },

    async fetchCompanionFiles() {
      try {
        this.companionFiles = (await CompanionFileCollection.fetchAll({filterKey: 'abstractimage', filterValue: this.image.id})).array;
      } catch (error) {
        console.log(error);
        this.error = true;
      }
    },

    async retryComputeProfile() {
      await this.deleteFile();
      await this.computeProfile();
    }
  },
  created() {
    this.fetchCompanionFiles();
  }
};
</script>

<style scoped>
  .buttons {
    margin-left: 0.8em;
  }

  .profile-status {
    display: flex;
    align-items: center;
  }

  .progress-bar {
    width: 100%;
    margin-left: 1em;
    margin-right: 1em;
  }
</style>

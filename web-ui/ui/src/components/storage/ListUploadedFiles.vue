<!-- Copyright (c) 2009-2021. Authors: see NOTICE file.

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
  <div class="panel">
    <p class="panel-heading">
      {{ $t('storage') }}
    </p>
    <div class="panel-block storage">
      <b-input
        :value="searchString"
        @input="debounceSearchString"
        class="search-uploaded-file"
        :placeholder="$t('search-placeholder')"
        icon="search"
      />

      <cytomine-table
        :collection="uploadedFileCollection"
        sort="created" order="desc"
        :revision="revision"
        :refreshInterval="tableRefreshInterval"
        :openedDetailed.sync="openedDetails"
        :detailed="false"
      >
        <template #default="{row: uFile}">
          <b-table-column :label="$t('preview')" width="80" class="image-overview">
            <image-thumbnail v-if="uFile.thumbURL" :url="uFile.thumbURL" :size="128" :key="uFile.thumbURL" :extra-parameters="{Authorization: 'Bearer ' + shortTermToken }"/>
            <div v-else class="is-size-7 has-text-grey">{{$t('no-preview-available')}}</div>
          </b-table-column>

          <b-table-column field="originalFilename" :label="$t('filename')" sortable width="200">
            {{ uFile.originalFilename }}
          </b-table-column>

          <b-table-column field="created" :label="$t('created')" sortable width="150">
            {{ Number(uFile.created) | moment('lll') }}
          </b-table-column>

          <b-table-column field="size" :label="$t('size')" sortable width="80">
            {{ filesize(uFile.size) }}
          </b-table-column>

          <b-table-column field="status" :label="$t('status')" sortable width="80">
            <uploaded-file-status :file="uFile" />
          </b-table-column>

          <b-table-column label="" width="120">
            <div class="buttons is-right">
              <a class="button is-small is-link" @click="download(uFile.downloadURL)" v-if="uFile.status >= 100">
                {{$t('button-download')}}
              </a>
              <button class="button is-small is-danger" @click="confirmDeletion(uFile)">
                {{$t('button-delete')}}
              </button>
            </div>
          </b-table-column>

        </template>

        <template #empty>
          <p>{{$t('no-uploaded-file')}}</p>
        </template>
      </cytomine-table>
    </div>
  </div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import ImageThumbnail from '@/components/image/ImageThumbnail';
import {UploadedFileCollection, UploadedFile} from 'cytomine-client';
import filesize from 'filesize';
import _ from 'lodash';
import CytomineTable from '@/components/utils/CytomineTable';
import UploadedFileStatusComponent from './UploadedFileStatus';
import {appendShortTermToken} from '@/utils/token-utils';

export default {
  name: 'list-uploaded-files',
  components: {
    CytomineTable,
    ImageThumbnail,
    'uploaded-file-status': UploadedFileStatusComponent
  },
  data() {
    return {
      loading: true,
      searchString: '',
      openedDetails: [],
    };
  },
  props: {
    tableRefreshInterval: {
      type: Number,
      default: 0
    },
    revision : {type: Number, default: 0}
  },
  computed: {
    currentUser: get('currentUser/user'),
    users: get('currentStorage/users'),
    shortTermToken: get('currentUser/shortTermToken'),
    uploadedFileCollection() {
      return new UploadedFileCollection({
        onlyRootsWithDetails: true,
        originalFilename: {ilike: encodeURIComponent(this.searchString)}
      });
    }
  },
  methods: {
    filesize(size) {
      return (size) ? filesize(size, {base: 10}) : null;
    },
    debounceSearchString: _.debounce(async function (value) {
      this.searchString = value;
    }, 500),
    updatedTree() {
      this.$emit('update:revision', this.revision + 1); // updating the table will result in new files objects => the uf details will also be updated
    },
    download(url) {
      window.location.assign(appendShortTermToken(url, this.shortTermToken));
    },
    confirmDeletion(uFile) {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-file'),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteFile(uFile)
      });
    },
    async deleteFile(uFile) {
      try {
        await UploadedFile.delete(uFile.id);
        this.updatedTree();
      } catch (error) {
        console.log(error);
        let errorValues = error.response.data.errorValues;
        let text;
        if (error.response.status === 403) {
          if (errorValues && errorValues.projectNames && errorValues.imageNames) {
            text = this.$t('notif-error-delete-used-uploaded-file', {
              projects: errorValues.projectNames.join(', '),
              names: errorValues.imageNames.join(', ')
            });
          } else {
            text = this.$t('notif-error-delete-uploaded-file-forbidden');
          }
        } else {
          text = this.$t('notif-error-delete-uploaded-file');
        }
        this.$notify({type: 'error', text});
      }
    },
  },
};
</script>

<style scoped>
.small-text {
  font-size: 0.9em;
}

.upload-table  {
  position: relative;
  bottom: 0.4em;
}

.upload-table td {
  vertical-align: middle !important;
}

.upload-table td:first-child {
  width: 20vw;
}

.upload-table td:nth-child(2) {
  width: 10em;
}

.upload-table td:nth-child(3) {
  width: 15vw;
}

.upload-table td:last-child .field {
  justify-content: flex-end;
}

.upload-table td:last-child .control {
  text-align: right;
}

.column.flex-column {
  flex-direction: column;
}

.progress:not(:last-child) {
  margin-bottom: 0.75em;
}

.column:first-child {
  padding-top: 1.25em;
}

.first-child-like {
  display: block;
  padding-top: 0.5em;
}

.image-overview {
  max-height: 4em;
  max-width: 6em;
}

.panel-block.storage {
  min-height: 20vh;
  position: relative;
}
</style>

<style>
.storage-wrapper .upload-draggable .button {
  margin-bottom: 0;
}

.storage-wrapper .upload-draggable {
  margin-right: 0.75em;
  position: relative;
  bottom: 4px;
}

.search-uploaded-file {
  max-width: 25em;
}
</style>


<template>
  <div>
    <b-loading :is-full-page="false" :active="loading" />
    <template v-if="!loading">
      <b-message v-if="!tags" type="is-danger" has-icon icon-size="is-small">
        <h2> {{ $t('error') }} </h2>
        <p> {{ $t('unexpected-error-info-message') }} </p>
      </b-message>
      <template v-else>
        <div class="columns">
          <div class="column is-one-quarter">
            <b-input v-model="searchString" :placeholder="$t('search-placeholder')" type="search" icon="search" />
          </div>

          <div class="column is-one-half has-text-right-desktop">
            <button class="button is-link" @click="startTagCreation()">
              {{$t('button-new-tag')}}
            </button>
          </div>
        </div>

        <b-table
          :data="filteredTags"
          :paginated="true"
          :per-page="perPage"
          pagination-size="is-small"
        >

          <template #default="{row: tag}">
            <b-table-column
              :field="'name'"
              :label="$t('name')"
              sortable
            >
            {{tag.name}}
            </b-table-column>

            <b-table-column
              :field="'creatorName'"
              :label="$t('creator')"
              sortable
            >
              {{tag.creatorName}}
            </b-table-column>

            <b-table-column
              :field="'created'"
              :label="$t('created')"
              sortable
            >
              {{ Number(tag.created) | moment('ll') }}
            </b-table-column>

            <b-table-column label=" " centered>
              <div class="buttons">
                <button class="button is-small is-link" @click="startTagEdition(tag)">
                  {{$t('button-edit')}}
                </button>
                <button class="button is-small is-danger" @click="deleteTagDialog(tag)">
                  {{$t('button-delete')}}
                </button>
              </div>
            </b-table-column>
          </template>

          <template #empty>
            <div class="content has-text-grey has-text-centered">
              <p>{{$t('no-tag-fitting-criteria')}}</p>
            </div>
          </template>

          <template #bottom-left>
            <b-select v-model="perPage" size="is-small">
              <option value="10">{{$t('count-per-page', {count: 10})}}</option>
              <option value="25">{{$t('count-per-page', {count: 25})}}</option>
              <option value="50">{{$t('count-per-page', {count: 50})}}</option>
              <option value="100">{{$t('count-per-page', {count: 100})}}</option>
            </b-select>
          </template>
        </b-table>

        <tag-modal :active.sync="modal" :tag="editedTag" @addTag="addTag" @updateTag="updateTag" />
      </template>
    </template>
  </div>
</template>

<script>
import {TagCollection} from 'cytomine-client';
import {getWildcardRegexp} from '@/utils/string-utils';
import TagModal from '@/components/tag/TagModal';

export default {
  name: 'admin-tags',
  components: {
    TagModal
  },
  data() {
    return {
      loading: true,
      tags: null,
      addTagModal: false,
      searchString: '',
      perPage: 25,
      modal: false,
      editedTag: null
    };
  },
  computed: {
    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    filteredTags() {
      if (!this.searchString) {
        return this.tags;
      }

      return this.tags.filter(ts => this.regexp.test(ts.name));
    }
  },
  methods: {
    startTagCreation() {
      this.editedTag = null;
      this.modal = true;
    },
    addTag(tag) {
      this.tags.push(tag);
    },
    startTagEdition(tag) {
      this.editedTag = tag;
      this.modal = true;
    },
    updateTag(tag) {
      this.editedTag.populate(tag);
    },

    deleteTagDialog(tag) {
      this.$buefy.dialog.confirm({
        title: this.$t('delete'),
        message: this.$t('delete-tag-confirmation-message', {tagName: tag.name}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteTag(tag)
      });
    },
    deleteTag(tag) {
      try {
        tag.delete();
        this.tags.splice(this.tags.indexOf(tag), 1);
        this.$notify({
          type: 'success',
          text: this.$t('notif-success-tag-delete', {tagName: tag.name})
        });
      } catch (error) {
        console.log(error);
        this.$notify({
          type: 'error',
          text: this.$t('notif-error-tag-delete', {tagName: this.currentTag.name})
        });
      }
    },
  },
  async created() {
    try {
      this.tags = (await TagCollection.fetchAll()).array;
    } catch (error) {
      console.log(error);
    }
    this.loading = false;
  }
};
</script>

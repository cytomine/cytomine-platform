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
  <slot name="error">
    <h2> {{ $t('error') }} </h2>
    <p> {{ $t('unexpected-error-info-message') }} </p>
  </slot>
</b-message>
<div v-else class="cytomine-table-wrapper">
  <b-loading :is-full-page="false" :active="!data" />
  <b-table
    v-if="data"
    :data="data"
    :loading="loading"
    paginated
    backend-pagination
    :total="total"
    :per-page="internalPerPage"
    :current-page.sync="internalCurrentPage"
    pagination-size="is-small"
    :detailed="detailed"
    :detail-key="detailKey"
    :opened-detailed="openedDetailed.slice()" @update:openedDetailed="$emit('update:openedDetailed', $event)"
    backend-sorting
    :default-sort="sort"
    :default-sort-direction="order"
    :checkable="checkable"
    :checked-rows.sync="internalCheckedRows"
    :is-row-checkable="isRowCheckable"
    @sort="updateSort"
  >
    <template #header="{column}">
      <slot name="header" :column="column" :index="index">
        {{ column.label }}
      </slot>
    </template>

    <template #default="{row, index}">
      <slot :row="row" :index="index"></slot>
    </template>

    <template #detail="{row, index}">
      <slot name="detail" :row="row" :index="index"></slot>
    </template>

    <template #empty>
      <div class="content has-text-grey has-text-centered">
        <slot name="empty" :row="row" :index="index">
          {{$t('no-result')}}
        </slot>
      </div>
    </template>

    <template #footer>
      <slot name="footer"></slot>
    </template>

    <template #bottom-left>
      <b-select v-model="internalPerPage" size="is-small">
        <option v-for="option in perPageOptions" :key="option" :value="option">
          {{$t('count-per-page', {count: option})}}
        </option>
      </b-select>
    </template>
  </b-table>
</div>
</template>

<script>
export default {
  name: 'cytomine-table',
  props: {
    collection: Object,
    isEmpty: {type: Boolean, default: false},
    perPageOptions: {type: Array, default: () => [5, 10, 25, 50, 100]},
    perPage: {type: Number, default: 25},
    currentPage: {type: Number, default: 1},
    detailed: {type: Boolean, default: true},
    detailKey: {type: String, default: 'id'},
    openedDetailed: {type: Array, default: () => []},
    sort: {type: String},
    order: {type: String, default: 'asc'},
    revision: Number, // updating this value will result in update of table data
    refreshInterval: {type: Number, default: 0}, // if > 0, table data will be refreshed according to refreshInterval (expressed in ms)
    checkable: {type: Boolean, default: false},
    checkedRows: {type: Array, default: () => []},
    isRowCheckable: {type: Function}
  },
  data() {
    return {
      index: 0,
      row: null,

      data: null,
      error: false,
      loading: true,
      total: 0,
      internalCheckedRows: [],
      internalCurrentPage: null,
      internalPerPage: null,
      internalSort: null,
      internalOrder: null,
      timeout: null
    };
  },
  computed: {
    internalCollection() {
      let collection = this.collection.clone();
      collection.max = this.internalPerPage;
      collection.sort = this.internalSort;
      collection.order = this.internalOrder;
      return collection;
    }
  },
  watch: {
    internalCollection() {
      this.fetchPage(true);
    },
    internalCurrentPage(page, previousValue) {
      if (previousValue) { // do not trigger change for value initialization
        this.$emit('update:currentPage', page);
        this.fetchPage(true);
      }
    },
    internalCheckedRows(chckedRows) {
      this.$emit('update:checkedRows', chckedRows);
    },
    internalPerPage(perPage) {
      this.$emit('update:perPage', perPage);
    },
    total(total) {
      this.$emit('setCollectionSize', total);
    },
    revision() {
      this.fetchPage();
    },
    refreshInterval(value) {
      if (value === 0) {
        clearTimeout(this.timeout);
        this.timeout = null;
      } else if (!this.timeout) { // user provided a valid refresh interval, and no fetch page planned => relaunch fetch page
        this.fetchPage();
      }
    }
  },
  methods: {
    updateSort(sort, order) {
      this.internalSort = sort;
      this.internalOrder = order;
      this.$emit('update:sort', sort);
      this.$emit('update:order', order);
    },
    async fetchPage(showLoading = false) {
      if (showLoading) {
        this.loading = true;
      }

      try {
        let data = {array: [], totalNbItems: 0};
        if (this.isEmpty) {
          this.internalCurrentPage = 1;
        } else {
          data = await this.internalCollection.fetchPage(this.internalCurrentPage - 1);
        }

        this.data = data.array;
        this.total = data.totalNbItems;

        //as data is refetched, we need to update the selected rows
        this.internalCheckedRows = this.data.filter(member => {
          return this.internalCheckedRows.map(u => u.id).includes(member.id);
        });

        this.$emit('update:checkedRows', this.internalCheckedRows);
      } catch (error) {
        if (this.internalCurrentPage > 1) { // error may be due to the page number (not enough elements) => retry on first page
          this.internalCurrentPage = 1;
          return;
        }

        console.log(error);
        this.error = true;
        return;
      }

      this.loading = false;

      clearTimeout(this.timeout);
      if (this.refreshInterval) {
        this.timeout = setTimeout(this.fetchPage, this.refreshInterval);
      }
    },
  },
  created() {
    this.internalCurrentPage = this.currentPage;
    this.internalPerPage = this.perPage;
    this.internalSort = this.sort;
    this.internalOrder = this.order;
    this.internalCheckedRows = this.checkedRows;
  }
};
</script>

<style scoped>
.cytomine-table-wrapper {
  min-height: 10em;
  position: relative;
}
</style>

<template>
  <div class="annotation-details">
    <table class="table">
      <tbody>
      <template v-if="isPropDisplayed('creation-info')">
        <tr>
          <td><strong>{{ $t('created-by') }}</strong></td>
          <td>
            {{ creatorName }}
          </td>
        </tr>
        <tr>
          <td><strong>{{ $t('created-on') }}</strong></td>
          <td> {{ Number(annotation.created) | moment('ll') }}</td>
        </tr>
      </template>

      <template v-if="currentUser.isDeveloper">
        <tr>
          <td><strong>{{ $t('id') }}</strong></td>
          <td>{{ annotation.id }}</td>
        </tr>
      </template>
      </tbody>
    </table>

    <div class="actions">
      <a class="button is-link is-small is-fullwidth" @click="$emit('centerView')">
        {{ $t('button-center-view-on-annot') }}
      </a>

      <button class="level-item button is-small" @click="copyURL()">
        {{ $t('button-copy-url') }}
      </button>
    </div>
  </div>
</template>

<script>
import copyToClipboard from 'copy-to-clipboard';
import {get} from '@/utils/store-helpers';

export default {
  name: 'AnnotationSimpleDetails',
  props: {
    annotation: {type: Object, required: true},
  },
  computed: {
    configUI: get('currentProject/configUI'),
    currentUser: get('currentUser/user'),
    annotationURL() {
      return `/project/${this.annotation.project}/image/${this.annotation.image}/annotation/${this.annotation.id}`;
    },
    canEdit() {
      return this.$store.getters['currentProject/canEditAnnot'](this.annotation);
    },
    creatorName() {
      return this.annotation.annotationLayer.name;
    },
  },
  methods: {
    copyURL() {
      copyToClipboard(window.location.origin + '/#' + this.annotationURL);
      this.$notify({type: 'success', text: this.$t('notif-success-annot-URL-copied')});
    },
    isPropDisplayed(prop) {
      return this.configUI[`project-explore-annotation-${prop}`];
    },
  },
};
</script>

<style scoped>
a.is-fullwidth {
  width: auto;
}

.actions {
  margin-bottom: 0.5em;
}

.actions .button {
  margin: 3px;
  box-sizing: border-box;
}

.annotation-details {
  position: relative;
  font-size: 0.85rem;
}

.table {
  width: 100%;
  margin-bottom: 0.7em !important;
  background: transparent;
}

.table th, .table td {
  vertical-align: middle;
}
</style>

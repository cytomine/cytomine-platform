<template>
  <div class="annotations-list-wrapper">
    <div v-show="opened" class="annotations-list-opened">
      <button class="delete" @click="opened = false"></button>

      <div class="annotations-list-sidebar">
        <!--<b-field position="is-centered" v-if="hasTracks">
          <b-radio-button v-model="displayType" native-value="TERM" size="is-small">
            {{$t('by-term')}}
          </b-radio-button>
          <b-radio-button v-model="displayType" native-value="TRACK" size="is-small">
            {{$t('by-track')}}
          </b-radio-button>
        </b-field>-->

        <ontology-tree
          v-if="isDisplayedByTerm"
          v-model="selectedTermsIds"
          :ontology="ontology"
          :multiple-selection="false"
          :hidden-nodes="hiddenTermsIds"
          :additional-nodes="[noTermOption]"
        />

        <track-tree
          v-if="!isDisplayedByTerm"
          v-model="selectedTracksIds"
          :tracks="tracks"
          :multiple-selection="false"
        />
      </div>

      <div class="annotations-list-container">
        <list-annotations-by v-if="currentItem" :key="currentItem.id"
          :categorization="displayType"
          :size="85"
          color="000000"
          :nb-per-page="nbPerPage"

          :prop="currentItem"

          :all-terms="terms"
          :all-users="allUsers"
          :all-images="images"
          :all-tracks="tracks"

          :multiple-terms="false"
          :no-term="noTerm"
          :multiple-track="false"
          :no-track="noTrack"

          :images-ids="[image.id]"
          :slices-ids="sliceIds"
          :users-ids="layersIds"
          :terms-ids="termsOptionsIds"
          :tracks-ids="tracksIds"
          :reviewed="false"

          :visible="opened"
          :index="index"
          :revision="revision"
          :show-details="showDetails"

          @updateTermsOrTracks="$emit('updateTermsOrTracks', $event)"
          @updateProperties="$emit('updateProperties')"
          @centerView="$emit('centerView', {annot: $event, sameView: isSameView($event)})"
          @delete="$emit('delete', $event)"
          @select="select"
        />
      </div>
    </div>

    <div v-show="!opened" class="opener" @click="opened = true">{{$t("annotations-list")}} <i class="fas fa-caret-up"></i></div>
  </div>
</template>

<script>
import {UserCollection} from 'cytomine-client';

import {get} from '@/utils/store-helpers';

import ListAnnotationsBy from '@/components/annotations/ListAnnotationsBy';
import OntologyTree from '@/components/ontology/OntologyTree';
import TrackTree from '@/components/track/TrackTree';

export default {
  name: 'annotations-list',
  components: {
    ListAnnotationsBy,
    OntologyTree,
    TrackTree,
  },
  props: [
    'index',
  ],
  data() {
    return {
      nbPerPage: 10,
      noTermOption: {id: 0, name: this.$t('no-term')},

      users: [],

      revision: 0
    };
  },
  computed: {
    ontology: get('currentProject/ontology'),
    configUI: get('currentProject/configUI'),

    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    images() {
      return [this.image];
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    },
    slices() {
      return this.imageWrapper.activeSlices;
    },
    sliceIds() {
      return this.slices.map(slice => slice.id);
    },

    isDisplayedByTerm() {
      return this.displayType === 'TERM';
    },
    items() {
      return (this.isDisplayedByTerm) ? this.termsOptions : this.tracks;
    },
    currentItem() {
      if (this.isDisplayedByTerm) {
        return this.termsOptions.find(term => term.id === this.selectedTermId);
      } else {
        return this.tracks.find(track => track.id === this.selectedTrackId);
      }
    },

    additionalNodes() {
      return [this.noTermOption];
    },
    terms() {
      return this.$store.getters['currentProject/terms'] || [];
    },
    hiddenTermsIds() {
      return this.$store.getters[this.imageModule + 'hiddenTermsIds'];
    },
    termsOptions() {
      return [...this.terms, ...this.additionalNodes].filter(term => !this.hiddenTermsIds.includes(term.id));
    },
    termsOptionsIds() {
      return this.termsOptions.map(term => term.id);
    },

    selectedTermId() {
      return (this.selectedTermsIds.length > 0) ? this.selectedTermsIds[0] : null;
    },
    noTerm() {
      return this.isDisplayedByTerm ? this.selectedTermId === this.noTermOption.id : this.termsOptionsIds.includes(this.noTermOption.id);
    },

    tracks() {
      return this.imageWrapper.tracks.tracks;
    },
    tracksIds() {
      let optionsIds = this.tracks.map(track => track.id);
      optionsIds.push(0); // Add 0 for "no track" option
      return optionsIds;
    },
    hasTracks() {
      return this.tracks.length > 0;
    },
    selectedTrackId() {
      return (this.selectedTracksIds.length > 0) ? this.selectedTracksIds[0] : null;
    },
    noTrack() {
      return this.isDisplayedByTerm;
    },

    layers() {
      let layers = this.imageWrapper.layers.selectedLayers || [];
      layers = layers.filter(layer => layer.visible);
      return layers;
    },
    layersIds() {
      return this.layers.map(layer => layer.id);
    },
    allUsers() {
      return this.users.concat(this.userJobs);
    },

    opened: {
      get() {
        return this.imageWrapper.annotationsList.open;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setShowAnnotationsList', value);
      }
    },

    displayType: {
      get() {
        return this.imageWrapper.annotationsList.displayType;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setDisplayType', value);
      }
    },

    selectedTermsIds: {
      get() {
        return this.imageWrapper.annotationsList.selectedTermsIds;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setSelectedTermsIds', value);
      }
    },

    selectedTracksIds: {
      get() {
        return this.imageWrapper.annotationsList.selectedTracksIds;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setSelectedTracksIds', value);
      }
    },

    showDetails() {
      return this.configUI['project-explore-annotation-main'];
    }
  },
  watch: {
    hasTracks(value) {
      if (!value) {
        this.displayType = 'TERM';
      }
    }
  },
  methods: {
    async fetchUsers() { // TODO in vuex (project module)
      this.users = (await UserCollection.fetchAll()).array;
    },
    addAnnotationHandler(annotation) {
      if (annotation.image === this.image.id) {
        this.revision++;
      }
    },
    reloadAnnotationsHandler(idImage) {
      if (idImage === null || idImage === this.image.id) {
        this.revision++;
      }
    },
    editAnnotationHandler(updatedAnnot) {
      if (updatedAnnot.image === this.image.id) {
        this.revision++;
      }
    },
    deleteAnnotationHandler(deletedAnnot) {
      if (deletedAnnot.image === this.image.id) {
        this.revision++;
      }
    },
    isSameView(annot) {
      return this.displayType === 'TERM' && this.sliceIds.includes(annot.slice);
    },
    select({annot, options}) {
      this.$emit('select', {annot, options: {trySameView: options.trySameView || this.isSameView(annot)}});
    },

    shortkeyHandler(key) {
      if (!this.isActiveImage) {
        return;
      }

      if (key === 'toggle-annotations') {
        this.opened = !this.opened;
      }
    }
  },
  async created() {
    if (this.selectedTermsIds.length === 0) {
      this.selectedTermsIds = (this.termsOptionsIds.length > 0) ? [this.termsOptionsIds[0]] : [];
    }
    if (this.selectedTracksIds.length === 0) {
      this.selectedTracksIds = (this.hasTracks) ? [this.tracks[0].id] : [];
    }

    await this.fetchUsers();
  },
  mounted() {
    this.$eventBus.$on('addAnnotation', this.addAnnotationHandler);
    this.$eventBus.$on('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$on('editAnnotation', this.editAnnotationHandler);
    this.$eventBus.$on('deleteAnnotation', this.deleteAnnotationHandler);
    this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
  },
  beforeDestroy() {
    // unsubscribe from all events
    this.$eventBus.$off('addAnnotation', this.addAnnotationHandler);
    this.$eventBus.$off('reloadAnnotations', this.reloadAnnotationsHandler);
    this.$eventBus.$off('editAnnotation', this.editAnnotationHandler);
    this.$eventBus.$off('deleteAnnotation', this.deleteAnnotationHandler);
    this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
  }
};
</script>

<style scoped>
.annotations-list-opened {
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  background: #f5f5f5;
  height: 25vh;
  pointer-events: auto;
  display: flex;
}

.delete {
  position: absolute;
  right: 25px;
  top: 7px;
  z-index: 10;
}

.annotations-list-container {
  overflow: auto;
  position: relative;
  border-bottom: 1px solid #ccc;
  height: 100%;
  width: 100%;
  padding-right: 25px;
}

.opener {
  background: #f5f5f5;
  width: 150px;
  border-radius: 5px 5px 0px 0px;
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  margin: auto;
  text-align: center;
  text-transform: uppercase;
  font-size: 10px;
  letter-spacing: 0.3px;
  cursor: pointer;
  pointer-events: auto;
}

.opener .fas {
  margin-left: 5px;
  font-size: 12px;
  line-height: 10px;
}

.box {
  background: unset;
  border-radius: unset;
  box-shadow: unset;
  padding: 0.75rem;
  height: 100%;
}

>>> h2 {
  margin-bottom: 0;
}

.annotations-list-sidebar {
  padding: 10px;
  overflow-y: auto;
  min-width: 18em;
}

>>> ul.pagination-list {
  justify-content: flex-end;
}
</style>

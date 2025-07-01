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
<div class="draw-tools-wrapper">
  <div class="buttons has-addons are-small" v-if="isToolDisplayed('select')">
    <button
      v-tooltip="$t('select')"
      class="button"
      :class="{'is-selected': activeTool === 'select'}"
      @click="activateTool('select')"
    >
      <span class="icon is-small"><i class="fas fa-mouse-pointer"></i></span>
    </button>
  </div>

  <template v-if="!reviewMode"> <!-- drawing new annotations forbidden while reviewing -->
    <div
      v-if="terms && terms.length > 0"
      class="buttons has-addons are-small term-selection"
      :class="{'has-preview': termsToAssociate.length > 0}"
      v-click-outside="() => showTermSelector = false"
    >
      <button
        v-tooltip="$t('terms-new-annotation')"
        class="button"
        :disabled="disabledDraw"
        @click="showTermSelector = !showTermSelector"
        :title="disabledDrawMessage"
      >
        <span class="icon is-small"><i class="fas fa-hashtag"></i></span>
      </button>

      <div class="color-preview" :style="{background: backgroundTermsNewAnnot}">
        <span v-if="termsToAssociate.length > 1">{{termsToAssociate.length}}</span>
      </div>

      <div class="ontology-tree-container" v-show="showTermSelector">
        <b-input v-model="searchStringTerm" :placeholder="$t('search-placeholder')" size="is-small" />
        <ontology-tree
          class="ontology-tree"
          v-model="termsToAssociate"
          :ontology="ontology"
          :searchString="searchStringTerm"
        />
      </div>
    </div>
    <div
      v-if="tracks && maxRank > 1"
      class="buttons has-addons are-small track-selection"
      :class="{'has-preview': tracksToAssociate.length > 0}"
      v-click-outside="() => showTrackSelector = false"
    >
      <button
        v-tooltip="$t('tracks-new-annotation')"
        class="button"
        :disabled="disabledDraw"
        @click="showTrackSelector = !showTrackSelector"
        :title="disabledDrawMessage"
      >
        <span class="icon is-small"><i class="fas fa-route"></i></span>
      </button>

      <div class="color-preview" :style="{'border-color': backgroundTracksNewAnnot}">
        <span v-if="tracksToAssociate.length > 1">{{tracksToAssociate.length}}</span>
      </div>

      <div class="tracks-tree-container" v-show="showTrackSelector">
        <b-input v-model="searchStringTrack" :placeholder="$t('search-placeholder')" size="is-small" />
        <track-tree
          class="track-tree"
          v-model="tracksToAssociate"
          :tracks="tracks"
          :searchString="searchStringTrack"
          :allow-new="true"
          :allow-edition="true"
          :image="image"
          @newTrack="refreshTracks"
          @updatedTrack="refreshTracks"
          @deletedTrack="refreshTracks"
        />
      </div>
    </div>

    <div class="buttons has-addons are-small">
      <button
        v-if="isToolDisplayed('point')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('point')"
        class="button"
        :class="{'is-selected': activeTool === 'point'}"
        @click="activateTool('point')"
      >
        <span class="icon is-small"><i class="fas fa-map-pin"></i></span>
      </button>

      <button
        v-if="isToolDisplayed('line')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('line')"
        class="button"
        :class="{'is-selected': activeTool === 'line'}"
        @click="activateTool('line')"
      >
        <span class="icon is-small"><i class="fas fa-minus"></i></span>
      </button>

      <button
        v-if="isToolDisplayed('freehand-line')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('freehand-line')"
        class="button"
        :class="{'is-selected': activeTool === 'freehand-line'}"
        @click="activateTool('freehand-line')"
      >
        <span class="icon is-small">
          <icon-line-free-hand />
        </span>
      </button>

      <!-- QUESTION: redefine expected behaviour
      <button class="button" :disabled="disabledDraw" title="Arrow"
              @click="activateTool('arrow')" :class="{'is-selected': activeTool === 'arrow'}">
          <span class="icon is-small"><i class="fas fa-long-arrow-right"></i></span>
      </button> -->

      <button
        v-if="isToolDisplayed('rectangle')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('rectangle')"
        class="button"
        :class="{'is-selected': activeTool === 'rectangle'}"
        @click="activateTool('rectangle')"
      >
        <span class="icon is-small"><i class="far fa-square"></i></span>
      </button>

      <button
        v-if="isToolDisplayed('circle')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('circle')"
        class="button"
        :class="{'is-selected': activeTool === 'circle'}"
        @click="activateTool('circle')"
      >
        <span class="icon is-small"><i class="far fa-circle"></i></span>
      </button>

      <button
        v-if="isToolDisplayed('polygon')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('polygon')"
        class="button"
        :class="{'is-selected': activeTool === 'polygon'}"
        @click="activateTool('polygon')"
      >
        <span class="icon is-small"><i class="fas fa-draw-polygon"></i></span>
      </button>

      <button
        v-if="isToolDisplayed('freehand-polygon')"
        :disabled="disabledDraw"
        :title="disabledDrawMessage"
        v-tooltip="$t('freehand-polygon')"
        class="button"
        :class="{'is-selected': activeTool === 'freehand-polygon'}"
        @click="activateTool('freehand-polygon')"
      >
        <span class="icon is-small">
          <icon-polygon-free-hand />
        </span>
      </button>
    </div>
  </template>

  <template v-else>
    <div class="buttons has-addons are-small">
      <button
        :disabled="isToolDisabled('accept')"
        v-tooltip="$t('validate-annotation')"
        class="button"
        :class="{'is-success': !isToolDisabled('accept')}"
        @click="accept()"
      >
      <span class="icon is-small"><i class="fas fa-check"></i></span>
    </button>
    <button
        :disabled="isToolDisabled('reject')"
        v-tooltip="$t('reject-annotation')"
        class="button"
        :class="{'is-danger': !isToolDisabled('reject')}"
        @click="reject()"
      >
      <span class="icon is-small"><i class="fas fa-minus"></i></span>
    </button>
    </div>
  </template>

  <div class="buttons has-addons are-small">
      <button
      v-if="isToolDisplayed('screenshot')"
      :disabled="disabledDraw"
      v-tooltip="$t('screenshot')"
      class="button"
      @click="takeScreenshot()"
    >
      <span class="icon is-small"><i class="fas fa-camera"></i></span>
    </button>
  </div>

  <div v-if="configUI['project-explore-annotation-main']" class="buttons has-addons are-small">
    <button
      :disabled="!selectedFeature"
      v-tooltip="$t('display-annot-details')"
      class="button" :class="{'is-selected': displayAnnotDetails && selectedFeature}"
      @click="displayAnnotDetails = !displayAnnotDetails"
    >
      <span class="icon is-small"><i class="fas fa-info"></i></span>
    </button>
  </div>

  <div class="buttons has-addons are-small">
    <button
      v-if="isToolDisplayed('fill')"
      :disabled="isToolDisabled('fill')"
      v-tooltip="$t('fill')"
      class="button"
      @click="fill()"
    >
      <span class="icon is-small"><i class="fas fa-fill"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('edit')"
      :disabled="isToolDisabled('edit')"
      v-tooltip="$t('modify')"
      class="button"
      :class="{'is-selected': activeEditTool === 'modify'}"
      @click="activateEditTool('modify')"
    >
      <span class="icon is-small"><i class="fas fa-edit"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('union')"
      :disabled="isToolDisabled('correct-add')"
      v-tooltip="$t('freehand-correct-add')"
      class="button"
      :class="{'is-selected': activeEditTool === 'correct-add'}"
      @click="activateEditTool('correct-add')"
    >
      <span class="icon is-small"><i class="superscript fas fa-plus"></i><i class="fas fa-pencil-alt"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('diff')"
      :disabled="isToolDisabled('correct-remove')"
      v-tooltip="$t('freehand-correct-remove')"
      class="button"
      :class="{'is-selected': activeEditTool === 'correct-remove'}"
      @click="activateEditTool('correct-remove')"
    >
      <span class="icon is-small"><i class="superscript fas fa-minus"></i><i class="fas fa-pencil-alt"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('move')"
      :disabled="isToolDisabled('move')"
      v-tooltip="$t('move')"
      class="button"
      :class="{'is-selected': activeEditTool === 'translate'}"
      @click="activateEditTool('translate')"
    >
      <span class="icon is-small"><i class="fas fa-arrows-alt"></i></span>
    </button>

    <button
        v-if="isToolDisplayed('resize')"
        :disabled="isToolDisabled('resize')"
        v-tooltip="$t('rescale')"
        class="button"
        :class="{'is-selected': activeEditTool === 'rescale'}"
        @click="activateEditTool('rescale')"
    >
      <span class="icon is-small"><i class="fas fa-expand"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('rotate')"
      :disabled="isToolDisabled('rotate')"
      v-tooltip="$t('rotate')"
      class="button"
      :class="{'is-selected': activeEditTool === 'rotate'}"
      @click="activateEditTool('rotate')"
    >
      <span class="icon is-small"><i class="fas fa-sync-alt"></i></span>
    </button>

    <button
      v-if="isToolDisplayed('delete')"
      :disabled="isToolDisabled('delete')"
      v-tooltip="$t('delete')"
      class="button"
      @click="confirmDeletion()"
    >
      <span class="icon is-small"><i class="far fa-trash-alt"></i></span>
    </button>
  </div>

  <div class="buttons has-addons are-small" v-if="isToolDisplayed('copy-paste')">
    <button
      :disabled="isToolDisabled('copy')"
      v-tooltip="$t('copy')"
      class="button"
      @click="copy()"
    >
      <span class="icon is-small"><i class="fas fa-copy"></i></span>
    </button>
    <button
      :disabled="disabledPaste"
      v-tooltip="$t('paste')"
      class="button"
      @click="paste()"
    >
      <span class="icon is-small"><i class="fas fa-paste"></i></span>
    </button>
    <div class="special-paste-selection" v-click-outside="() => showRepeatSelector = false" v-if="maxRepeats > 0">
      <button
        :disabled="disabledPaste"
        v-tooltip="$t('paste-repeat')"
        class="button"
        @click="showRepeatSelector = !showRepeatSelector"
      >
      <span class="icon is-small">
        <i class="fas fa-paste"></i>
        <i class="fas fa-star special-paste-icon"></i>
      </span>
      </button>

      <div class="special-paste-container" v-show="showRepeatSelector">
        <div class="panel">
          <div class="panel-block">
            <p>
              <i18n path="paste-repeat-info">
                <input v-model="nbRepeats" type="number" place="input" class="repeat-input" min="1" :max="maxRepeats"/>
              </i18n>
              <br>
            </p>
            <div class="repeat-button-container">
              <button class="button is-small" @click="repeat()">{{$t('paste-repeat')}}</button>
            </div>
          </div>
        </div>


      </div>
    </div>
    <div class="special-paste-selection" v-click-outside="() => showPasteAndLinkModal = false" v-if="isInImageGroup">
      <button
        :disabled="disabledPaste || !linkableCopiedAnnot"
        v-tooltip="$t('paste-with-link')"
        class="button"
        @click="openPasteWithLinkModal()"
      >
        <span class="icon is-small">
          <i class="fas fa-paste"></i>
          <i class="fas fa-link special-paste-icon"></i>
        </span>
      </button>
    </div>
  </div>

  <div class="buttons has-addons are-small"
       v-if="isInImageGroup && (isToolDisplayed('link') || isToolDisplayed('unlink'))">
    <div class="special-paste-selection" v-click-outside="() => showAnnotationLinkSelector = false">
      <button
          :disabled="isToolDisabled('link')"
          v-tooltip="$t('link')"
          class="button"
          :class="{'is-selected': showAnnotationLinkSelector}"
          @click="showAnnotationLinkSelector = !showAnnotationLinkSelector"
      >
        <span class="icon is-small"><i class="fas fa-link"></i></span>
      </button>
      <div class="special-paste-container" v-if="showAnnotationLinkSelector">
        <annotation-link-selector :index="index" />
      </div>
    </div>
    <button
        :disabled="isToolDisabled('unlink')"
        v-tooltip="$t('unlink')"
        class="button"
        @click="confirmUnlink()"
    >
      <span class="icon is-small"><icon-unlink-annotations /></span>
    </button>
  </div>

  <div v-if="isToolDisplayed('undo-redo')" class="buttons has-addons are-small">
    <button
      :disabled="actions.length === 0"
      v-tooltip="$t('undo')"
      class="button"
      @click="undo()"
    >
      <span class="icon is-small"><i class="fas fa-undo"></i></span>
    </button>

    <button
      :disabled="undoneActions.length === 0"
      v-tooltip="$t('redo')"
      class="button"
      @click="redo()"
    >
      <span class="icon is-small"><i class="fas fa-redo"></i></span>
    </button>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import OntologyTree from '@/components/ontology/OntologyTree';
import TrackTree from '@/components/track/TrackTree';
import IconPolygonFreeHand from '@/components/icons/IconPolygonFreeHand';
import IconLineFreeHand from '@/components/icons/IconLineFreeHand';
import IconUnlinkAnnotations from '@/components/icons/IconUnlinkAnnotations';
import PasteAnnotationWithLinkModal from '@/components/viewer/interactions/PasteAnnotationWithLinkModal';
import AnnotationLinkSelector from '@/components/viewer/interactions/AnnotationLinkSelector';

import WKT from 'ol/format/WKT';
import {containsExtent, getCenter, getIntersection} from 'ol/extent';

import {Cytomine, Annotation, AnnotationType, AnnotationLink} from 'cytomine-client';
import {
  Action, updateTermProperties, updateTrackProperties, updateAnnotationLinkProperties,
  listAnnotationsInGroup
} from '@/utils/annotation-utils';


export default {
  name: 'draw-tools',
  components: {
    AnnotationLinkSelector,
    TrackTree,
    OntologyTree,
    IconPolygonFreeHand,
    IconLineFreeHand,
    IconUnlinkAnnotations
  },
  props: {
    index: String
  },
  data() {
    return {
      showTermSelector: false,
      format: new WKT(),
      searchStringTerm: '',
      showTrackSelector: false,
      searchStringTrack: '',
      showRepeatSelector: false,
      showAnnotationLinkSelector: false,
      showPasteAndLinkModal: false,
      nbRepeats: 2,
    };
  },
  computed: {
    configUI: get('currentProject/configUI'),
    ontology: get('currentProject/ontology'),
    currentUser: get('currentUser/user'),

    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageWrapper() {
      return this.viewerWrapper.images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    imageGroupId() {
      return this.$store.getters[this.imageModule + 'imageGroupId'];
    },
    slice() {
      // Cannot draw on multiple slices at same time
      return (this.imageWrapper.activeSlices) ? this.imageWrapper.activeSlices[0] : null;
    },
    multipleActiveSlices() {
      return this.imageWrapper.activeSlices && this.imageWrapper.activeSlices.length > 1;
    },
    maxRank() {
      return this.$store.getters[this.imageModule + 'maxRank'];
    },
    terms() {
      return this.$store.getters['currentProject/terms'];
    },
    termsToAssociate: {
      get() {
        return this.imageWrapper.draw.termsNewAnnots;
      },
      set(terms) {
        this.$store.commit(this.imageModule + 'setTermsNewAnnots', terms);
      }
    },
    backgroundTermsNewAnnot() {
      if (this.termsToAssociate.length === 1) {
        return this.terms.find(term => this.termsToAssociate[0] === term.id).color;
      } else {
        return '#e2e2e2';
      }
    },
    tracks() {
      return this.imageWrapper.tracks.tracks;
    },
    tracksToAssociate: {
      get() {
        return this.imageWrapper.draw.tracksNewAnnots;
      },
      set(tracks) {
        this.$store.commit(this.imageModule + 'setTracksNewAnnots', tracks);
      }
    },
    isInImageGroup() {
      return this.imageGroupId !== null;
    },
    backgroundTracksNewAnnot() {
      if (this.tracksToAssociate.length === 1) {
        return this.tracks.find(track => this.tracksToAssociate[0] === track.id).color;
      } else {
        return '#e2e2e2';
      }
    },
    activeTool: {
      get() {
        return this.imageWrapper.draw.activeTool;
      },
      set(tool) {
        this.$store.dispatch(this.imageModule + 'activateTool', tool);
      }
    },
    displayAnnotDetails: {
      get() {
        return this.imageWrapper.selectedFeatures.displayAnnotDetails;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setDisplayAnnotDetails', value);
      }
    },
    activeEditTool: {
      get() {
        return this.imageWrapper.draw.activeEditTool;
      },
      set(tool) {
        this.$store.commit(this.imageModule + 'activateEditTool', tool);
      }
    },
    selectedFeature() {
      return this.$store.getters[this.imageModule + 'selectedFeature'];
    },
    layers() {
      return this.imageWrapper.layers.selectedLayers || [];
    },
    noActiveLayer() {
      return !this.layers.find(layer => layer.drawOn);
    },
    disabledDraw() {
      return this.noActiveLayer || this.multipleActiveSlices;
    },
    disabledDrawMessage() {
      return (this.multipleActiveSlices) ? this.$t('warning-cannot-draw-multiple-slices') : null;
    },
    actions() {
      return this.imageWrapper.undoRedo.actions;
    },
    undoneActions() {
      return this.imageWrapper.undoRedo.undoneActions;
    },
    isActiveImage() {
      return this.viewerWrapper.activeImage === this.index;
    },
    reviewMode() {
      return this.imageWrapper.review.reviewMode;
    },
    copiedAnnot: {
      get() {
        return this.viewerWrapper.copiedAnnot;
      },
      set(annot) {
        this.$store.commit(this.viewerModule + 'setCopiedAnnot', annot);
      }
    },
    copiedAnnotImageInstance: {
      get() {
        return this.viewerWrapper.copiedAnnotImageInstance;
      },
      set(image) {
        this.$store.commit(this.viewerModule + 'setCopiedAnnotImageInstance', image);
      }
    },
    linkableCopiedAnnot() {
      return this.isInImageGroup && this.copiedAnnot && this.copiedAnnot.imageGroup === this.imageGroupId;
    },
    disabledPaste() {
      return this.disabledDraw || !this.copiedAnnot;
    },
    imageExtent() {
      return [0, 0, this.image.width, this.image.height];
    },
    maxRepeats() {
      return this.maxRank - this.slice.rank - 1;
    }
  },
  watch: {
    noActiveLayer(value) {
      if (value) {
        this.activeTool = 'select';
      }
    },
    selectedFeature(feature) {
      // disable correct tools if no feature is selected
      if (!feature && ['correct-add', 'correct-remove'].includes(this.activeEditTool)) {
        this.activeEditTool = null;
      }
    },
    reviewMode(value) {
      if (value) {
        this.activeTool = 'select';
      }
    }
  },
  methods: {
    isToolDisplayed(tool) {
      return this.configUI[`project-tools-${tool}`];
    },

    isToolDisabled(tool) {
      if (!this.selectedFeature) {
        return true; // no feature selected -> all edit tools disabled
      }

      if (tool === 'copy') {
        return false;
      }

      if (tool === 'link') {
        return !this.isInImageGroup;
      }

      let ftr = this.selectedFeature;
      let annot = ftr.properties.annot;
      if (tool === 'unlink') {
        return annot.group === null;
      }
      if (tool === 'accept') {
        return annot.type === AnnotationType.REVIEWED;
      }
      if (tool === 'reject') {
        return annot.type !== AnnotationType.REVIEWED;
      }

      if (!this.$store.getters['currentProject/canEditAnnot'](annot)) {
        return true;
      }

      let geomType = ftr.geometry.type;
      if (geomType === 'Point') {
        return (tool !== 'move' && tool !== 'delete'); // disable all tools except move and delete for points
      } else if (geomType === 'LineString') {
        return ['fill', 'correct-add', 'correct-remove'].includes(tool); // disable fill and correct tools for lines
      } else {
        return false;
      }
    },

    activateTool(tool) {
      this.activeTool = tool;
    },

    activateEditTool(tool) {
      this.activeTool = 'select';
      this.activeEditTool = (this.activeEditTool === tool) ? null : tool; // toggle behaviour
    },

    async fill() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.activateEditTool(null);
      let annot = feature.properties.annot.clone();

      try {
        await annot.fill();
        this.$eventBus.$emit('editAnnotation', annot);
        this.$eventBus.$emit('reloadAnnotationCrop', annot);
        this.$store.commit(this.imageModule + 'addAction', {annot, type: Action.UPDATE});
      } catch (err) {
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-fill')});
      }
    },

    async deleteAnnot() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.activateEditTool(null);

      try {
        let annot = feature.properties.annot;
        await Annotation.delete(annot.id);
        if (annot.group) {
          let editedAnnots = [];
          if (annot.annotationLink.length === 2) {
            // If there were 2 links, the group has been deleted by backend
            let otherId = annot.annotationLink.filter(al => al.annotation !== annot.id)[0].annotation;
            let other = await Annotation.fetch(otherId);
            other.imageGroup = annot.imageGroup;
            await updateTermProperties(other);
            await updateTrackProperties(other);
            await updateAnnotationLinkProperties(other);

            editedAnnots = [other];
          } else {
            editedAnnots = await listAnnotationsInGroup(annot.project, annot.group);
          }
          editedAnnots.forEach(a => {
            this.$eventBus.$emit('editAnnotation', a);
            if (this.copiedAnnot && a.id === this.copiedAnnot.id) {
              let copiedAnnot = this.copiedAnnot.clone();
              copiedAnnot.annotationLink = a.annotationLink;
              copiedAnnot.group = a.group;
              this.copiedAnnot = copiedAnnot;
            }
          });
        }
        this.$eventBus.$emit('deleteAnnotation', annot);
        this.$store.commit(this.imageModule + 'addAction', {annot: annot, type: Action.DELETE});
      } catch (err) {
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-deletion')});
      }
    },
    confirmDeletion() {
      if (!this.selectedFeature) {
        return;
      }

      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-annotation'),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteAnnot()
      });
    },

    openPasteWithLinkModal() {
      this.$buefy.modal.open({
        component: PasteAnnotationWithLinkModal,
        parent: this,
        hasModalCard: true,
        props: {
          index: this.index
        }
      });
    },

    copy() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.copiedAnnotImageInstance = this.image;
      this.copiedAnnot = feature.properties.annot.clone();
      this.$notify({type: 'success', text: this.$t('notif-success-annotation-copy')});
    },
    convertLocation(copiedAnnot, destImage) {
      /* If we want to paste in the same image but in another slice */
      if (destImage.id === copiedAnnot.image) {
        return copiedAnnot.location;
      }

      let geometry = new WKT().readGeometry(copiedAnnot.location);
      let wrapper = this.imageWrapper;
      let centerExtent = getCenter(geometry.getExtent());

      /* Translate the original location of the annotation to the center of the current FOV */
      geometry.translate(wrapper.view.center[0] - centerExtent[0], wrapper.view.center[1] - centerExtent[1]);

      /* Compute the rescaling factors if the resolution is known for both images */
      let scaleX = 1;
      let scaleY = 1;
      let srcImage = this.copiedAnnotImageInstance;
      let hasPhysicalSizeX = srcImage.physicalSizeX !== null && destImage.physicalSizeX !== null;
      let hasPhysicalSizeY = srcImage.physicalSizeY !== null && destImage.physicalSizeY !== null;

      if (hasPhysicalSizeX && hasPhysicalSizeY) {
        scaleX = srcImage.physicalSizeX / destImage.physicalSizeX;
        scaleY = srcImage.physicalSizeY / destImage.physicalSizeY;
      } else if (hasPhysicalSizeX) {
        scaleX = srcImage.physicalSizeX / destImage.physicalSizeX;
        scaleY = scaleX;
      }

      /* Rescale the annotation */
      geometry.scale(scaleX, scaleY);

      /* Rescale the annotation if it is larger than the destination image size */
      let annotExtent = geometry.getExtent();
      let annotWidth = annotExtent[2] - annotExtent[0];
      let annotHeight = annotExtent[3] - annotExtent[1];
      if (annotWidth > destImage.width || annotHeight > destImage.height) {
        let scale = annotHeight > annotWidth ? annotWidth / annotHeight : annotHeight / annotWidth;
        geometry.scale(scale);
      }

      /* Check if the translation is within the image boundaries */
      let imageExtent = [0, 0, destImage.width, destImage.height];
      if (!containsExtent(imageExtent, geometry.getExtent())) {
        let geomExtent = geometry.getExtent();
        /* Get the part of annotation within the boundaries */
        let intersection = getIntersection(imageExtent, geomExtent);

        /* Get the difference between the parts inside and outside the image boundaries */
        let difference = [];
        for (let i = 0; i < intersection.length; i++) {
          difference[i] = intersection[i] - geomExtent[i];
        }

        /* Translate the difference to have the complete annotation inside the image boundaries */
        geometry.translate(difference[0] + difference[2], difference[1] + difference[3]);
      }

      return new WKT().writeGeometry(geometry);
    },
    async paste() {
      if (!this.copiedAnnot) {
        return;
      }

      /* Convert the location if it is needed */
      let location = this.convertLocation(this.copiedAnnot, this.image);
      if (!location) {
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-paste')});
        return;
      }

      let annot = new Annotation({
        location: location,
        image: this.image.id,
        slice: this.slice.id,
        user: this.currentUser.id,
        term: this.copiedAnnot.term,
        track: this.copiedAnnot.track
      });

      try {
        await annot.save();
        // TODO in backend: response should include userByTerm and correct value for term
        // (https://github.com/cytomine/Cytomine-core/issues/1143)
        annot.term = this.copiedAnnot.term.slice();
        annot.userByTerm = this.copiedAnnot.term.map(term => {
          return {term, user: [this.currentUser.id]};
        });
        annot.imageGroup = this.imageGroupId;
        // ----

        this.$eventBus.$emit('addAnnotation', annot);
        this.$eventBus.$emit('selectAnnotation', {index: this.index, annot});
        this.$store.commit(this.imageModule + 'addAction', {annot, type: Action.CREATE});
      } catch (err) {
        console.log(err);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-creation')});
      }
    },
    async repeat() {
      if (!this.copiedAnnot) {
        return;
      }

      try {
        await this.copiedAnnot.repeat(this.slice.id, this.nbRepeats);
        this.$eventBus.$emit('reloadAnnotations', {idImage: this.image.id});
        this.$notify({type: 'success', text: this.$t('notif-success-annotation-repeat')});
      } catch (err) {
        console.log(err);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-repeat')});
      }
    },

    confirmUnlink() {
      if (!this.selectedFeature) {
        return;
      }

      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-annotation-link'),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.unlink()
      });
    },
    async unlink() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.activateEditTool(null);

      try {
        let annot = feature.properties.annot;
        if (!annot.group) {
          return;
        }
        await AnnotationLink.delete(annot.id, annot.group);
        let updatedAnnot = annot.clone();
        await updateAnnotationLinkProperties(updatedAnnot);

        let editedAnnots = [];
        if (annot.annotationLink.length === 2) {
          // If there were 2 links, the group has been deleted by backend
          let otherId = annot.annotationLink.filter(al => al.annotation !== annot.id)[0].annotation;
          let other = await Annotation.fetch(otherId);
          other.imageGroup = annot.imageGroup;
          await updateTermProperties(other);
          await updateTrackProperties(other);
          await updateAnnotationLinkProperties(other);

          editedAnnots = [updatedAnnot, other];
        } else {
          editedAnnots = [updatedAnnot, ...(await listAnnotationsInGroup(annot.project, annot.group))];
        }

        editedAnnots.forEach(annot => {
          this.$eventBus.$emit('editAnnotation', annot);
          if (this.copiedAnnot && annot.id === this.copiedAnnot.id) {
            let copiedAnnot = this.copiedAnnot.clone();
            copiedAnnot.annotationLink = annot.annotationLink;
            copiedAnnot.group = annot.group;
            this.copiedAnnot = copiedAnnot;
          }
        });
        this.$notify({type: 'success', text: this.$t('notif-success-annotation-link-deletion')});
      } catch (err) {
        console.log(err);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-link-deletion')});
      }
    },


    async undo() {
      if (this.actions.length === 0) {
        return;
      }

      let action = this.actions[this.actions.length - 1];
      try {
        let opposedAction = await this.reverseAction(action, true);
        this.$store.commit(this.imageModule + 'undoAction', opposedAction);
      } catch (err) {
        console.log(err);
        this.$notify({type: 'error', text: this.$t('notif-error-undo')});
      }
    },
    async redo() {
      if (this.undoneActions.length === 0) {
        return;
      }

      let action = this.undoneActions[this.undoneActions.length - 1];
      try {
        let opposedAction = await this.reverseAction(action, false);
        this.$store.commit(this.imageModule + 'redoAction', opposedAction);
      } catch (err) {
        console.log(err);
        this.$notify({type: 'error', text: this.$t('notif-error-redo')});
      }
    },
    async reverseAction({annot, type, command}, undo) {
      let newType = type;

      if (type === Action.CREATE) {
        await Annotation.delete(annot.id);
        this.$eventBus.$emit('deleteAnnotation', annot);
        newType = Action.DELETE;
        command = Cytomine.instance.lastCommand;
      } else if (type === Action.DELETE) {
        let collection = await Cytomine.instance.undo(command); // always undo if annotation was deleted
        let newAnnot = await this.getUpdatedAnnotation(collection);
        this.$eventBus.$emit('addAnnotation', newAnnot);
        newType = Action.CREATE;
      } else { // annotation was updated
        let collection;
        if (undo) {
          collection = await Cytomine.instance.undo(command);
        } else {
          collection = await Cytomine.instance.redo(command);
        }
        let newAnnot = await this.getUpdatedAnnotation(collection);
        this.$eventBus.$emit('editAnnotation', newAnnot);
      }

      return {annot, command, type: newType};
    },
    async getUpdatedAnnotation(collection) {
      for (let model of collection) {
        let jsonAnnot = model.annotation || model.reviewedannotation;
        if (jsonAnnot) {
          let annot = new Annotation(jsonAnnot);
          annot.imageGroup = this.imageGroupId;
          await updateTermProperties(annot);
          await updateTrackProperties(annot);
          await updateAnnotationLinkProperties(annot);
          return annot;
        }
      }
    },

    async accept() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.activateEditTool(null);
      let annot = feature.properties.annot;
      if (annot.type === AnnotationType.REVIEWED) {
        return;
      }

      try {
        let reviewedAnnot = await annot.review();
        reviewedAnnot.userByTerm = annot.userByTerm; // copy terms from initial annot
        this.$eventBus.$emit('reviewAnnotation', annot);
        this.$eventBus.$emit('addAnnotation', reviewedAnnot);
        this.$eventBus.$emit('selectAnnotation', {index: this.index, annot: reviewedAnnot});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-validation')});
      }
    },
    async reject() {
      let feature = this.selectedFeature;
      if (!feature) {
        return;
      }

      this.activateEditTool(null);
      let reviewedAnnot = feature.properties.annot;
      if (reviewedAnnot.type !== AnnotationType.REVIEWED) {
        return;
      }

      try {
        let annot = await Annotation.fetch(reviewedAnnot.parentIdent);
        annot.userByTerm = reviewedAnnot.userByTerm; // copy terms from reviewed annot
        await annot.cancelReview();
        this.$eventBus.$emit('deleteAnnotation', reviewedAnnot);
        this.$eventBus.$emit('addAnnotation', annot, false);
        this.$eventBus.$emit('selectAnnotation', {index: this.index, annot});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-rejection')});
      }
    },

    refreshTracks() {
      this.$store.dispatch(this.viewerModule + 'refreshTracks', {idImage: this.image.id});
    },

    takeScreenshot() {
      this.$emit('screenshot');
    },

    shortkeyHandler(key) {
      if (key !== 'toggle-all-current' && !this.isActiveImage) { // shortkey should only be applied to active map
        return;
      }

      switch (key) {
        case 'tool-select':
          if (this.isToolDisplayed('select')) {
            this.activateTool('select');
          }
          return;
        case 'tool-point':
          if (this.isToolDisplayed('point') && !this.disabledDraw) {
            this.activateTool('point');
          }
          return;
        case 'tool-line':
          if (this.isToolDisplayed('line') && !this.disabledDraw) {
            this.activateTool('line');
          }
          return;
        case 'tool-freehand-line':
          if (this.isToolDisplayed('freehand-line') && !this.disabledDraw) {
            this.activateTool('freehand-line');
          }
          return;
        case 'tool-rectangle':
          if (this.isToolDisplayed('rectangle') && !this.disabledDraw) {
            this.activateTool('rectangle');
          }
          return;
        case 'tool-circle':
          if (this.isToolDisplayed('circle') && !this.disabledDraw) {
            this.activateTool('circle');
          }
          return;
        case 'tool-polygon':
          if (this.isToolDisplayed('polygon') && !this.disabledDraw) {
            this.activateTool('polygon');
          }
          return;
        case 'tool-freehand-polygon':
          if (this.isToolDisplayed('polygon') && !this.disabledDraw) {
            this.activateTool('freehand-polygon');
          }
          return;
        case 'tool-screenshot':
          if (this.isToolDisplayed('screenshot') && !this.disabledDraw) {
            this.takeScreenshot();
          }
          return;
        case 'tool-delete':
          if (this.isToolDisplayed('delete') && !this.disabledDraw) {
            this.confirmDeletion();
          }
          return;
        case 'tool-undo':
          if (this.isToolDisplayed('undo-redo')) {
            this.undo();
          }
          return;
        case 'tool-redo':
          if (this.isToolDisplayed('undo-redo')) {
            this.redo();
          }
          return;
        case 'tool-review-accept':
          if (this.reviewMode && this.isToolDisplayed('accept')) {
            this.accept();
          }
          return;
        case 'tool-review-reject':
          if (this.reviewMode && this.isToolDisplayed('reject')) {
            this.reject();
          }
          return;
        case 'tool-fill':
          if (this.isToolDisplayed('fill') && !this.isToolDisabled('fill')) {
            this.fill();
          }
          return;
        case 'tool-correct-add':
          if (this.isToolDisplayed('union') && !this.isToolDisabled('correct-add')) {
            this.activateEditTool('correct-add');
          }
          return;
        case 'tool-correct-remove':
          if (this.isToolDisplayed('diff') && !this.isToolDisabled('correct-remove')) {
            this.activateEditTool('correct-remove');
          }
          return;
        case 'tool-modify':
          if (this.isToolDisplayed('edit') && !this.isToolDisabled('edit')) {
            this.activateEditTool('modify');
          }
          return;
        case 'tool-move':
          if (this.isToolDisplayed('move') && !this.isToolDisabled('move')) {
            this.activateEditTool('translate');
          }
          return;
        case 'tool-rescale':
          if (this.isToolDisplayed('resize') && !this.isToolDisabled('resize')) {
            this.activateEditTool('rescale');
          }
          return;
        case 'tool-rotate':
          if (this.isToolDisplayed('rotate') && !this.isToolDisabled('rotate')) {
            this.activateEditTool('rotate');
          }
          return;
        case 'toggle-current':
        case 'toggle-all-current':
          if (this.configUI['project-explore-annotation-main'] && this.selectedFeature) {
            this.displayAnnotDetails = !this.displayAnnotDetails;
          }
          return;
        case 'tool-copy':
          if (this.isToolDisplayed('copy-paste') && !this.isToolDisabled('copy')) {
            this.copy();
          }
          return;
        case 'tool-paste':
          if (this.isToolDisplayed('copy-paste') && !this.disabledPaste) {
            this.paste();
          }
          return;
      }
    }
  },
  mounted() {
    this.$eventBus.$on('shortkeyEvent', this.shortkeyHandler);
  },
  beforeDestroy() {
    this.$eventBus.$off('shortkeyEvent', this.shortkeyHandler);
  }
};
</script>

<style scoped>
.buttons {
  float:left;
  margin-left: 1px;
  margin-right: 1px;
  margin-bottom: 0 !important;
}

.button {
  margin-bottom:0px !important;
}

.superscript {
  position: relative;
  bottom: 0.65em;
  left: 1px;
  font-size: 0.7em;
}

:focus {outline:none;}
::-moz-focus-inner {border:0;}

.term-selection, .track-selection, .special-paste-selection {
  position: relative;
}

.term-selection.has-preview i.fas, .track-selection.has-preview i.fas {
  position: relative;
  right: 0.25em;
  top: 0.2em;
  font-size: 0.9em;
}

.term-selection .ontology-tree-container, .track-selection .tracks-tree-container, .special-paste-container {
  position: absolute;
  top: 100%;
  left: -1.5em;
  margin-top: 5px;
  min-width: 18em;
  max-width: 20vw;
  max-height: 40vh;
  overflow: auto;
}

.term-selection .ontology-tree-container, .track-selection .tracks-tree-container {
  box-shadow: 0 2px 3px rgba(10, 10, 10, 0.1), 0 0 0 1px rgba(10, 10, 10, 0.1);
  border-radius: 4px;
  background: white;
}

.term-selection:not(.has-preview) .color-preview, .track-selection:not(.has-preview) .color-preview {
  display:none;
}

.color-preview {
  width: 1em;
  height: 1em;
  display: inline-block;
  border-radius: 0.45em;
  position: absolute;
  top: 0.25em;
  right: 0.25em;
  z-index: 1000;
  pointer-events: none;
  font-size: 0.7em;
  font-weight: 600;
  text-align:center;
  line-height: 0.9em;
  font-family: Arial;
}

.track-selection .color-preview {
  border: 1.5px solid;
}

.repeat-input {
  width: 50px;
}

.repeat-button-container {
  text-align: center;
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}
</style>

<style lang="scss">
$colorActiveIcon: #fff;

.draw-tools-wrapper {
  .special-paste-selection .special-paste-container {
    p {
      margin: 0.75em;
      font-size: 0.9em;
    }

    .panel-block:last-of-type {
      border-bottom-left-radius: 4px;
      border-bottom-right-radius: 4px;
    }

    .panel-block:first-of-type {
      border-top-left-radius: 4px;
      border-top-right-radius: 4px;
    }

    .panel {
      font-size: 0.9rem;
    }

    .panel-block {
      padding: 0.2em 0.5em;
    }
  }

  .term-selection .ontology-tree-container, .track-selection .tracks-tree-container {

    .control {
      margin: 0.75em;
      margin-bottom: 0;
    }

    .ontology-tree, .track-tree {
      padding: 0.75em 0;
    }

    .sl-vue-tree-sidebar {
      margin-right: 1.5em;
    }
  }

  .button.is-selected {
    background-color: #6899d0;
    color: $colorActiveIcon;

    path {
      stroke: $colorActiveIcon;
    }
  }

  .icon svg {
    height: 1.15em !important;
  }

  .icon .special-paste-icon {
    font-size: 0.5em;
    position: absolute;
    right: 0.2rem;
    top: 0.2rem;
  }
}
</style>

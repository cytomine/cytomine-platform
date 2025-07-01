<template>
  <cytomine-modal-card :title="$t('paste-with-link')">
    <b-loading :is-full-page="false" :active="loading" class="small" />
    <template v-if="!loading">
      <b-message v-if="error" type="is-danger" has-icon icon-size="is-small">
        <h2> {{ $t('error') }} </h2>
        <p> {{ $t('unexpected-error-info-message') }} </p>
      </b-message>
      <template v-else>
        <div class="info" v-if="copiedAnnot.group"
             v-html="$tc('count-copied-annot-links', nbAlreadyLinkedAnnotations, {count: nbAlreadyLinkedAnnotations})"></div>
        <annotation-links-preview
            :size="64"
            :main-color="mainColor"
            :link-color="linkColor"
            :show-main-annotation="true"
            :annotation="copiedAnnot"
            :images="imagesInGroup"
        />
        <div class="info" v-html="$t('paste-with-link-info')"></div>

        <div v-if="imagesInGroupInViewer.length > 0">
          <div class="field header">
            <b-checkbox :value="allCheckedInViewer" @change.native="checkAllInViewer()">
              {{$t('eligible-images-in-this-viewer', {imageGroup: imageGroup.name})}}
            </b-checkbox>
          </div>
          <div class="field" v-for="image in imagesInGroupInViewer" :key="`in-viewer-${index}-${image.id}`">
            <b-checkbox v-model="checkedInViewer" :native-value="image.id">
              <image-name :image="image"></image-name> {{$t('at')}}
              <select v-model="image.inViewerPosition">
                <option
                    v-for="position in inViewerPositions"
                    :key="`${position.value}-${index}-${image.id}`"
                    :value="position.value"
                >
                  {{position.label}}
                </option>
              </select>
            </b-checkbox>
          </div>
        </div>
        <div v-if="imagesInGroupNotInViewer.length > 0">
          <div class="field header">
            <b-checkbox :value="allCheckedNotInViewer" @change.native="checkAllNotInViewer()">
              {{$t('eligible-images-not-in-this-viewer', {imageGroup: imageGroup.name})}}
            </b-checkbox>
          </div>
          <div class="field" v-for="image in imagesInGroupNotInViewer" :key="`not-in-viewer-${index}-${image.id}`">
            <b-checkbox v-model="checkedNotInViewer" :native-value="image.id">
              <image-name :image="image"></image-name> {{$t('at')}}
              <select v-model="image.notInViewerPosition">
                <option
                    v-for="position in notInViewerPositions"
                    :key="`${position.value}-${index}-${image.id}`"
                    :value="position.value"
                >
                  {{position.label}}
                </option>
              </select>
            </b-checkbox>
          </div>
        </div>
        <div v-if="imagesInGroupInViewer.length === 0 && imagesInGroupNotInViewer.length === 0"
             class="has-text-grey has-text-centered">
          <em>{{$t('all-images-have-linked-annot')}}</em>
        </div>
      </template>
    </template>

    <template #footer>
      <button class="button" @click="$parent.close()">
        {{$t('button-cancel')}}
      </button>
      <button class="button is-link" :disabled="selectedImagesAndOptions.length === 0" @click="paste()">
        {{$t('button-save')}}
      </button>
    </template>
  </cytomine-modal-card>
</template>


<script>
import WKT from 'ol/format/WKT';
import {get} from '@/utils/store-helpers';
import ImageName from '@/components/image/ImageName';
import CytomineModalCard from '@/components/utils/CytomineModalCard';
import AnnotationLinksPreview from '@/components/annotations/AnnotationLinksPreview';

import {ImageGroup, ImageInstanceCollection, AnnotationGroup, AnnotationCollection, Annotation, AnnotationLink} from 'cytomine-client';
import {getCenter, containsExtent, getIntersection} from 'ol/extent';
import {listAnnotationsInGroup} from '@/utils/annotation-utils';

export default {
  name: 'paste-annotation-with-link-modal',
  components: {CytomineModalCard, ImageName, AnnotationLinksPreview},
  props: {
    index: String
  },
  data() {
    return {
      format: new WKT(),
      imageGroup: null,
      loading: true,
      error: false,

      mainColor: '0099ff',
      linkColor: '696969',

      viewerCenterPosition: {label: this.$t('viewer-center-position'), value: 'viewer'},
      imageCenterPosition: {label: this.$t('image-center-position'), value: 'image'},
      annotationPosition: {label: this.$t('annotation-position'), value: 'annotation'},

      checkedInViewer: [],
      checkedNotInViewer: [],
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    imageGroupId() {
      return this.$store.getters[this.imageModule + 'imageGroupId'];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    imagesInGroup() {
      return (!this.imageGroup) ? [] : this.imageGroup.imageInstances;
    },
    copiedAnnot: {
      get() {
        return this.viewerWrapper.copiedAnnot;
      },
      set(annot) {
        this.$store.commit(this.viewerModule + 'setCopiedAnnot', annot);
      }
    },
    copiedAnnotImageInstance() {
      return this.viewerWrapper.copiedAnnotImageInstance;
    },
    alreadyLinkedAnnotations() {
      return this.copiedAnnot.annotationLink;
    },
    nbAlreadyLinkedAnnotations() {
      return Math.max(0, this.alreadyLinkedAnnotations.length - 1);
    },
    ineligibleImageIds() {
      return [this.copiedAnnot.image, ...this.alreadyLinkedAnnotations.map(link => link.image)];
    },

    eligibleImages() {
      return this.imagesInGroup.filter(image => !this.ineligibleImageIds.includes(image.id));
    },

    imageWrappers() {
      return Object.keys(this.viewerWrapper.images).reduce((obj, index) => {
        obj[index] = {
          index,
          imageInstance: this.viewerWrapper.images[index].imageInstance,
          imageGroupLink: this.viewerWrapper.images[index].imageGroupLink,
          view: this.viewerWrapper.images[index].view,
        };
        return obj;
      }, []);
    },
    imageWrappersInGroupInViewer() {
      return this.imageWrappers.filter(wrapper =>
        wrapper.imageGroupLink &&
        wrapper.imageGroupLink.group === this.imageGroupId);
    },
    imagesIdsInGroupInViewer() {
      return this.imageWrappersInGroupInViewer.map(wrapper => wrapper.imageInstance.id);
    },

    imagesInGroupInViewer() {
      return this.eligibleImages.filter(image => this.imagesIdsInGroupInViewer.includes(image.id));
    },
    imagesInGroupNotInViewer() {
      return this.eligibleImages.filter(image => !this.imagesIdsInGroupInViewer.includes(image.id) &&
        image.id !== this.image.id);
    },

    selectedImagesAndOptions() {
      let selected1 = this.imagesInGroupInViewer.filter(image => this.checkedInViewer.includes(image.id))
        .map(image => {
          return {'image': image.id, 'position': image.inViewerPosition};
        });
      let selected2 = this.imagesInGroupNotInViewer.filter(image => this.checkedNotInViewer.includes(image.id))
        .map(image => {
          return {'image': image.id, 'position': image.notInViewerPosition};
        });
      return selected1.concat(selected2);
    },

    inViewerPositions() {
      return [this.viewerCenterPosition, this.imageCenterPosition, this.annotationPosition];
    },
    notInViewerPositions() {
      return [this.imageCenterPosition, this.annotationPosition];
    },

    allCheckedInViewer() {
      return this.checkedInViewer.length === this.imagesInGroupInViewer.length;
    },
    allCheckedNotInViewer() {
      return this.checkedNotInViewer.length === this.imagesInGroupNotInViewer.length;
    }
  },
  methods: {
    checkAllInViewer() {
      this.checkedInViewer = (this.allCheckedInViewer) ? [] : this.imagesInGroupInViewer.map(image => image.id);
    },
    checkAllNotInViewer() {
      this.checkedNotInViewer = (this.allCheckedNotInViewer) ? [] : this.imagesInGroupNotInViewer.map(image => image.id);
    },

    async paste() {
      if (!this.copiedAnnot || this.selectedImagesAndOptions.length === 0) {
        return;
      }

      try {
        let annotGroup = null;
        let existingAnnotGroup = this.copiedAnnot.group;
        let existingAnnots = [this.copiedAnnot.id, ...this.alreadyLinkedAnnotations.map(al => al.annotation)];
        if (existingAnnotGroup === null) {
          annotGroup = await new AnnotationGroup({
            imageGroup: this.imageGroupId,
            project: this.image.project,
            type: 'SAME_OBJECT'
          }).save();
        } else {
          annotGroup = await AnnotationGroup.fetch(existingAnnotGroup);
        }

        let imageInstanceCollection = await ImageInstanceCollection.fetchAll({
          filterKey: 'imagegroup',
          filterValue: this.imageGroupId
        });

        let collection = new AnnotationCollection();
        this.selectedImagesAndOptions.forEach(selected => {
          let imageInstance = imageInstanceCollection.array.find(
            imageInstance => imageInstance.id === selected.image
          );
          if (!imageInstance) {
            throw Error(`ImageInstance not found for ${selected}`);
          }

          let location = this.convertLocation(this.copiedAnnot.location, imageInstance, selected.position);
          if (!location) {
            throw Error(`Invalid location for ${selected}`);
          }

          collection.push(new Annotation({
            image: selected.image,
            user: this.currentUser.id,
            term: this.copiedAnnot.term,
            group: annotGroup.id,
            location: location
          }));
        });
        await collection.save();

        if (existingAnnotGroup === null) {
          await new AnnotationLink({
            annotationIdent: this.copiedAnnot.id,
            group: annotGroup.id,
            image: this.copiedAnnot.image
          }).save();
        }

        (await listAnnotationsInGroup(this.image.project, annotGroup.id)).forEach(a => {
          if (existingAnnots.includes(a.id)) {
            this.$eventBus.$emit('editAnnotation', a);

            if (a.id === this.copiedAnnot.id) {
              let copiedAnnot = this.copiedAnnot.clone();
              copiedAnnot.annotationLink = a.annotationLink;
              copiedAnnot.group = a.group;
              this.copiedAnnot = copiedAnnot;
            }
          } else {
            this.$eventBus.$emit('addAnnotation', a);
          }
          if (this.imagesIdsInGroupInViewer.includes(a.image)) {
            let index = this.findWrapper(a.image).index;
            this.$eventBus.$emit('selectAnnotation', {index, annot: a, center: false});
          }
        });

        this.$notify({type: 'success', text: this.$t('notif-success-annotation-link-paste')});
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-annotation-link-paste')});
      }
      this.$parent.close();
    },
    convertLocation(originalLocation, destImage, position) {
      let geometry = new WKT().readGeometry(originalLocation);
      let centerExtent = getCenter(geometry.getExtent());

      if (position === this.imageCenterPosition.value) {
        geometry.translate((destImage.width / 2) - centerExtent[0], (destImage.height / 2) - centerExtent[1]);
      } else if (position === this.viewerCenterPosition.value) {
        let wrapper = this.findWrapper(destImage.id);
        if (!wrapper) {
          return;
        }
        geometry.translate(wrapper.view.center[0] - centerExtent[0], wrapper.view.center[1] - centerExtent[1]);
      }

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
    findWrapper(id) {
      return this.imageWrappers.find(wrapper => wrapper.imageInstance.id === id);
    }
  },
  async created() {
    try {
      this.imageGroup = await ImageGroup.fetch(this.imageGroupId);
      for (let imageInstance of this.imageGroup.imageInstances) {
        this.$set(imageInstance, 'inViewerPosition', this.viewerCenterPosition.value);
        this.$set(imageInstance, 'notInViewerPosition', this.imageCenterPosition.value);
      }
      this.loading = false;
    } catch (error) {
      console.log(error);
      this.error = true;
    }
  }
};
</script>

<style scoped>
.paste-with-link-selector {
  margin: 0.75em;
  font-size: 0.9em;
}

.header {
  font-weight: bold;
  margin-top: 1rem;
}

.field:not(.header) {
  margin-left: 0.9em;
}
</style>

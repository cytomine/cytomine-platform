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
<div>
  <h1>{{$t('review')}}</h1>
  <template v-if="image.reviewed">
    <b-message type="is-success" size="is-small" has-icon>
      <i18n :path="isReviewer ? 'you-have-validated-image-on' : 'image-validated-by-on'">
        <username v-if="!isReviewer" place="user" :user="reviewer" />
        <span place="date">{{ Number(image.reviewStop) | moment('ll LT') }}</span>
      </i18n>
    </b-message>

    <button v-if="isReviewer" class="button is-small is-fullwidth" @click="unvalidate()">
      <span class="icon"><i class="fas fa-thumbs-down"></i></span>
      <span>{{$t('button-unvalidate-and-continue-review')}}</span>
    </button>
  </template>

  <template v-else-if="image.inReview">
    <template v-if="isReviewer">
      <div v-if="!reviewMode">
        <b-message type="is-info" size="is-small" has-icon>
          <i18n path="you-are-reviewing-image-since">
            <span place="date">{{ Number(image.reviewStart) | moment('ll LT') }}</span>
          </i18n>
        </b-message>
        <button class="button is-small is-fullwidth" @click="reviewMode = true">
          <span class="icon"><i class="fas fa-play-circle"></i></span>
          <span>{{$t('button-continue-review')}}</span>
        </button>
      </div>

      <div v-else>
          <div class="small">
            <i18n path="review-list-visible-layers">
              <list-usernames place="visibleLayers" :users="visibleUserLayers" />
            </i18n>
            <div v-if="!taskReviewAll || taskReviewAll.progress === 100">
              {{this.$t('review-explanation')}}
              <div class="buttons is-centered are-small has-addons is-fullwidth">
                <button class="button" @click="reviewAll(true)">
                  <span class="icon"><i class="fas fa-check"></i></span>
                  <span>{{$t('button-accept-all')}}</span>
                </button>
                <button class="button" @click="reviewAll(false)">
                  <span class="icon"><i class="fas fa-minus"></i></span>
                  <span>{{$t('button-reject-all')}}</span>
                </button>
              </div>
            </div>

            <cytomine-task v-else :task.sync="taskReviewAll" />
          </div>

          <div class="buttons are-small">
            <button class="button is-fullwidth" @click="reviewMode = false">
              <span class="icon"><i class="fas fa-pause-circle"></i></span>
              <span>{{$t('button-continue-review-later')}}</span>
            </button>
            <button class="button is-danger is-fullwidth" @click="cancelReview()">
                <span class="icon"><i class="fas fa-times"></i></span>
                <span>{{$t('button-cancel-review')}}</span>
            </button>
            <button class="button is-success is-fullwidth" @click="validate()">
                <span class="icon"><i class="fas fa-thumbs-up"></i></span>
                <span>{{$t('button-validate-review')}}</span>
            </button>
          </div>
      </div>
    </template>

    <b-message v-else type="is-info" size="is-small" has-icon>
      <i18n path="image-in-review-by-since">
        <username place="user" :user="reviewer" />
        <span place="date">{{ Number(image.reviewStart) | moment('ll LT') }}</span>
      </i18n>
    </b-message>
  </template>

  <template v-else>
    {{$t('image-not-reviewed')}}
    <button class="button is-small is-fullwidth has-margin-top" @click="startReview()">
      <span class="icon"><i class="fas fa-play-circle"></i></span>
      <span>{{$t('button-start-review')}}</span>
    </button>
  </template>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';
import Username from '@/components/user/Username';
import ListUsernames from '@/components/user/ListUsernames';
import CytomineTask from '@/components/utils/CytomineTask';
import {User, Task, AnnotationCollection} from 'cytomine-client';

export default {
  name: 'review-panel',
  props: {
    index: String
  },
  components: {
    Username,
    ListUsernames,
    CytomineTask
  },
  data() {
    return {
      taskReviewAll: null,
      reviewer: null
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    project: get('currentProject/project'),
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    imageWrapper() {
      return this.$store.getters['currentProject/currentViewer'].images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance.clone(); // returns a clone of the image so that it can be modified in this component without mutating state outside vuex
    },
    reviewMode: {
      get() {
        return this.imageWrapper.review.reviewMode;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setReviewMode', value);
      }
    },
    isReviewer() { // true iff the current user is the reviewer
      return this.image.reviewUser === this.currentUser.id;
    },
    selectedLayers() {
      return this.imageWrapper.layers.selectedLayers || [];
    },
    visibleUserLayers() {
      return this.selectedLayers.filter(layer => layer.visible && !layer.isReview);
    },
    visibleUserLayerIds() {
      return this.visibleUserLayers.map(layer => layer.id);
    },
    reviewLayer() {
      return this.selectedLayers.find(layer => layer.isReview) || {};
    }
  },
  methods: {
    commitImage() {
      this.$store.commit(this.imageModule + 'setImageInstance', this.image);
    },
    async startReview() {
      try {
        await this.image.review();
        this.commitImage();
        this.reviewMode = true;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-start-review')});
      }
    },
    async cancelReview() {
      try {
        await this.image.stopReview(true);
        this.commitImage();
        this.reviewMode = false;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-cancel-review')});
      }
    },
    async validate() {
      try {
        await this.image.stopReview();
        this.commitImage();
        this.reviewMode = false;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-validate-review')});
      }
    },
    async unvalidate() {
      try {
        await this.image.stopReview(true);
        this.commitImage();
        this.reviewMode = false;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-unvalidate-review')});
      }
    },
    async reviewAll(accept) {
      try {
        this.taskReviewAll = await new Task({project: this.project.id}).save();
        await AnnotationCollection.reviewAll({
          accept,
          image: this.image.id,
          users: this.visibleUserLayerIds,
          task: this.taskReviewAll.id
        });
        this.$eventBus.$emit('reloadAnnotations', {idImage: this.image.id, clear: true});
      } catch (error) {
        console.log(error);
        this.taskReviewAll = null;
        this.$notify({type: 'error', text: this.$t('notif-error-review-' + (accept ? 'accept' : 'reject') + '-all')});
      }
    },
  },
  async created() {
    if (this.image.reviewUser && !this.isReviewer) {
      this.reviewer = await User.fetch(this.image.reviewUser);
    }
  }
};
</script>

<style scoped>
.buttons, .has-margin-top {
  margin-top: 0.5em;
}

.buttons.is-fullwidth .button {
  flex: 1;
}

.small {
  font-size: 0.9em;
}
</style>

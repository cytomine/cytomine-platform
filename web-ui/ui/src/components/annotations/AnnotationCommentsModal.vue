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
<cytomine-modal-card :title="$t('annotation-comments')" @close="$parent.close()">
  <div v-if="!comments || !comments.length">
    <em class="has-text-grey">
      {{$t('no-annotation-comments')}}
    </em>
    <hr>
  </div>
  <template v-else>
    <div class="comments-wrapper">
      <div v-for="comment in comments" :key="comment.id">
        <p class="comment-sender is-size-7">
          <strong>{{ comment.senderName }}</strong>
          <span class="has-text-grey">{{Number(comment.created) | moment('ll LT')}}</span>
        </p>
        <p class="comment-content">{{comment.comment}}</p>
        <hr>
      </div>
    </div>
  </template>

  <div v-show="!addingComment" class="has-text-centered">
    <button class="button is-link" v-if="!currentUser.guestByNow" @click="addingComment = true">{{$t('button-add-comment')}}</button>
  </div>

  <div v-show="addingComment">
    <h2>{{$t('add-new-comment')}}</h2>
    <b-field>
      <b-radio v-model="sendToAllMembers" :native-value="true" :disabled="members.length == 0">
        {{$t('send-to-all-project-members')}}
      </b-radio>
    </b-field>
    <b-field>
      <b-radio v-model="sendToAllMembers" :native-value="false">
        {{$t('send-to-some-members')}}
      </b-radio>
    </b-field>
    <b-field v-if="!sendToAllMembers" :type="{'is-danger': errors.has('members')}" :message="errors.first('members')">
      <domain-tag-input v-model="selectedMembers" :domains="members" placeholder="search-user" name="members" v-validate="'required'" searchedProperty="fullName" displayedProperty="fullName" />
    </b-field>
    <b-field :type="{'is-danger': errors.has('comment')}" :message="errors.first('comment')">
      <b-input v-model="text" type="textarea" :placeholder="$t('enter-comment')" rows="2" name="comment" v-validate="'required'" />
    </b-field>
    <p class="buttons is-right are-small">
      <button class="button" @click="addingComment = false" :disabled="loading">
        {{$t('button-cancel')}}
      </button>
      <button class="button is-link" :class="{'is-loading': loading}"
        :disabled="loading || members.length == 0 || errors.any()" @click="share()">
        {{$t('button-share')}}
      </button>
    </p>
  </div>
</cytomine-modal-card>
</template>

<script>
import {get} from '@/utils/store-helpers';

import {AnnotationComment} from 'cytomine-client';
import DomainTagInput from '@/components/utils/DomainTagInput';

import CytomineModalCard from '@/components/utils/CytomineModalCard';

export default {
  name: 'annotation-comments-modal',
  components: {
    DomainTagInput,
    CytomineModalCard
  },
  $_veeValidate: {validator: 'new'},
  props: {
    annotation: Object,
    comments: Array
  },
  data() {
    return {
      addingComment: false,
      sendToAllMembers: true,
      selectedMembers: [],
      text: '',
      loading: false
    };
  },
  computed: {
    currentUser: get('currentUser/user'),
    allMembers: get('currentProject/members'),
    members() { // all project members except current user
      return this.allMembers.filter(member => member.id !== this.currentUser.id);
    },
    annotationURL() {
      let uri = `project/${this.annotation.project}/image/${this.annotation.image}/annotation/${this.annotation.id}`;
      return `${window.location.origin}/#/${uri}`;
    }
  },
  watch: {
    addingComment() {
      this.text = '';
      this.selectedMembers = [];
      this.$nextTick(() => {
        this.sendToAllMembers = true;
        this.$validator.reset();
      });
    }
  },
  methods: {
    async share() {
      let result = await this.$validator.validateAll();
      if (!result) {
        return;
      }

      this.loading = true;
      try {
        let sender = this.currentUser.fullName;
        let newComment = await new AnnotationComment({
          annotation: this.annotation,
          subject: `Cytomine: ${sender} commented an annotation`, // not translated because the content of the mail will be in english
          from: sender,
          receivers: (this.sendToAllMembers ? this.members : this.selectedMembers).map(m => m.id),
          comment: this.text,
          annotationURL: this.annotationURL,
          shareAnnotationURL: this.annotationURL + '?action=comments'
        }).save();
        this.$emit('addComment', newComment);
        this.$notify({type: 'success', text: this.$t('notif-success-new-comment')});
        this.addingComment = false;
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-new-comment')});
      }
      this.loading = false;
    },
  }
};
</script>

<style scoped>
>>> .modal-card {
  max-height: 80vh;
}

>>> .modal-card-body {
  display: flex;
  flex-direction: column;
}

.comments-wrapper {
  overflow: auto;
  margin-bottom: 1em;
  min-height: 5em;
}

.comment-sender strong {
  margin-right: 0.5em;
}

.comment-content {
  margin-top: 0.25em;
  margin-bottom: 0.5em;
}

>>> textarea {
  margin: 0.75em 0 0;
}

hr {
  margin: 0.75em 0 !important;
}

>>> .dropdown-content {
  max-height: 7em !important;
}
</style>

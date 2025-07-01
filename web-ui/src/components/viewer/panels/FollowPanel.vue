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
<div class="follow-panel">
  <h1>{{$t('broadcast')}}</h1>

  <div>
    <b-checkbox v-model="broadcastModel" type="is-info" :disabled="disabledBroadcast">
      {{$t('broadcast-my-position')}}
    </b-checkbox>
  </div>

    <template v-if="broadcast">
      <h2>{{$t('followers')}} ({{this.followers.length}})</h2>
      <div class="followers">
        <div class="field" v-for="user in followers" :key="user.id">
          <li><username :user="user" /></li>
        </div>
      </div>
    </template>

  <h2 :class="{disabled: broadcast}">{{$t('follow-user')}}</h2>

  <div class="follow-panel-content" :class="{disabled: broadcast}">
    <div class="field">
      <b-radio v-model="trackedUserModel" :native-value="null" type="is-info" :disabled="broadcast">
        {{$t('no-tracking')}}
      </b-radio>
    </div>

    <template v-if="onlineManagers.length > 0">
      <h3>{{$t('online-managers')}}</h3>
      <div class="field" v-for="user in onlineManagers" :key="user.id">
        <b-radio v-model="trackedUserModel" :native-value="user.id" type="is-info" :disabled="broadcast">
          <username :user="user" />
        </b-radio>
      </div>
    </template>

    <template v-if="onlineContributors.length > 0">
      <h3>{{$t('online-contributors')}}</h3>
      <div class="field" v-for="user in onlineContributors" :key="user.id">
        <b-radio v-model="trackedUserModel" :native-value="user.id" type="is-info" :disabled="broadcast">
          <username :user="user" />
        </b-radio>
      </div>
    </template>
  </div>
</div>
</template>

<script>
import {get} from '@/utils/store-helpers';

import Username from '@/components/user/Username';
import {appendShortTermToken} from '@/utils/token-utils.js';
import {UserPosition} from 'cytomine-client';

import constants from '@/utils/constants.js';

export default {
  name: 'follow-panel',
  components: {Username},
  props: {
    index: String,
    view: Object
  },
  data() {
    return {
      onlineUsers: [],
      followers: [],

      broadcastModel: false,
      trackedUserModel: null,

      timeoutOnlineUsers: null,
      timeoutTracking: null,

      disabledBroadcast: false,

      wsUserPositionPath: `ws://${window.location.host}/ws/user-position/`,

      userPostitionWebsock: null,
      wsConnected: false,
      wsInterval: undefined,
      viewerPositionChanged: false,
      lastPositionUpdate: Date.now()
    };
  },
  computed: {
    projectMembers: get('currentProject/members'),
    projectManagers: get('currentProject/managers'),
    shortTermToken: get('currentUser/shortTermToken'),
    projectContributors() {
      return this.$store.getters['currentProject/contributors'];
    },
    currentUser: get('currentUser/user'),
    project: get('currentProject/project'),
    blindMode() {
      return this.project.blindMode;
    },
    viewerModule() {
      return this.$store.getters['currentProject/currentViewerModule'];
    },
    imageModule() {
      return this.$store.getters['currentProject/imageModule'](this.index);
    },
    viewerWrapper() {
      return this.$store.getters['currentProject/currentViewer'];
    },
    images() {
      return this.viewerWrapper.images;
    },
    linkedIndexes() {
      return this.viewerWrapper.links.find(group => group.includes(this.index)) || [];
    },
    linkedIndexTracking() { // true if current view is linked with another view with tracking enabled
      return this.linkedIndexes.some(idx => idx !== this.index && this.images[idx].tracking.trackedUser);
    },
    imageWrapper() {
      return this.viewerWrapper.images[this.index];
    },
    image() {
      return this.imageWrapper.imageInstance;
    },
    activePanel() {
      return this.imageWrapper.activePanel;
    },
    broadcast: {
      get() {
        return this.imageWrapper.tracking.broadcast;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setBroadcast', value);
      }
    },
    alreadyBroadcastingImage() {
      return Object.keys(this.images).some(index => {
        let image = this.images[index];
        return index !== this.index && image.imageInstance
          && image.imageInstance.id === this.image.id && image.tracking.broadcast;
      });
    },
    trackedUser: {
      get() {
        return this.imageWrapper.tracking.trackedUser;
      },
      set(value) {
        this.$store.commit(this.imageModule + 'setTrackedUser', value);
      }
    },
    onlineManagers() {
      return this.projectManagers.filter(({id}) => this.onlineUsers.includes(id));
    },
    onlineContributors() {
      return this.projectContributors.filter(({id}) => this.onlineUsers.includes(id));
    },
    trackedUserFullName() {
      let trackedUser = this.projectMembers.find(user => user.id === this.trackedUser);
      if (trackedUser) {
        return trackedUser.fullName;
      }
      return null;
    }
  },
  watch: {
    activePanel(panel) {
      if (panel === 'follow') {
        this.fetchOnline();
      }
    },

    broadcast() {
      if (this.broadcast) {
        this.stopTrack();
        if (!this.wsConnected) {
          this.initWebSocket();
          this.wsInterval = setInterval(() => {
            this.sendPosition();
          }, constants.TRACKING_REFRESH_INTERVAL);
        }
        this.trackedUser = null;
      } else {
        clearInterval(this.wsInterval);
        this.userPostitionWebsock.close();
      }
    },

    alreadyBroadcastingImage(value) {
      if (!value) {
        this.disabledBroadcast = false;
      }
    },

    broadcastModel(value) {
      if (value && this.alreadyBroadcastingImage) {
        this.$notify({type: 'error', text: this.$t('notif-error-already-broadcasting-this-image')});
        this.disabledBroadcast = true;
        this.$nextTick(() => this.broadcastModel = false);
        return;
      }
      this.broadcast = value;
    },

    trackedUserModel(value) {
      // if map is linked to another map on which tracking is already enabled, possible conflict
      if (value && this.linkedIndexTracking) {
        this.$buefy.dialog.confirm({
          title: this.$t('possible-conflict'),
          message: this.$t('confirm-unlink-view-to-track'),
          confirmText: this.$t('button-confirm'),
          cancelText: this.$t('button-cancel'),
          onConfirm: () => {
            this.$store.commit(this.viewerModule + 'unlinkImage', {indexImage: this.index});
            this.trackedUser = value;
          },
          onCancel: () => {
            this.stopTrack();
            this.trackedUserModel = null;
          }
        });
      } else {
        this.stopTrack();
        this.trackedUser = value;
      }
    },

    trackedUser(id) {
      this.trackedUserModel = id;

      if (id) {
        if (!this.wsConnected) {
          this.initTracking();
        }
        this.track();
        this.fetchOnline();
      }
    },

    onlineUsers(onlines) {
      if (this.trackedUser && !onlines.includes(this.trackedUser)) {
        this.$notify({
          type: 'info',
          text: this.$t('end-tracking-user-no-longer-broadcasting', {
            username: this.trackedUserFullName,
            imageName: this.blindMode ? this.image.blindedName : this.image.instanceFilename
          })
        });
        this.trackedUser = null;
      }
    },
    'view.zoom'() {
      this.viewerPositionChanged = true;
    },
    'view.rotation'() {
      this.viewerPositionChanged = true;
    },
    'view.viewCenter.0'() {
      this.viewerPositionChanged = true;
    },
    'view.viewCenter.1'() {
      this.viewerPositionChanged = true;
    }
  },
  methods: {
    initTracking() {
      this.initWebSocket();
      this.userPostitionWebsock.onopen = this.onOpentracking;
    },
    initWebSocket() {
      try {
        this.userPostitionWebsock = new WebSocket(appendShortTermToken(this.wsUserPositionPath + this.currentUser.id + '/' + this.image.id + '/' + this.broadcast, this.shortTermToken));
        this.userPostitionWebsock.onopen = this.onOpen;
        this.userPostitionWebsock.onclose = this.onClose;
        this.userPostitionWebsock.onmessage = this.onMessage;
      } catch (error) {
        console.log('error', error);
      }
    },
    onOpen() {
      this.wsConnected = true;
    },
    onOpentracking() {
      this.onOpen();
      this.userPostitionWebsock.send(this.trackedUserModel);
    },
    onClose() {
      this.wsConnected = false;
    },
    onMessage(message) {
      if (message.data === 'stop-track') {
        this.stopTrack();
      } else {
        let pos = JSON.parse(message.data);
        this.moveView(pos);
      }
    },
    moveView(pos) {
      this.view.animate({
        center: [pos.x, pos.y],
        zoom: pos.zoom,
        rotation: pos.rotation,
        duration: 500
      });
    },
    stopTrack() {
      if (this.wsConnected) {
        this.userPostitionWebsock.close();
        this.wsConnected = false;
      }
      this.trackedUser = null;
    },

    sendPosition() {
      const shouldRefreshForKeepAlive = Date.now() - this.lastPositionUpdate > constants.WS_POSITION_KEEP_ALIVE_INTERVAL;
      if (this.broadcast && this.wsConnected && (this.viewerPositionChanged || shouldRefreshForKeepAlive)) {
        const position = JSON.stringify({
          x: this.view.viewCenter[0],
          y: this.view.viewCenter[1],
          zoom: this.view.zoom,
          rotation: this.view.rotation
        });
        this.userPostitionWebsock.send(position);
        this.viewerPositionChanged = false;
        this.lastPositionUpdate = Date.now();
      }
    },
    async track() {
      if (!this.trackedUser || this.wsConnected) {
        return;
      }

      try {
        console.log('fetchLastPosition');
        let pos = await UserPosition.fetchLastPosition(this.image.id, this.trackedUser);
        if (!pos.id || !pos.broadcast) {
          return;
        }

        this.moveView(pos);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-tracked-user-position')});
      }

      clearTimeout(this.timeoutTracking);
      if (!this.wsConnected) {
        this.timeoutTracking = setTimeout(this.track, constants.TRACKING_REFRESH_INTERVAL);
      }
    },

    async fetchOnline() {
      if (!this.trackedUser && this.activePanel !== 'follow') { // if panel not opened and no tracking ongoing, no need to refresh the data
        return;
      }

      let onlines = await this.image.fetchConnectedUsers(true); // retrieve broadcasting user
      this.onlineUsers = onlines.filter(id => id !== this.currentUser.id);

      if (this.broadcast) {
        this.followers = [];
        let followersIds = await this.$store.dispatch('currentProject/fetchFollowers', {userId: this.currentUser.id, imageId: this.image.id});

        this.projectMembers.forEach(member => {
          if (followersIds.includes('' + member.id)) {
            this.followers.push(member);
          }
        });
      }

      clearTimeout(this.timeoutOnlineUsers);
      this.timeoutOnlineUsers = setTimeout(this.fetchOnline, constants.BROADCASTING_USERS_REFRESH_INTERVAL);
    },
  },
  created() {
    this.trackedUserModel = this.trackedUser;
    this.broadcastModel = this.broadcast;
    this.fetchOnline();
    this.track();
  },
  beforeDestroy() {
    clearTimeout(this.timeoutTracking);
    clearTimeout(this.timeoutOnlineUsers);
    if (this.wsConnected) {
      this.userPostitionWebsock.close();
      this.wsConnected = false;
    }
  }
};
</script>

<style scoped>
h2 {
  margin-top: 0.8em;
  margin-bottom: 0.4em;
  font-size: 1em;
}

h3 {
  margin-top: 0.8em;
  margin-bottom: 0.4em;
  font-weight: 600;
}

.disabled {
  color: #7a7a7a;
}

.follow-panel-content {
  max-height: 14em;
  overflow: auto;
}

.followers{
  overflow: scroll;
  max-height: 4.5em;
}
</style>

<style>
.follow-panel .b-checkbox {
  align-items: flex-start !important;
}
</style>

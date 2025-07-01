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
  <div class="track-tree" :class="{selector: allowSelection, draggable: allowDrag, editable: allowEdition}">
    <sl-vue-tree v-model="treeNodes" :allowMultiselect="false" @select="select" @drop="drop" ref="tree">
      <template #toggle="{node}">
        <template v-if="!node.data.hidden && !node.isLeaf && node.children.length > 0">
          <i :class="['tree-toggle', 'fas', node.isExpanded ? 'fa-angle-down' : 'fa-angle-right']"></i>
        </template>
        <div class="sl-vue-tree-gap"></div>
      </template>

      <template #title="{node}">
        <div v-if="!node.data.hidden" class="tree-selector">
          <i class="tree-checkbox"
             v-if="allowSelection"
             :class="classNames(node)">
          </i>
          <cytomine-track :track="node.data" />
        </div>
        <div v-else></div>
      </template>

      <template #sidebar="{node}">
        <slot v-if="!node.data.hidden" name="custom-sidebar" :track="node.data">
          <div v-if="allowEdition" class="buttons">
            <button class="button is-small" @click="startTrackUpdate(node)">
            <span class="icon is-small">
              <i class="fas fa-edit"></i>
            </span>
            </button>
            <button class="button is-small" @click="confirmTrackDeletion(node)">
            <span class="icon is-small">
              <i class="far fa-trash-alt"></i>
            </span>
            </button>
          </div>
        </slot>
      </template>
    </sl-vue-tree>

    <slot v-if="noResult" name="no-result">
      <em class="has-text-grey no-result">{{$t('no-result')}}</em>
    </slot>

    <div v-if="allowEdition || allowNew" class="add-track-container">
      <button class="button is-small" @click="startTrackCreation()">{{$t('add-track')}}</button>
    </div>

  </div>
</template>

<script>
import SlVueTree from 'sl-vue-tree';
import CytomineTrack from './CytomineTrack';
import TrackModal from './TrackModal';
import {Track} from 'cytomine-client';

export default {
  name: 'track-tree',
  model: {
    prop: 'selectedNodes',
    event: 'setSelectedNodes'
  },
  props: {
    tracks: {type: Array},
    additionalNodes: {type: Array, default: () => []},
    startWithAdditionalNodes: {type: Boolean, default: false},
    searchString: {type: String, default: ''},
    selectedNodes: {type: Array, default: () => []},
    allowSelection: {type: Boolean, default: true},
    multipleSelection: {type: Boolean, default: true},
    allowDrag: {type: Boolean, default: false},
    allowEdition: {type: Boolean, default: false},
    allowNew: {type: Boolean, default: false},
    image: {type: Object, default: null} //Cannot be null if allowNew
  },
  components: {
    SlVueTree,
    CytomineTrack
  },
  data() {
    return {
      treeNodes: [],
      internalSelectedNodes: [],
      editedNode: null
    };
  },
  computed: {
    lowCaseSearchString() {
      return this.searchString.toLowerCase();
    },
    noResult() {
      return this.treeNodes.every(node => node.data.hidden);
    }
  },
  watch: {
    tracks() {
      this.makeTree();
    },
    additionalNodes() {
      this.makeTree();
    },
    selectedNodes() {
      this.internalSelectedNodes = this.selectedNodes.slice();
      this.refreshNodeSelection();
    },
    lowCaseSearchString() {
      this.filter();
    }
  },
  methods: {
    makeTree() {
      if (!this.tracks) {
        this.treeNodes = [];
        return;
      }

      let nodes = this.createSubTree(this.tracks.slice());
      let additionalNodes = this.createSubTree(this.additionalNodes.slice());
      this.treeNodes = this.startWithAdditionalNodes ? additionalNodes.concat(nodes) : nodes.concat(additionalNodes);

      this.filter();
    },

    createSubTree(tracks) {
      return tracks.map(track => this.createNode(track));
    },

    createNode(track) {
      return {
        title: track.name,
        isLeaf: false, // all tracks can be used as parent for drag and drop
        isDraggable: this.allowDrag,
        isExpanded: true,
        isSelected: this.internalSelectedNodes.includes(track.id),
        data: {
          id: track.id,
          name: track.name,
          color: track.color,
          parent: track.parent,
          image: track.image,
          hidden: false
        },
        children: track.children && track.children.length > 0 ? this.createSubTree(track.children) : []
      };
    },

    filter() {
      let str = this.lowCaseSearchString;
      this.applyToAllNodes(node => {
        let match = node.title.toLowerCase().indexOf(str) >= 0;
        if (node.children) {
          let matchInChildren = node.children.some(child => !child.data.hidden); // OK because applyToAllNodes performs bottom-up operations
          node.isExpanded = matchInChildren;
          match = match || matchInChildren;
        }
        node.data.hidden = !match;
      });
    },

    classNames(node) {
      if (this.multipleSelection) {
        return node.isSelected ? ['fas', 'fa-check-square'] : ['far', 'fa-square'];
      } else {
        return node.isSelected ? ['fas', 'fa-dot-circle'] : ['far', 'fa-circle'];
      }
    },

    select(nodes, event) {
      if (!this.allowSelection) {
        return;
      }

      if (this.clickOnTreeSelector(event.target)) {
        nodes.forEach(node => {
          if (this.multipleSelection) {
            let indexSelected = this.internalSelectedNodes.indexOf(node.data.id);
            if (indexSelected >= 0) {
              this.internalSelectedNodes.splice(indexSelected, 1);
              this.$emit('unselect', node.data.id);
            } else {
              this.internalSelectedNodes.push(node.data.id);
              this.$emit('select', node.data.id);
            }
          } else {
            this.internalSelectedNodes = [node.data.id];
            this.$emit('select', node.data.id);
          }
        });
        this.$emit('setSelectedNodes', this.internalSelectedNodes);
      }

      this.refreshNodeSelection();
    },
    refreshNodeSelection() {
      if (!this.allowSelection) {
        return;
      }

      this.applyToAllNodes(node => {
        node.isSelected = this.internalSelectedNodes.some(id => id === node.data.id);
      });
    },
    clickOnTreeSelector(elem) {
      if (elem.classList.contains('tree-selector')) {
        return true;
      }
      return elem.parentElement ? this.clickOnTreeSelector(elem.parentElement) : false;
    },
    applyToAllNodes(fct, nodes = this.treeNodes) {
      nodes.forEach(node => {
        if (node.children) {
          this.applyToAllNodes(fct, node.children);
        }
        fct(node);
      });
    },

    startTrackCreation() {
      this.editedNode = null;
      this.openModal();
    },
    createTrack(track) {
      this.treeNodes.push(this.createNode(track));
      this.$emit('newTrack', track);
    },

    startTrackUpdate(node) {
      this.editedNode = node;
      this.openModal();
    },
    updateTrack(track) {
      this.$refs.tree.updateNode(this.editedNode.path, {data: {...track}});
      this.$emit('updatedTrack', track);
    },

    openModal() {
      this.$buefy.modal.open({
        parent: this,
        component: TrackModal,
        props: {
          track: this.editedNode ? this.editedNode.data : null,
          image: this.image
        },
        events: {
          newTrack: this.createTrack,
          updateTrack: this.updateTrack
        },
        hasModalCard: true
      });
    },

    drop(nodes, position) {
      nodes.forEach(async node => {
        let idParent = (position.placement === 'inside') ? position.node.data.id : position.node.data.parent;
        if (node.data.parent !== idParent) {
          try {
            await new Track(node.data).changeParent(idParent);
            this.applyToAllNodes(tmp => {
              if (tmp.data.id === node.data.id) {
                tmp.data.parent = idParent;
              }
            });
          } catch (error) {
            console.log(error);
            this.$notify({type: 'error', text: this.$t('notif-error-track-tree-update')});
          }
        } else {
          this.$notify({type: 'warn', text: this.$t('notif-warn-track-tree-order-not-persisted')});
        }
      });
    },

    confirmTrackDeletion(node) {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-track', {name: node.data.name}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteTrack(node)
      });
    },
    async deleteTrack(node) {
      try {
        await Track.delete(node.data.id);
        this.$refs.tree.remove([node.path]);
        this.$emit('deletedTrack', node.data.id);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-track-deletion')});
      }
    }
  },
  created() {
    this.internalSelectedNodes = this.selectedNodes.slice();
    this.makeTree();
  }
};
</script>

<style scoped>
  .add-track-container {
    text-align: center;
    margin-top: 0.5em;
    margin-bottom: 0.5em;
  }
</style>


<style>
  .track-tree {
    padding: 0 0 2px 0;
  }

  .track-tree .tree-checkbox {
    margin-right: 10px;
    color: rgba(0, 0, 0, 0.2);
    font-size: 1rem;
  }

  .track-tree.selector .sl-vue-tree-node-item:hover {
    background: rgba(0, 0, 0, 0.05);
  }

  .track-tree.selector .sl-vue-tree-selected > .sl-vue-tree-node-item {
    background: rgba(0, 0, 0, 0.05);
    font-weight: 600;
  }

  .track-tree .sl-vue-tree-selected > .sl-vue-tree-node-item .tree-checkbox {
    color: #61b2e8;
  }

  .track-tree.selector .sl-vue-tree-gap {
    width: 24px;
  }

  .track-tree.selector .tree-selector {
    cursor: pointer;
    flex-grow: 1;
  }

  .track-tree .tree-selector {
    min-width: 0; /* to allow correct handling of overflow-wrap */
  }

  .track-tree .tree-selector:hover .tree-checkbox {
    color: #61b2e8;
  }

  .track-tree .no-result {
    margin-left: 20px;
    line-height: 1.5;
    font-size: 0.9rem;
  }

  .track-tree .buttons, .track-tree .button {
    margin-bottom: 0 !important;
  }

  .track-tree.editable .sl-vue-tree-sidebar {
    padding-left: 20px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: flex-end;
  }
</style>

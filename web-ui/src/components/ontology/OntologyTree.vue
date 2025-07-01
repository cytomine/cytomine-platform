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
<div class="ontology-tree" :class="{selector: allowSelection, draggable: allowDrag, editable: allowEdition}">
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
        <cytomine-term :term="node.data" />
      </div>
      <div v-else></div>
    </template>

    <template #sidebar="{node}">
      <slot v-if="!node.data.hidden" name="custom-sidebar" :term="node.data">
        <div v-if="allowEdition" class="buttons">
          <button class="button is-small" @click="startTermUpdate(node)">
            <span class="icon is-small">
              <i class="fas fa-edit"></i>
            </span>
          </button>
          <button class="button is-small" @click="confirmTermDeletion(node)">
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

  <div v-if="allowEdition || allowNew" class="add-term-container">
    <button class="button is-small" @click="startTermCreation()">{{$t('add-term')}}</button>
  </div>

</div>
</template>

<script>
import SlVueTree from 'sl-vue-tree';
import CytomineTerm from './CytomineTerm';
import TermModal from './TermModal';
import {Term} from 'cytomine-client';
import {getWildcardRegexp} from '@/utils/string-utils';

export default {
  name: 'ontology-tree',
  model: {
    prop: 'selectedNodes',
    event: 'setSelectedNodes'
  },
  props: {
    ontology: {type: Object},
    additionalNodes: {type: Array, default: () => []},
    startWithAdditionalNodes: {type: Boolean, default: false},
    searchString: {type: String, default: ''},
    selectedNodes: {type: Array, default: () => []},
    allowSelection: {type: Boolean, default: true},
    multipleSelection: {type: Boolean, default: true},
    allowDrag: {type: Boolean, default: false},
    allowEdition: {type: Boolean, default: false},
    allowNew: {type: Boolean, default: false},
    hiddenNodes: {type: Array, default: () => []}
  },
  components: {
    SlVueTree,
    CytomineTerm
  },
  data() {
    return {
      treeNodes: [],
      internalSelectedNodes: [],
      editedNode: null
    };
  },
  computed: {
    regexp() {
      return getWildcardRegexp(this.searchString);
    },
    noResult() {
      return this.treeNodes.every(node => node.data.hidden);
    }
  },
  watch: {
    ontology() {
      this.makeTree();
    },
    additionalNodes() {
      this.makeTree();
    },
    selectedNodes() {
      this.internalSelectedNodes = this.selectedNodes.slice();
      this.refreshNodeSelection();
    },
    regexp() {
      this.filter();
    },
    hiddenNodes() {
      this.filter();
    }
  },
  methods: {
    makeTree() {
      let nodes = (this.ontology) ? this.createSubTree(this.ontology.children.array.slice()) : [];
      let additionalNodes = this.createSubTree(this.additionalNodes.slice());
      this.treeNodes = this.startWithAdditionalNodes ? additionalNodes.concat(nodes) : nodes.concat(additionalNodes);

      this.filter();
    },

    createSubTree(terms) {
      return terms.map(term => this.createNode(term));
    },

    createNode(term) {
      return {
        title: term.name,
        isLeaf: false, // all terms can be used as parent for drag and drop
        isDraggable: this.allowDrag,
        isExpanded: true,
        isSelected: this.internalSelectedNodes.includes(term.id),
        data: {
          id: term.id,
          name: term.name,
          color: term.color,
          parent: term.parent,
          ontology: (this.ontology) ? this.ontology.id : null,
          hidden: false
        },
        children: term.children && term.children.length > 0 ? this.createSubTree(term.children) : []
      };
    },

    filter() {
      this.applyToAllNodes(node => {
        let match = this.regexp.test(node.title) && !this.hiddenNodes.includes(node.data.id);
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
            if (this.internalSelectedNodes.includes(node.data.id)) {
              this.internalSelectedNodes = [];
            } else {
              this.internalSelectedNodes = [node.data.id];
            }

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

    startTermCreation() {
      this.editedNode = null;
      this.openModal();
    },
    createTerm(term) {
      this.treeNodes.push(this.createNode(term));
      this.$emit('newTerm', term);
    },

    startTermUpdate(node) {
      this.editedNode = node;
      this.openModal();
    },
    updateTerm(term) {
      this.$refs.tree.updateNode(this.editedNode.path, {data: {...term}});
    },

    openModal() {
      this.$buefy.modal.open({
        parent: this,
        component: TermModal,
        props: {
          term: this.editedNode ? this.editedNode.data : null,
          ontology: this.ontology
        },
        events: {
          newTerm: this.createTerm,
          updateTerm: this.updateTerm
        },
        hasModalCard: true
      });
    },

    drop(nodes, position) {
      nodes.forEach(async node => {
        let idParent = (position.placement === 'inside') ? position.node.data.id : position.node.data.parent;
        if (node.data.parent !== idParent) {
          try {
            await new Term(node.data).changeParent(idParent);
            this.applyToAllNodes(tmp => {
              if (tmp.data.id === node.data.id) {
                tmp.data.parent = idParent;
              }
            });
          } catch (error) {
            console.log(error);
            this.$notify({type: 'error', text: this.$t('notif-error-ontology-tree-update')});
          }
        } else {
          this.$notify({type: 'warn', text: this.$t('notif-warn-ontology-tree-order-not-persisted')});
        }
      });
    },

    confirmTermDeletion(node) {
      this.$buefy.dialog.confirm({
        title: this.$t('confirm-deletion'),
        message: this.$t('confirm-deletion-term', {name: node.data.name}),
        type: 'is-danger',
        confirmText: this.$t('button-confirm'),
        cancelText: this.$t('button-cancel'),
        onConfirm: () => this.deleteTerm(node)
      });
    },
    async deleteTerm(node) {
      try {
        await Term.delete(node.data.id);
        this.$refs.tree.remove([node.path]);
      } catch (error) {
        console.log(error);
        this.$notify({type: 'error', text: this.$t('notif-error-term-deletion')});
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
.add-term-container {
  text-align: center;
  margin-top: 0.5em;
  margin-bottom: 0.5em;
}
</style>


<style>
.ontology-tree {
  padding: 0 0 2px 0;
}

.ontology-tree .tree-checkbox {
  margin-right: 10px;
  color: rgba(0, 0, 0, 0.2);
  font-size: 1rem;
}

.ontology-tree.selector .sl-vue-tree-node-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

.ontology-tree.selector .sl-vue-tree-selected > .sl-vue-tree-node-item {
  background: rgba(0, 0, 0, 0.05);
  font-weight: 600;
}

.ontology-tree .sl-vue-tree-selected > .sl-vue-tree-node-item .tree-checkbox {
  color: #61b2e8;
}

.ontology-tree.selector .sl-vue-tree-gap {
  width: 24px;
}

.ontology-tree.selector .tree-selector {
  cursor: pointer;
  flex-grow: 1;
}

.ontology-tree .tree-selector {
  min-width: 0; /* to allow correct handling of overflow-wrap */
}

.ontology-tree .tree-selector:hover .tree-checkbox {
  color: #61b2e8;
}

.ontology-tree .no-result {
  margin-left: 20px;
  line-height: 1.5;
  font-size: 0.9rem;
}

.ontology-tree .buttons, .ontology-tree .button {
  margin-bottom: 0 !important;
}

.ontology-tree.editable .sl-vue-tree-sidebar {
  width: 100px;
  padding-left: 20px;
  flex-shrink: 0;
}

.ontology-tree.editable .sl-vue-tree-sidebar {
  display: flex;
  align-items: top;
}
</style>

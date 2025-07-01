/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import {Cytomine} from 'cytomine-client';

export default {
  state() {
    return {
      actions: [],
      undoneActions: []
    };
  },

  mutations: {
    resetActions(state) {
      state.actions = [];
      state.undoneActions = [];
    },

    addAction(state, {annot, type}) {
      let action = {annot, type, command: Cytomine.instance.lastCommand};
      state.actions.push(action);
      state.undoneActions = [];
    },

    undoAction(state, opposedAction) {
      state.actions.pop();
      state.undoneActions.push(opposedAction);
    },

    redoAction(state, opposedAction) {
      state.undoneActions.pop();
      state.actions.push(opposedAction);
    }
  },

  actions: {
    setImageInstance({commit}) {
      commit('resetActions');
    }
  }
};

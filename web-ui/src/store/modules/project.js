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

import listImages from './project_modules/list-images.js';
import listImageGroups from './project_modules/list-image-groups.js';
import listAnnotations from './project_modules/list-annotations.js';

export default {
  namespaced: true,

  state() {
    return {
      id: null,
      name: null
    };
  },

  mutations: {
    setProject(state, project) {
      state.id = project.id;
      state.name = project.name;
    },
  },

  modules: {
    viewers: {
      namespaced: true
    },
    listImages,
    listImageGroups,
    listAnnotations
  }
};

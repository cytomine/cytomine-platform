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

import {
  AnnotationType,
  Cytomine,
  Ontology,
  Project,
  ProjectConnection,
  ProjectMemberRole,
  UserCollection
} from 'cytomine-client';

import {getAllTerms} from '@/utils/ontology-utils';

function getDefaultState() {
  return {
    project: null,
    configUI: {},
    ontology: null,
    managers: [],
    members: [],
    currentViewer: null,
  };
}

export default {
  namespaced: true,

  state: getDefaultState(),

  mutations: {
    resetState(state) {
      Object.assign(state, getDefaultState());
    },

    setProject(state, project) {
      state.project = project;
    },

    setOntology(state, ontology) {
      state.ontology = ontology;
    },

    setConfigUI(state, config) {
      state.configUI = config;
    },

    setManagers(state, managers) {
      state.managers = managers;
    },

    setMembers(state, members) {
      state.members = members;
    },

    setCurrentViewer(state, id) {
      state.currentViewer = id;
    },
  },

  actions: {
    async loadProject({state, dispatch, commit}, idProject) {
      let projectChange = !state.project || state.project.id !== idProject;
      let project = await Project.fetch(idProject);
      commit('setProject', project);
      commit(`projects/${project.id}/setProject`, project, {root: true});

      let promises = [
        dispatch('fetchUIConfig'),
        dispatch('fetchOntology'),
        dispatch('fetchProjectMembers')
      ];

      if (projectChange) {
        promises.push(new ProjectConnection({project: idProject}).save());
      }
      await Promise.all(promises);
    },
    async reloadProject({state, commit}) {
      let project = await Project.fetch(state.project.id);
      commit('setProject', project);
    },

    async updateProject({state, dispatch, commit}, updatedProject) {
      let reloadOntology = state.project.ontology !== updatedProject.ontology;
      commit('setProject', updatedProject);
      commit(`projects/${updatedProject.id}/setProject`, updatedProject, {root: true});
      if (reloadOntology) {
        await dispatch('fetchOntology');
      }
    },

    async fetchUIConfig({state, commit}) {
      let config = await Cytomine.instance.fetchUIConfigCurrentUser(state.project.id);
      commit('setConfigUI', config);
    },

    async fetchProjectMembers({state, commit}) {
      let collection = new UserCollection({
        filterKey: 'project',
        filterValue: state.project.id,
      });

      let members =  (await collection.fetchAll()).array;
      let managers = members.filter(u => u.role === ProjectMemberRole.MANAGER || u.role === ProjectMemberRole.REPRESENTATIVE);

      commit('setManagers', managers);
      commit('setMembers', members);
    },

    async fetchFollowers(_, {userId, imageId}) {
      return await UserCollection.fetchFollowers(userId, imageId);
    },

    async fetchOntology({state, commit}) {
      let ontology = state.project.ontology ? await Ontology.fetch(state.project.ontology) : null;
      commit('setOntology', ontology);
    }
  },

  getters: {
    canEditLayer: (state, getters, rootState) => idLayer => {
      let currentUser = rootState.currentUser.user;
      let project = state.project;
      return getters.canManageProject ||
        (!project.isReadOnly && (idLayer === currentUser.id || !project.isRestricted));
    },

    canEditAnnot: (_, getters, rootState) => annot => {
      let currentUser = rootState.currentUser.user;
      let idLayer = annot.user;
      if (annot.type === AnnotationType.REVIEWED) {
        return currentUser.adminByNow || (!currentUser.guestByNow && annot.reviewUser === currentUser.id);
      }
      return getters.canEditLayer(idLayer);
    },

    canEditImage: (state, getters, rootState) => image => {
      let currentUser = rootState.currentUser.user;
      let project = state.project;
      return getters.canManageProject ||
        (!currentUser.guestByNow && !project.isReadOnly && (image.user === currentUser.id || !project.isRestricted));
    },

    canManageProject: (state, _, rootState) => { // true iff current user is admin or project manager
      let currentUser = rootState.currentUser.user || {};
      return currentUser.adminByNow || (!currentUser.guestByNow && state.managers.some(user => user.id === currentUser.id));
    },

    contributors: (state) => {
      let idsManagers = state.managers.map(user => user.id);
      return state.members.filter(user => !idsManagers.includes(user.id));
    },

    terms: (state) => {
      return state.ontology ? getAllTerms(state.ontology) : null;
    },

    currentProjectModule: (state) => {
      if (!state.project) {
        return null;
      }
      return `projects/${state.project.id}/`;
    },

    currentViewer: (state, _, rootState) => {
      if (!state.project) {
        return null;
      }
      return rootState.projects[state.project.id].viewers[state.currentViewer];
    },

    currentViewerModule: (state, getters) => {
      return `${getters.currentProjectModule}viewers/${state.currentViewer}/`;
    },

    imageModule: (_, getters) => index => {
      return `${getters.currentViewerModule}images/${index}/`;
    }
  }
};

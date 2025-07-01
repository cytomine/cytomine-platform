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

import router from '@/routes';
import {getModuleNamespace} from '@/store/store';
import imageModule from './viewer_modules/image';

export default {
  namespaced: true,

  state() {
    return {
      links: [],
      linkMode: 'ABSOLUTE',
      imageSelector: false,
      activeImage: 0,
      indexNextImage: 0,

      copiedAnnot: null,
      copiedAnnotImageInstance: null
    };
  },

  mutations: {
    setImageSelector(state, value) {
      state.imageSelector = value;
    },

    addImage(state) {
      state.imageSelector = false;
      state.activeImage = state.indexNextImage;
      state.indexNextImage++;
    },

    registerImage(state) {
      state.indexNextImage++;
    },

    setActiveImage(state, index) {
      state.activeImage = index;
    },

    setCopiedAnnot(state, annot) {
      state.copiedAnnot = annot;
    },

    setCopiedAnnotImageInstance(state, image) {
      state.copiedAnnotImageInstance = image;
    },

    setLinkMode(state, mode) {
      state.linkMode = mode;
    },

    // ----- View links

    createLinkGroup(state, indexes) {
      state.links.push(indexes);
    },

    linkImageToGroup(state, {indexGroup, indexImage}) {
      state.links[indexGroup].push(indexImage);
    },


    mergeLinkGroups(state, indexes) {
      state.links[indexes[0]].push(...state.links[indexes[1]]);
      state.links.splice(indexes[1], 1);
    },

    unlinkImage(state, {indexGroup, indexImage}) {
      let links = state.links;
      if (!indexGroup) { // if group not specified, find the group of the provided image
        indexGroup = links.findIndex(group => group.includes(indexImage));
        if (indexGroup === -1) {
          return;
        }
      }

      let group = links[indexGroup];
      let indexWithinGroup = group.findIndex(idx => idx === indexImage);
      group.splice(indexWithinGroup, 1);
      if (group.length === 1) { // if group no longer contains several images, delete it
        links.splice(indexGroup, 1);
      }
    }
  },

  actions: {
    changePath({getters}) {
      let idAnnotation = router.currentRoute.params.idAnnotation;
      let action = router.currentRoute.query.action;
      // eslint-disable-next-line no-unused-vars
      router.replace(getters.pathViewer({idAnnotation, action})).catch(_ => {});
    },

    registerImage({state, commit, getters}) {
      let index = state.indexNextImage;
      commit('registerImage');
      this.registerModule(getters.pathImageModule(index), imageModule);
    },
    async addImage({state, commit, getters, dispatch}, {image, slices, annot = null}) {
      let index = state.indexNextImage;
      commit('addImage');
      this.registerModule(getters.pathImageModule(index), imageModule);
      if (annot) {
        commit(`images/${index}/setRoutedAnnotation`, annot);
      }
      await dispatch(`images/${index}/initialize`, {image, slices});
      dispatch('changePath');
    },

    setImageResolution({state, commit}, {idImage, resolution}) {
      for (let index in state.images) {
        let image = state.images[index].imageInstance;
        if (image && image.id === idImage) {
          commit(`images/${index}/setResolution`, resolution);
        }
      }
    },

    addTerm({state, commit}, term) { // add term to all image wrappers (as all images belong to same project)
      for (let index in state.images) {
        commit(`images/${index}/addTerm`, term);
      }
    },

    refreshTracks({state, dispatch}, {idImage}) {
      for (let index in state.images) {
        let image = state.images[index].imageInstance;
        if (image && image.id === idImage) {
          dispatch(`images/${index}/refreshTracks`);
        }
      }
    },

    setCenter({state, getters, commit}, {index, center}) {
      let relative = state.linkMode === 'RELATIVE';
      let refImage = state.images[index];
      let increments = refImage.view.center.map((val, i) => center[i] - val);
      let refZoom = refImage.imageInstance.zoom - refImage.view.zoom;

      /* Update the center of the linked images */
      let indexesToUpdate = getters.getLinkedIndexes(index);
      indexesToUpdate.forEach(idx => {
        let image = state.images[idx];

        /* Compute the translation needed for a rotated image */
        let u = Math.cos(image.view.rotation) * increments[0] - Math.sin(image.view.rotation) * increments[1];
        let v = Math.cos(image.view.rotation) * increments[1] + Math.sin(image.view.rotation) * increments[0];

        /* Compute the new center of the linked image in absolute mode */
        let newCenter = [
          image.view.center[0] + u,
          image.view.center[1] + v
        ];

        /* Compute the new center of the linked image in relative mode */
        if (relative) {
          let diffZoom = image.imageInstance.zoom - image.view.zoom - refZoom;
          let zoomFactor = Math.pow(2, diffZoom);
          newCenter = [
            image.view.center[0] + u * zoomFactor,
            image.view.center[1] + v * zoomFactor
          ];
        }
        commit(`images/${idx}/setCenter`, newCenter);
      });
    },

    setZoom({state, getters, commit}, {index, zoom}) {
      let relative = state.linkMode === 'RELATIVE';
      let zoomIncrement = zoom - state.images[index].view.zoom;
      let indexesToUpdate = getters.getLinkedIndexes(index);
      indexesToUpdate.forEach(idx => {
        let newZoom = (relative) ? (state.images[idx].view.zoom + zoomIncrement) : zoom;
        commit(`images/${idx}/setZoom`, newZoom);
      });
    },

    setRotation({state, getters, commit}, {index, rotation}) {
      let relative = state.linkMode === 'RELATIVE';
      let rotationInc = rotation - state.images[index].view.rotation + 2 * Math.PI;
      let indexesToUpdate = getters.getLinkedIndexes(index);
      indexesToUpdate.forEach(idx => {
        let newRotation = (relative) ? (state.images[idx].view.rotation + rotationInc) % (2 * Math.PI) : rotation;
        commit(`images/${idx}/setRotation`, newRotation);
      });
    },

    setScaleLineCollapsed({commit}, {index, collapsed}) {
      commit(`images/${index}/setScaleLineCollapsed`, collapsed);
    },

    async refreshData({state, dispatch}) {
      await Promise.all(Object.keys(state.images).map(async index => {
        await dispatch(`images/${index}/refreshData`);
      }));
      dispatch('changePath');
    },

    removeImage({commit, getters, dispatch}, index) {
      commit('unlinkImage', {indexImage: index});
      this.unregisterModule(getters.pathImageModule(index), imageModule);
      dispatch('changePath');
    },
  },

  getters: {
    pathModule: state => {
      return getModuleNamespace(state);
    },

    pathImageModule: (_, getters) => index => {
      return [...getters.pathModule, 'images', index];
    },

    pathViewer: (state, getters) => ({idAnnotation, action} = {}) => {
      // module path = ['projects', idProject, 'viewers', idViewer]
      let idProject = getters.pathModule[1];
      let idViewer = getters.pathModule[3];
      // ---
      let imagesIds = Object.values(state.images).map(img =>
        img.imageInstance ? img.imageInstance.id : 0
      );
      let slicesIds = Object.values(state.images).map(img => {
        if (img.activeSlices && img.activeSlices.length > 0) {
          return img.activeSlices.map(slice => slice.id).join(':');
        }
        return 0;
      });
      let annot = idAnnotation ? `/annotation/${idAnnotation}` : '';
      let actionStr = action ? '&action=' + action : '';
      return `/project/${idProject}/image/${imagesIds.join('-')}/slice/${slicesIds.join('-')}${annot}?viewer=${idViewer}${actionStr}`;
    },

    getLinkedIndexes: state => index => { // find all indexes linked to provided index (if image is not linked, return only its index)
      return state.links.find(group => group.includes(index)) || [index];
    }
  },

  modules: {
    images: {
      namespaced: true
    }
  }
};

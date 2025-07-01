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

import Vue from 'vue';

import Keycloak from './keycloak';
Vue.use(Keycloak);

import VueRouter from 'vue-router';
import router from './routes.js';
Vue.use(VueRouter);

import i18n from './lang.js';

import store from './store/store.js';

import Buefy from 'buefy';
Vue.use(Buefy, {defaultIconPack: 'fas'});

import VeeValidate, {Validator} from 'vee-validate';
Validator.extend('positive', value => Number(value) > 0);
Vue.use(VeeValidate, {
  i18nRootKey: 'validations',
  i18n,
  inject: false
});

import Notifications from 'vue-notification';
Vue.use(Notifications);

import VTooltip from 'v-tooltip';
Vue.use(VTooltip);

import VueMoment from 'vue-moment';
const moment = require('moment');
Vue.use(VueMoment, {moment});

import VueShortKey from 'vue-shortkey';
Vue.use(VueShortKey, {
  prevent: [
    'input[type=text]',
    'input[type=password]',
    'input[type=search]',
    'input[type=email]',
    'textarea',
    '.ql-editor'
  ]
});

import VueHtml2Canvas from 'vue-html2canvas';

Vue.use(VueHtml2Canvas);

import * as vClickOutside from 'v-click-outside-x';
Vue.use(vClickOutside);

import VueLayers from 'vuelayers';
import CytomineSource from './vuelayers-suppl/cytomine-source';
import RasterSource from './vuelayers-suppl/raster-source';
import TranslateInteraction from './vuelayers-suppl/translate-interaction';
import RotateInteraction from './vuelayers-suppl/rotate-interaction';
import ModifyInteraction from './vuelayers-suppl/modify-interaction';
import RescaleInteraction from './vuelayers-suppl/rescale-interaction';
Vue.use(VueLayers);
Vue.use(CytomineSource);
Vue.use(RasterSource);
Vue.use(TranslateInteraction);
Vue.use(RotateInteraction);
Vue.use(ModifyInteraction);
Vue.use(RescaleInteraction);

import Chart from 'chart.js';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import ChartZoom from 'chartjs-plugin-zoom';
Chart.plugins.unregister(ChartZoom);
Chart.plugins.unregister(ChartDataLabels);
Chart.helpers.merge(Chart.defaults.global.plugins.datalabels, {
  anchor: 'end',
  align: 'end',
  offset: 5,
  clamp: true
});

import App from './App.vue';

Vue.config.productionTip = false;
Vue.prototype.$eventBus = new Vue();

Vue.$keycloak
  .init({
    onLoad: 'login-required'
  })
  .then(() => {
    new Vue({
      render: h => h(App),
      router,
      store,
      i18n
    }).$mount('#app');
  });

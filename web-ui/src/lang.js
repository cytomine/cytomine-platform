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
import VueI18n from 'vue-i18n';

Vue.use(VueI18n);

export default new VueI18n({
  locale: 'en',
  fallbackLocale: 'en',
  messages: {
    'en': require('./locales/json/en.i18n.json'),
    'es': require('./locales/json/es.i18n.json'),
    'fr': require('./locales/json/fr.i18n.json'),
    'nl': require('./locales/json/nl.i18n.json')
  }
});

export const changeLanguageMixin = {
  methods: {
    changeLanguage(language) {
      language = language || this.$i18n.fallbackLocale;
      let locale = language.toLowerCase();
      this.$i18n.locale = locale;
      this.$moment.locale(locale);
    }
  }
};

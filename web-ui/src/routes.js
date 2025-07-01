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
import VueRouter from 'vue-router';

// Import Components
import GlobalDashboard from './components/GlobalDashboard.vue';
import ListProjects from './components/project/ListProjects.vue';
import CytomineStorage from './components/storage/CytomineStorage.vue';
import ListOntologies from './components/ontology/ListOntologies.vue';
import ListImages from './components/image/ListImages.vue';
import ListImageGroups from './components/image-group/ListImageGroups.vue';
import ImageInformation from './components/image/ImageInformation.vue';
import ListAnnotations from './components/annotations/ListAnnotations.vue';
import ProjectActivity from './components/project/ProjectActivity.vue';
import ProjectInformation from './components/project/ProjectInformation.vue';
import ProjectConfiguration from './components/project/ProjectConfiguration.vue';
import Account from './components/user/Account.vue';
import AdvancedSearch from './components/search/AdvancedSearch.vue';
import CytomineViewer from './components/viewer/CytomineViewer.vue';
import CytomineProject from './components/project/CytomineProject.vue';
import ProjectHome from './components/project/ProjectHome.vue';
import MemberActivityDetails from './components/project/activity/MemberActivityDetails.vue';
import AdminPanel from './components/admin/AdminPanel.vue';
import AppEngineMainPage from './components/appengine/AppEngineMainPage.vue';
import AppInfoPage from './components/appengine/AppInfoPage.vue';
import UserActivity from './components/user/UserActivity.vue';
import PageNotFound from './components/PageNotFound.vue';

// Define routes
const routes = [
  {
    path: '/',
    component: GlobalDashboard, // Alternative: component: require("./components/GlobalDashboard.vue").default
  },
  {
    path: '/projects',
    component: ListProjects,
  },
  {
    path: '/storage',
    component: CytomineStorage,
  },
  {
    path: '/ontology/:idOntology?',
    component: ListOntologies
  },
  {
    path: '/advanced-search/:searchString?',
    component: AdvancedSearch
  },
  {
    path: '/account',
    component: Account,
  },
  {
    path: '/project/:idProject',
    component: CytomineProject,
    children: [
      {
        path: '',
        component: ProjectHome
      },
      {
        path: 'images',
        component: ListImages
      },
      {
        path: 'image-groups',
        component: ListImageGroups
      },
      {
        path: 'image/:idImages',
        component: CytomineViewer
      },
      {
        path: 'image/:idImages/slice/:idSlices',
        component: CytomineViewer
      },
      {
        path: 'image/:idImage/information',
        component: ImageInformation
      },
      {
        path: 'image/:idImages/annotation/:idAnnotation',
        component: CytomineViewer
      },
      {
        path: 'image/:idImages/slice/:idSlices/annotation/:idAnnotation',
        component: CytomineViewer
      },
      {
        path: 'annotations',
        component: ListAnnotations
      },
      {
        path: 'activity',
        component: ProjectActivity
      },
      {
        path: 'activity/user/:idUser',
        component: MemberActivityDetails
      },
      {
        path: 'information',
        component: ProjectInformation
      },
      {
        path: 'configuration',
        component: ProjectConfiguration
      },
      {
        path: '*',
        component: PageNotFound
      }
    ]
  },
  {
    path: '/activity',
    component: UserActivity
  },
  {
    path: '/admin',
    component: AdminPanel
  },
  // AppEngine 
  {
    path: '/appengine',
    component: AppEngineMainPage
  },
  {
    path: '/appengine/:id',
    component: AppInfoPage,
  },
  {
    path: '/appengine/:namespace/:version',
    component: AppInfoPage,
  },

  // redirections for old URLS
  {path: '/userdashboard', redirect: '/'},
  {path: '/project', redirect: '/projects'},
  {path: '/explorer', redirect: '/'},
  {path: '/upload', redirect: '/storage'},

  {path: '/activity', redirect: '/'},
  {path: '/activity-:idProject-', redirect: '/project/:idProject/activity'},
  {path: '/activity-:idProject-:idUser', redirect: '/project/:idProject/activity/user/:idUser'},

  {path: '/search-', redirect: '/advanced-search'},

  {path: '/admin-tabs-dashboard', redirect: '/admin?tab=dashboard'},
  {path: '/admin-tabs-users', redirect: '/admin?tab=users'},
  {path: '/admin-tabs-groups', redirect: '/'}, // TODO
  {path: '/admin-tabs-permissions', redirect: '/'}, // TODO
  {path: '/admin-tabs-configuration', redirect: '/admin?tab=configuration'},

  {path: '/tabs-dashboard-:idProject', redirect: '/project/:idProject/information'},
  {path: '/tabs-images-:idProject', redirect: '/project/:idProject/images'},
  {path: '/tabs-annotations-:idProject', redirect: '/project/:idProject/annotations'},
  {path: '/tabs-annotationproperties-:idProject-:idAnnot', redirect: '/project/:idProject'},
  {path: '/tabs-imageproperties-:idProject-:idImage', redirect: '/project/:idProject'},
  {path: '/tabs-imageproperties-:idProject-:idImage', redirect: '/project/:idProject'},
  {path: '/tabs-config-:idProject', redirect: '/project/:idProject/configuration'},
  {path: '/tabs-usersconfig-:idProject', redirect: '/project/:idProject/configuration?tab=members'},
  {path: '/tabs-#tabs-useractivity-:idProject-:idUser', redirect: '/project/:idProject/activity/user/:idUser'},
  {path: '/tabs-image-:idProject-:idImage-0', redirect: '/project/:idProject/image/:idImage'},
  {path: '/tabs-image-:idProject-:idImage-:idAnnotation', redirect: '/project/:idProject/image/:idImage/annotation/:idAnnotation'},
  {path: '/tabs-image-:idProject-:idImage-', redirect: '/project/:idProject/image/:idImage'},
  // -----

  {
    path: '*',
    component: PageNotFound
  }
];

// Create router instance
const router = new VueRouter({
  routes: routes,
  linkActiveClass: 'is-active'
});

export default router;

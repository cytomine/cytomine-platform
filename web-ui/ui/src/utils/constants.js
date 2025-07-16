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

export default {
  STOP_PREVIEW_KEYWORD: 'STOP_PREVIEW',
  DIGITAL_ZOOM_INCREMENT: 4,
  // features switches
  APPENGINE_ENABLED: false,
  PRELOADED_SLICES: 50,
  MAX_MERGEABLE_CHANNELS: 36,
  // Refresh intervals (expressed in milliseconds)
  VIEWER_ANNOTATIONS_REFRESH_INTERVAL: 10000,
  MEMBERS_ACTIVITY_REFRESH_INTERVAL: 30000,
  PING_INTERVAL: 20000, // should be lower than 30 seconds (otherwise, not counted in backend)
  TASK_REFRESH_INTERVAL: 2000,
  STORAGE_REFRESH_INTERVAL: 10000,
  ONGOING_UPLOAD_REFRESH_INTERVAL: 500,
  SAVE_POSITION_IN_IMAGE_INTERVAL: 5000, // position also stored each time the user moves in the image
  BROADCASTING_USERS_REFRESH_INTERVAL: 10000,
  TRACKING_REFRESH_INTERVAL: 500,
  ANNOTATION_STROKE_COLOR: [0, 0, 0, 1],
  ANNOTATION_STROKE_SELECT_COLOR: [0, 153, 255, 1],
  MAX_IMAGES_FOR_FILTER: 50000,
  // ---
  IDLE_DURATION: 120, // if the user does not move his mouse on the page during this duration, he is considered as inactive - no more ping (expressed in seconds)
  CONFIG_KEY_WELCOME: 'WELCOME',
  PREFIX_HIDDEN_PROPERTY_KEY: '@',
  DEFAULT_PROPERTY_KEY: '@DEFAULT_PROPERTY',
  DEFAULT_IMAGE_CONTROLS_STEP: 2,
  CATEGORY_ITEMS_PER_BATCH: 10,
  ANNOTATIONS_MAX_ITEMS_PER_CATEGORY: 10,
  // To keep the WebSockets alive by sending the broadcaster viewer position
  WS_POSITION_KEEP_ALIVE_INTERVAL: 10000,
};

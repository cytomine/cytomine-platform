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
<div></div>
</template>


<script>
import {get} from '@/utils/store-helpers';

export default {
  computed: {
    project: get('currentProject/project'),
    configUI: get('currentProject/configUI')
  },
  created() {
    if (this.configUI['project-images-tab']) {
      this.$router.replace(`/project/${this.project.id}/images`);
    } else {
      this.$router.replace(`/project/${this.project.id}/information`);
    }
  }
};
</script>

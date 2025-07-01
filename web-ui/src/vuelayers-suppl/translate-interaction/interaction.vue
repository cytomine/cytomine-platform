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

<script>
/** @module translate-interaction/interaction */
import TranslateInteraction from 'ol/interaction/Translate';
import observableFromOlEvent from 'vuelayers/lib/rx-ext/from-ol-event';
import interaction from 'vuelayers/lib/mixin/interaction';
import {isCollection} from 'vuelayers/lib/ol-ext/util';
import {isFunction} from 'vuelayers/lib/util/minilo';
import {hasInteraction} from 'vuelayers/lib/util/assert';
import {makeWatchers} from 'vuelayers/lib/util/vue-helpers';

/**
 * @vueProps
 */
const props = {
  /**
   * Source or collection identifier from IdentityMap.
   * @type {String}
   */
  source: {
    type: String,
    required: true,
  },
  /**
   * Hit-detection tolerance. Pixels inside the radius around the given position will be checked for features.
   * This only works for the canvas renderer and not for WebGL.
   * @type {number}
   */
  hitTolerance: {
    type: Number,
    default: 0,
  },
};

/**
 * @vueMethods
 */
const methods = {
  /**
   * @return {Promise<ol.interaction.Translate>}
   * @protected
   */
  async createInteraction() {
    let sourceIdent = this.makeIdent(this.source);
    let source = await this.$identityMap.get(sourceIdent, this.$options.INSTANCE_PROMISE_POOL);
    if (isFunction(source.getFeatures)) {
      let features = source.getFeatures();
      if (isCollection(features)) {
        source = features;
      }
    }
    return new TranslateInteraction({
      // source: isVectorSource(source) ? source : undefined,
      features: isCollection(source) ? source : undefined,
      hitTolerance: this.hitTolerance
    });
  },
  /**
   * @return {void}
   * @protected
   */
  mount() {
    interaction.methods.mount.call(this);
  },
  /**
   * @return {void}
   * @protected
   */
  unmount() {
    interaction.methods.unmount.call(this);
  },
  /**
   * @return {void}
   * @protected
   */
  subscribeAll() {
    subscribeToInteractionChanges.call(this);
  },
};

const watch = makeWatchers(['source'], () => function () {
  this.scheduleRecreate();
});

/**
 * @vueProto
 * @alias module:translate-interaction/interaction
 * @title vl-interaction-translate
 */
export default {
  name: 'vl-interaction-translate',
  mixins: [interaction],
  props,
  methods,
  watch,
};

/**
 * @private
 */
function subscribeToInteractionChanges() {
  hasInteraction(this);
  const translateEvents = observableFromOlEvent(this.$interaction, ['translatestart', 'translateend']);
  this.subscribeTo(translateEvents, evt => {
    ++this.rev;
    this.$emit(evt.type, evt);
  });
}
</script>

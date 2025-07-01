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
/** @module rotate-interaction/interaction */
import RotateFeatureInteraction from 'ol-rotate-feature';
import observableFromOlEvent from 'vuelayers/lib/rx-ext/from-ol-event';
import interaction from 'vuelayers/lib/mixin/interaction';
import stylesContainer from 'vuelayers/lib/mixin/styles-container';
import {defaultEditStyle, createStyle} from 'vuelayers/lib/ol-ext/style';
import {isCollection, isVectorSource} from 'vuelayers/lib/ol-ext/util';
import {mapValues, isFunction} from 'vuelayers/lib/util/minilo';
import mergeDescriptors from 'vuelayers/lib/util/multi-merge-descriptors';
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
   * Initial angle in radians (positive is counter-clockwise), applied for features already added to collection.
   * @type {number}
   */
  angle: {
    type: Number,
    default: 0,
  },
};

/**
 * @vueMethods
 */
const methods = {
  /**
   * @return {Promise<ol.interaction.Rotate>}
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
    return new RotateFeatureInteraction({
      source: isVectorSource(source) ? source : undefined,
      features: isCollection(source) ? source : undefined,
      deleteCondition: this.deleteCondition,
      insertVertexCondition: this.insertVertexCondition,
      pixelTolerance: this.pixelTolerance,
      style: this.createStyleFunc(),
      wrapX: this.wrapX,
    });
  },
  /**
   * @return {ol.StyleFunction}
   * @protected
   */
  getDefaultStyles() {
    const defaultStyles = mapValues(defaultEditStyle(), styles => styles.map(createStyle));
    return function __selectDefaultStyleFunc(feature) {
      if (feature.getGeometry()) {
        return defaultStyles[feature.getGeometry().getType()];
      }
    };
  },
  /**
   * @returns {Object}
   * @protected
   */
  getServices() {
    return mergeDescriptors(
      interaction.methods.getServices.call(this),
      stylesContainer.methods.getServices.call(this),
    );
  },
  /**
   * @return {ol.interaction.Interaction|undefined}
   * @protected
   */
  getStyleTarget() {
    return this.$interaction;
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
  /**
   * @param {Array<{style: ol.style.Style, condition: (function|boolean|undefined)}>|ol.StyleFunction|Vue|undefined} styles
   * @return {void}
   * @protected
   */
  setStyle(styles) {
    if (styles !== this._styles) {
      this._styles = styles;
      this.scheduleRefresh();
    }
  },
};

const watch = makeWatchers(['source'], () => function () {
  this.scheduleRecreate();
});

/**
 * @vueProto
 * @alias module:rotate-interaction/interaction
 * @title vl-interaction-rotate
 */
export default {
  name: 'vl-interaction-rotate',
  mixins: [interaction, stylesContainer],
  props,
  methods,
  watch,
};

/**
 * @private
 */
function subscribeToInteractionChanges() {
  hasInteraction(this);
  const rotateEvents = observableFromOlEvent(this.$interaction, ['rotatestart', 'rotateend']);
  this.subscribeTo(rotateEvents, evt => {
    ++this.rev;
    this.$emit(evt.type, evt);
  });
}
</script>

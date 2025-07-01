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
/**
 * @module cytomine-source/source
 */
import TileGrid from 'ol/tilegrid/TileGrid';
import {hasView} from 'vuelayers/lib/util/assert';
import {CustomTile} from 'ol/source/Zoomify';
import tileSource from 'vuelayers/lib/mixin/tile-source';
import TileImage from 'ol/source/TileImage';

const props = {
  extent: {
    type: Array,
  },
  nbResolutions: {
    type: Number,
  },
};

const data = () => {
  return {
    resolutions: null
  };
};

function created() {
  const resolutions = [1];
  for (let i = 1; i <= this.nbResolutions; ++i) {
    resolutions.push(1 << i);
  }
  resolutions.reverse();

  this.resolutions = resolutions;
}

const methods = {
  createTileGrid() {
    // Create a normalized tile grid, inspired from Zoomify.
    hasView(this);

    const extent = this.extent;
    return new TileGrid({
      tileSize: this.tileSize,
      extent: extent,
      origin: [extent[0], extent[3]],
      resolutions: this.resolutions,
    });
  },

  createSource() {
    return new TileImage({
      attributions: this.attributions,
      cacheSize: this.cacheSize,
      crossOrigin: this.crossOrigin,
      maxZoom: this.maxZoom,
      minZoom: this.minZoom,
      opaque: this.opaque,
      projection: this.projection,
      reprojectionErrorThreshold: this.reprojectionErrorThreshold,
      tileClass: CustomTile.bind(null, this._tileGrid),
      tileGrid: this._tileGrid,
      tilePixelRatio: this.tilePixelRatio,
      tileUrlFunction: this.createUrlFunc(),
      tileLoadFunction: this.tileLoadFunction,
      wrapX: this.wrapX,
      transition: this.transition,
    });
  },
};

const watch = {
  url() {
    if (this.$source) {
      this.$source.setTileUrlFunction(this.createUrlFunc());
      this.$source.setTileLoadFunction(this.tileLoadFunction);
      this.scheduleRefresh();
    }
  },
};


export default {
  name: 'vl-source-cytomine',
  mixins: [tileSource],
  props,
  data,
  methods,
  created,
  watch
};
</script>

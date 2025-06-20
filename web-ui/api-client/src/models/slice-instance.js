import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class SliceInstance extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'sliceinstance';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.uploadedFile = null;
    this.path = null;
    this.image = null;
    this.mime = null;

    this.channel = null;
    this.zStack = null;
    this.time = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.path}`;
  }

  /**
   * Fetch the information about the annotation layers present in the image instance
   *
   * @returns {Array<user, slice, countAnnotation, countReviewedAnnotation>} The list of annotation layers with counts
   */
  async fetchAnnotationsIndex() {
    if (this.isNew()) {
      throw new Error('Cannot fetch annotations index of slice with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/annotationindex.json`);
    return data.collection;
  }

  async fetchHistogram({nBins} = {}) {
    if (this.isNew()) {
      throw new Error('Cannot get histogram for a slice with no ID.');
    }
    let params = {nBins};
    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/histogram.json`, {params});
    return data.collection;
  }

  async fetchHistogramBounds() {
    if (this.isNew()) {
      throw new Error('Cannot get histogram bounds for a slice with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/histogram/bounds.json`);
    return data.collection;
  }

  async fetchChannelHistograms({nBins} = {}) {
    if (this.isNew()) {
      throw new Error('Cannot get channel histograms for a slice with no ID.');
    }
    let params = {nBins};
    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/channelhistogram.json`, {params});
    return data.collection;
  }

  async fetchChannelHistogramBounds() {
    if (this.isNew()) {
      throw new Error('Cannot get channel histogram bounds for a slice with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/channelhistogram/bounds.json`);
    return data.collection;
  }
}

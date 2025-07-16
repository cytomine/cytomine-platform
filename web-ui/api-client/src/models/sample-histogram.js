import Model from './model.js';

export default class SampleHistogram extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'samplehistogram';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.slice = null;
    this.sample = null;
    this.min = null;
    this.max = null;
    this.histogram = null;
  }
}

import Collection from './collection.js';
import SampleHistogram from '../models/sample-histogram.js';

export default class SampleHistogramCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return SampleHistogram;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['abstractslice', 'sliceinstance'];
  }
}
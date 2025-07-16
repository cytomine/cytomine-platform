import Collection from './collection.js';
import AbstractSlice from '../models/abstract-slice.js';

export default class AbstractSliceCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AbstractSlice;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['abstractimage', 'uploadedfile'];
  }
}

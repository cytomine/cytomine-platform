import Collection from './collection.js';
import Storage from '../models/storage.js';

export default class StorageCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Storage;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

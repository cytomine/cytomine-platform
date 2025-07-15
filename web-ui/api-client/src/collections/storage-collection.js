import Collection from './collection.js';
import Storage from '../models/storage.js';

export default class StorageCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.all = null;
  }

  /** @inheritdoc */
  static get model() {
    return Storage;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

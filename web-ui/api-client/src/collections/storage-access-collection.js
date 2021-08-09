import Collection from './collection.js';
import StorageAccess from '../models/storage-access.js';

export default class StorageAccessCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return StorageAccess;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'storageaccess';
  }

  get uri() {
    return 'storage/access.json';
  }
}

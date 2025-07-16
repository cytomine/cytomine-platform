import Collection from './collection.js';
import StorageUser from '../models/storage-user';


export default class StorageUserCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return StorageUser;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['storage'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'usersstats';
  }

  get uri() {
    if (this._filter.key === 'storage' && this._filter.value) {
      return `storage/${this._filter.value}/usersstats.json`;
    } else {
      throw new Error('Filter "storage" is mandatory.');
    }
  }
}

import Model from './model.js';

export default class StorageAccess extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'storageaccess';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.id = null;
    this.permission = null;
  }

}

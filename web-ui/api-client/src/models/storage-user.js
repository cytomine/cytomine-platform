import Model from './model.js';

export default class StorageUser extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'storage-user';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.id = null;
    this.username = null;
    this.firstname = null;
    this.lastname = null;
    this.numberOfFiles = null;
    this.totalSize = null;
    this.role = null;
    this.created = null;
  }

}

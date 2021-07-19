import Model from './model.js';

export default class ImageServer extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imageserver';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.url = null;
    this.basePath = null;
    this.available = null;
  }

  /** @override */
  update() {
    throw new Error('Update of image server not implemented in API.');
  }

  /** @override */
  delete() {
    throw new Error('Delete of image server not implemented in API.');
  }
}

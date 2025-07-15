import Model from './model.js';

export default class ImageGroup extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imagegroup';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.project = null;
  }
}

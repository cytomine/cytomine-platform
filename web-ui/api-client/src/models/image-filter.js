import Model from './model.js';

export default class ImageFilter extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imagefilter';
  }

  /** @override */
  update() {
    throw new Error('Update of image filter not implemented in API.');
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.processingServer = null;
    this.baseUrl = null;
  }
}

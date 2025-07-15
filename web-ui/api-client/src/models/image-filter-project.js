import Model from './model.js';

export default class ImageFilterProject extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imagefilterproject';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.imageFilter = null;
    this.project = null;

    // data of the imageFilter, filled by backend
    this.name = null;
    this.processingServer = null;
    this.baseUrl = null;
    // ---
  }

  /** @override */
  fetch() {
    throw new Error('A ImageFilterProject instance cannot be fetched.');
  }

  /** @override */
  update() {
    throw new Error('A ImageFilterProject instance cannot be updated.');
  }
}

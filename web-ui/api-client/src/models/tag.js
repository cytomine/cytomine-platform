import Model from './model.js';

export default class Tag extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'tag';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.user = null;
  }
}

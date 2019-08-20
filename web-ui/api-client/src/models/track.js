import Model from './model.js';

export default class Track extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'track';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.color = null;
  }
}

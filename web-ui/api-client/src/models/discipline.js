import Model from './model.js';

export default class Discipline extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'discipline';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
  }
}

import Model from './model.js';

export default class SoftwareProject extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'softwareproject';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.project = null;
    this.software = null;

    this.name = null; // name of the software, filled by backend based on software value
  }

  /** @override */
  update() {
    throw new Error('A SoftwareProject instance cannot be updated.');
  }
}

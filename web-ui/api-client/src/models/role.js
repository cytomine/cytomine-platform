import Model from './model.js';

export default class Role extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'role';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();
    this.authority = null;
  }

  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.authority}`;
  }

  /** @override */
  update() {
    throw new Error('A Role instance cannot be updated.');
  }

  /** @override */
  delete() {
    throw new Error('A Role instance cannot be deleted.');
  }
}

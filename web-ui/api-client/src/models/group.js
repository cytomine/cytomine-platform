import Model from './model.js';

export default class Group extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'group';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.gid = null;
  }

  // TODO: LDAP
}

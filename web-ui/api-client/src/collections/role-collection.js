import Collection from './collection.js';
import Role from '../models/role.js';

export default class RoleCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Role;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

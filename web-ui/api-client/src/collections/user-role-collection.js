import Collection from './collection.js';
import UserRole from '../models/user-role.js';

export default class UserRoleCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return UserRole;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['user'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'role';
  }
}

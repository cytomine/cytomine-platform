import Collection from './collection.js';
import UserGroup from '../models/user-group.js';

export default class UserGroupCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return UserGroup;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['user'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'group';
  }
}

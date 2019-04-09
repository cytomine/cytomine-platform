import Collection from './collection.js';
import User from '../models/user.js';

export default class UserCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return User;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project', 'ontology'];
  }
}

import Collection from './collection.js';
import Group from '../models/group.js';

export default class GroupCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Group;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

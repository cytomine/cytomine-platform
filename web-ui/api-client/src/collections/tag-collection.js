import Collection from './collection.js';
import Tag from '../models/tag.js';

export default class TagCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Tag;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

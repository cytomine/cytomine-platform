import Collection from './collection.js';
import Generic from '../models/generic.js';

export default class GenericCollection extends Collection {

  _initProperties() {
    this.path = null;
  }

  /** @inheritdoc */
  static get model() {
    return Generic;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  get uri() {
    if (this.path!=null) {
      return this.path;
    }
    else {
      throw new Error('A generic collection must have an "path" defined.');
    }

  }
}

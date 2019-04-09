import Collection from './collection.js';
import Software from '../models/software.js';

export default class SoftwareCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Software;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }
}

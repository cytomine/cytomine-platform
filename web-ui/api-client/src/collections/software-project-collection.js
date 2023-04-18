import Collection from './collection.js';
import SoftwareProject from '../models/software-project.js';

export default class SoftwareProjectCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return SoftwareProject;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }
}

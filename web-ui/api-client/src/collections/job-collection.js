import Collection from './collection.js';
import Job from '../models/job.js';

export default class JobCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.software = null;
    this.project = null;
    this.light = null;
  }

  /** @inheritdoc */
  static get model() {
    return Job;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

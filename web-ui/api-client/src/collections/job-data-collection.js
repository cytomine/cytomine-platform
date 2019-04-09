import Collection from './collection.js';
import JobData from '../models/job-data.js';

export default class JobDataCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return JobData;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null /* admin permission required */, 'job'];
  }
}

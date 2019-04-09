import Collection from './collection.js';
import JobTemplate from '../models/job-template.js';

export default class JobParameterCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return JobTemplate;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }
}

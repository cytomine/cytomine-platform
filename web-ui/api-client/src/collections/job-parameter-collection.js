import Collection from './collection.js';
import JobParameter from '../models/job-parameter.js';

export default class JobParameterCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return JobParameter;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null /* admin permission required */, 'job'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this._filter.key === 'job' && this._filter.value) {
      return `job/${this._filter.value}/parameter.json`;
    }
    else {
      return super.uri;
    }
  }
}

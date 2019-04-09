import Collection from './collection.js';
import UserJob from '../models/user-job.js';

export default class UserJobCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return UserJob;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'userjob';
  }
}

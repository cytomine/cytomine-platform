import Collection from './collection.js';
import ProjectRepresentative from '../models/project-representative.js';

export default class ProjectRepresentativeCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ProjectRepresentative;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'representative';
  }
}

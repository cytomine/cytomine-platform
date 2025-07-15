import Collection from './collection.js';
import ProjectDefaultLayer from '../models/project-default-layer.js';

export default class ProjectDefaultLayerCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ProjectDefaultLayer;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'defaultlayer';
  }
}

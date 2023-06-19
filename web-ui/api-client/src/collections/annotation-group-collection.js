import Collection from './collection.js';
import AnnotationGroup from '../models/annotation-group.js';

export default class AnnotationGroupCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AnnotationGroup;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project', 'imagegroup'];
  }
}

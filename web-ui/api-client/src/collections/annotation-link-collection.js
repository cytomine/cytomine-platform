import Collection from './collection.js';
import AnnotationLink from '../models/annotation-link.js';

export default class AnnotationLinkCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AnnotationLink;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['annotation', 'annotationgroup'];
  }
}

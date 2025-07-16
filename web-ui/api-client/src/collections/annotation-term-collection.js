import Collection from './collection.js';
import AnnotationTerm from '../models/annotation-term.js';

export default class AnnotationTermCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AnnotationTerm;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['annotation'];
  }

  /** @inheritdoc */
  get callbackIdentifier() {
    return 'term';
  }
}

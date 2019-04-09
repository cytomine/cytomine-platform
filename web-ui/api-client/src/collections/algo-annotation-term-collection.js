import Collection from './collection.js';
import AlgoAnnotationTerm from '../models/algo-annotation-term.js';

export default class AlgoAnnotationTermCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AlgoAnnotationTerm;
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

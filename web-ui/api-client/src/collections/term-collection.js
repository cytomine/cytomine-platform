import Collection from './collection.js';
import Term from '../models/term.js';

export default class TermCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Term;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project', 'ontology'];
  }
}

import Collection from './collection.js';
import Ontology from '../models/ontology.js';

export default class OntologyCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.light = null; // if true, only id and name properties will be filled
  }

  /** @inheritdoc */
  static get model() {
    return Ontology;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

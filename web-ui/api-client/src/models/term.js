import Cytomine from '../cytomine.js';
import Model from './model.js';

export default class Term extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'term';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.comment = null;
    this.ontology = null;
    // this.rate = null;
    this.parent = null; // cannot be changed directly; instead, use changeParent() method
    this.color = null;
  }

  async changeParent(idNewParent) {
    if(this.isNew()) {
      throw new Error('Cannot change the parent of a term with no ID.');
    }

    if(this.parent) {
      await Cytomine.instance.api.delete(`relation/parent/term1/${this.parent}/term2/${this.id}.json`);
      this.parent = null;
    }

    if(idNewParent) {
      await Cytomine.instance.api.post('relation/parent/term.json', {term1: idNewParent, term2: this.id});
      this.parent = idNewParent;
    }
  }
}

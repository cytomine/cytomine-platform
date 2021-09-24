import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class Score extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'score';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.values = null;
  }

  async reorder(ids) {
    if(this.isNew()) {
      throw new Error('Cannot reorder values in a score with no ID.');
    }
    await Cytomine.instance.api.put(`${this.callbackIdentifier}/${this.id}/reorder.json?ids=${ids.join(',')}`);
  }
}

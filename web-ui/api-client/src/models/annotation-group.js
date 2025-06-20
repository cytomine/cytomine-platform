import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class AnnotationGroup extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'annotationgroup';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.imageGroup = null;
    this.project = null;
    this.type = null;
  }

  async merge(toMerge) {
    if (this.isNew()) {
      throw new Error('Cannot merge to an annotation group with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/${this.callbackIdentifier}/${toMerge}/merge.json`, {});
    this.populate(data[this.callbackIdentifier]);
    // Cytomine.instance.lastCommand = data.command;
    return this;
  }
}

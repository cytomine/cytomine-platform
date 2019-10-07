import Model from './model.js';
import Cytomine from '@/cytomine';

export default class TrustedSource extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'software_user_repository';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.provider = null;
    this.username = null;
    this.dockerUsername = null;
    this.prefix = null;
  }

  async refresh() {
    if(this.isNew()) {
      throw new Error('Cannot refresh a trusted source with no ID.');
    }

    await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/refresh.json`);
    return self;
  }
}

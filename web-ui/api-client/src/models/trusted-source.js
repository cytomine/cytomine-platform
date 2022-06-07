import Model from './model.js';
import Cytomine from '@/cytomine';

export default class TrustedSource extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'softwareuserrepository';
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

    await Cytomine.instance.api.get(`software_user_repository/${this.id}/refresh.json`);
    return self;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this.isNew()) {
      return 'software_user_repository.json';
    }
    else {
      return `software_user_repository/${this.id}.json`;
    }
  }
}

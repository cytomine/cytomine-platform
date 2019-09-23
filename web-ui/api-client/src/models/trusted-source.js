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
    let {data} = await Cytomine.instance.api.get('software_user_repository/refresh_user_repository.json');
    return data;
  }
}

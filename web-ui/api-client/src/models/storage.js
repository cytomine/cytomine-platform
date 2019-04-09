import Cytomine from '../cytomine.js';
import Model from './model.js';

export default class Storage extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'storage';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.basePath = null;
    this.user = null;
  }

  /**
   * Create a storage for the provided user (name and basePath are set by the backend)
   *
   * @param {number} idUser The identifier of the user
   *
   * @returns {this} The created storage
   */
  static async create(idUser) {
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/create/${idUser}.json`);
    return new this(data);
  }
}

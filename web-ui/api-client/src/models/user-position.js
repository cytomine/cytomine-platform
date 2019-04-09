import Cytomine from '../cytomine.js';
import Model from './model.js';

export default class UserPosition extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'userposition';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.image = null;
    this.project = null;

    this.location = null;
    this.x = null;
    this.y = null;
    this.zoom = null;
    this.rotation = null;
    this.broadcast = null;
  }

  /** @override */
  static async fetch() {
    throw new Error('UserPosition instances cannot be fetched.');
  }

  /** @override */
  async fetch() {
    throw new Error('UserPosition instances cannot be fetched.');
  }

  /**
   * Fetch the last position of a user in an image
   *
   * @param {number} imageInstance    the identifier of the image
   * @param {number} user             the identifier of the user
   * @param {boolean} broadcast       if true, the last position broadcasted by the user will be returned
   * @returns the last position of the user
   */
  static async fetchLastPosition(imageInstance, user, broadcast=false) {
    let uri = `imageinstance/${imageInstance}/position/${user}.json${broadcast ? '?broadcast=true' : ''}`;
    let {data} = await Cytomine.instance.api.get(uri);
    return new this(data);
  }

  /** @override */
  async save() {
    throw new Error('A UserPosition instance cannot be saved. Use the static method create instead.');
  }

  /**
   * @static Record the position of the current user on an image
   *
   * @param {{image, topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, zoom, broadcast}} position
   *
   * @returns {this} The created position
   */
  static async create(position) {
    if(!position || !position.image) {
      throw new Error('The position parameter should have an image property.');
    }
    let image = position.image;
    let {data} = await Cytomine.instance.api.post(`imageinstance/${image}/position.json`, position);
    return new this(data);
  }

  /** @override */
  async update() {
    throw new Error('A UserPosition instance cannot be updated.');
  }

  /** @override */
  async delete() {
    throw new Error('A UserPosition instance cannot be deleted.');
  }
}

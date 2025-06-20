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

  /**
   * Add user to a storage
   *
   * @param {number} idUser The identifier of the user
   * @param {String} permission Permission for this user (READ, WRITE, ADMINISTRATION)
   *
   * @returns {Object} Result (OK) in case of success
   */
  static async addUser(idUser, permission) {
    if (this.isNew()) {
      throw new Error('Cannot add a user from a storage with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`storage/${this.id}/user/${idUser}.json?` + (permission ? `permission=${permission}` : ''));
    return data;
  }

  /**
   * Add users to a storage
   *
   * @param {array} idUsers identifiers of the users
   * @param {String} permissions Permission for these user (READ, WRITE, ADMINISTRATION,...)
   *
   * @returns {Object} Result (OK) in case of success
   */
  async addUsers(idUsers, permission) {
    if (this.isNew()) {
      throw new Error('Cannot add a user to a storage with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`storage/${this.id}/user.json?users=${idUsers.join(',')}` + (permission ? `&permission=${permission}` : ''));
    return data;
  }

  /**
   * Remove user from the storage
   *
   * @param {number} idUser The identifier of the user

   * @returns {Object} Result (OK) in case of success
   */
  static async removeUser(idUser) {
    if (this.isNew()) {
      throw new Error('Cannot delete a user from a storage with no ID.');
    }
    let {data} = await Cytomine.instance.api.delete(`storage/${this.id}/user/${idUser}.json`);
    return data;
  }

  /**
   * Delete users from the storage
   *
   * @param {array} idUsers identifiers of the users
   */
  async deleteUsers(idUsers) {
    if (this.isNew()) {
      throw new Error('Cannot delete a user from a storage with no ID.');
    }
    let {data} = await Cytomine.instance.api.delete(`storage/${this.id}/user.json?users=${idUsers.join(',')}`);
    return data;
  }

  /**
   * Change permission for a user on this storage
   *
   * @param {number} idUser identifier of the user
   * @param {String} permission Permission for this user (READ, WRITE, ADMINISTRATION)
   *
   * @returns {Object} Result (OK) in case of success
   */
  async changePermission(idUser, permission) {
    if (this.isNew()) {
      throw new Error('Cannot change permission to a storage with no ID.');
    }
    let {data} = await Cytomine.instance.api.put(`storage/${this.id}/user/${idUser}.json?` + (permission ? `&permission=${permission}` : ''));
    return data;
  }
}

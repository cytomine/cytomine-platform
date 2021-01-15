import Cytomine from '../cytomine.js';
import Model from './model.js';
import UserCollection from '../collections/user-collection.js';
import RoleCollection from '../collections/role-collection.js';
import Role from './role.js';

export default class User extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'user';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.firstname = null;
    this.lastname = null;
    this.username = null;
    this.email = null;

    this.color = null;

    this.guest = null;
    this.user = null;
    this.admin = null;
    this.algo = null;

    this.sipAccount = null;

    // properties defined only when fetching current user
    this.passwordExpired = null;
    this.publicKey = null;
    this.privateKey = null;
    this.guestByNow = null;
    this.userByNow = null;
    this.adminByNow = null;
  }

  /**
   * @static Fetch the current user
   *
   * @returns {User} The current user
   */
  static async fetchCurrent() { // specific class fot current user?
    let {data} = await Cytomine.instance.api.get('user/current.json');
    let currentUser = new this(data);
    return currentUser;
  }

  /**
   * Fetch the friends of the user (users sharing access to a project)
   *
   * @param {number} [project]    The identifier of the project (if specified, only users having access to this
   *                              project will be included in response)
   * @param {boolean} [offline]   If true, offline users will be included in response
   *
   * @returns {type} Description
   */
  async fetchFriends(project, offline) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the friends of a user with no ID.');
    }

    let params = {};
    if(project) {
      params.project = project;
    }
    if(offline != null) {
      params.offline = offline;
    }

    let {data} = await Cytomine.instance.api.get(`user/${this.id}/friends.json`, {params});
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Fetch a résumé of the activity of the user in the provided project
   *
   * @param {number} idProject The identifier of the project
   *
   * @returns {{firstConnection: String, lastConnection: String, totalAnnotations: Number, totalConnections: Number}}
   *          The résumé of the user activity
   */
  async fetchResumeActivity(idProject) {
    if(this.isNew()) {
      throw new Error('Cannot fetch a resume of activity for a user with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`project/${idProject}/resumeActivity/${this.id}.json`);
    return data;
  }

  /**
   * Fetch the number of annotations for the user in the provided project or in all projects
   *
   * @param {boolean} [reviewed=false]    If true, counts reviewed annotations, if false, user annotations
   * @param {number}  [idProject]         The identifier of the project to consider (if undefined, annotations of all
   *                                      projects will be counted)
   *
   * @returns {number} The retrieved count
   */
  async fetchNbAnnotations(reviewed=false, idProject) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the number of annotations for a user with no ID.');
    }

    let params = {};
    if(idProject) {
      params.project = idProject;
    }

    let annotationPath = 'userannotation';
    if(reviewed) {
      annotationPath = 'reviewedannotation';
    }

    let {data} = await Cytomine.instance.api.get(`user/${this.id}/${annotationPath}/count.json`, {params});
    return data.total;
  }

  /**
   * Regenerate API keys.
   *
   * @returns {this} The updated user (with new API keys)
   */
  async regenerateKeys() {
    this.publicKey = '';
    this.privateKey = '';
    return this.update();
  }

  /**
   * Lock the user
   *
   * @returns {this} The updated user
   */
  async lock() {
    if(this.isNew()) {
      throw new Error('Cannot lock a user with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`user/${this.id}/lock`);
    this.populate(data.user);
    return this;
  }

  /**
   * Unlock the user
   *
   * @returns {this} The updated user
   */
  async unlock() {
    if(this.isNew()) {
      throw new Error('Cannot unlock a user with no ID.');
    }

    let {data} = await Cytomine.instance.api.delete(`user/${this.id}/lock`);
    this.populate(data.user);
    return this;
  }

  /**
   * Define the role of the user
   *
   * @param {number} idRole       The identifier of the role to assign
   * @returns {RoleCollection}    The list of roles associated to the user
   */
  async defineRole(idRole) {
    if(this.isNew()) {
      throw new Error('Cannot define the role of a user with no ID.');
    }
    let {data} = await Cytomine.instance.api.put(`user/${this.id}/role/${idRole}/define.json`);
    let collection = new RoleCollection();
    data.collection.forEach(item => collection.push(new Role(item)));
    return collection;
  }

  /**
   * Check the password of the current user
   *
   * @param {string} password       The password to check
   * @returns {boolean}    Whether or not the password is valid
   */
  static async checkCurrentPassword(password) {
    try {
      await Cytomine.instance.api.post('user/security_check.json', {password});
      return true;
    }
    catch(error) {
      return false;
    }
  }

  /**
   * Change the password of the user
   *
   * @param {string} password       The new password
   * @returns {this}    The user
   */
  async savePassword(password) {
    if(this.isNew()) {
      throw new Error('Cannot change password of a user with no ID.');
    }
    await Cytomine.instance.api.put(`user/${this.id}/password.json`, {password});
    return this;
  }

  // TODO: uncomment once issue in core is solved (ID parameter not correctly handled)
  // /**
  //  * @static Fetch the API keys of the provided user
  //  *
  //  * @param {number} [id]         The identifier of the user (mandatory if publicKey is null)
  //  * @param {string} [publicKey]  The public key of the user (mandatory if id is null)
  //  *
  //  * @returns {{publicKey: String, privateKey: String}} The API keys
  //  */
  // static async fetchKeys(id, publicKey) {
  //   let {data} = await Cytomine.instance.api.get(`userkey/${publicKey}/keys.json?id=${id}`);
  //   return data;
  // }
  //
  // /**
  //  * Fetch the API keys of the user
  //  *
  //  * @returns {{publicKey: String, privateKey: String}} The keys
  //  */
  // async fetchKeys() {
  //   if(this.isNew()) {
  //     throw new Error("Cannot fetch the keys of a user with no ID.");
  //   }
  //   return User.fetchKeys(this.id);
  // }

  // TODO: LDAP
}

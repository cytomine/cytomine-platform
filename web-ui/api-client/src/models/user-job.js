import Cytomine from '../cytomine.js';
import Model from './model.js';
import User from './user.js';

export default class UserJob extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'userJob';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.username = null;
    this.algo = null;

    this.user = null;
    this.humanUsername = null;
    this.job = null;
    this.rate = null;

    this.publicKey = null;
    this.privateKey = null;
  }

  /**
   * @override
   * Save the user job
   *
   * @param {number} [software]   The identifier of the software (mandatory if job property not set)
   * @param {number} [project]    The identifier of the project (mandatory if job property not set)
   *
   * @returns {this} The saved user job, as returned by backend
   */
  async save(software, project) {
    if(this.isNew()) {
      let properties = this.getPublicProperties();
      // HACK
      if(this.user) {
        properties.parent = this.user;
        delete properties.user;
      }
      // -----
      if(software && project) {
        properties.software = software;
        properties.project = project;
      }
      let {data} = await Cytomine.instance.api.post(this.uri, properties);
      this.populate(data[this.callbackIdentifier]);
      Cytomine.instance.lastCommand = data.command;
      return this;
    }
    else {
      return this.update();
    }
  }

  /** @override */
  async update() {
    // let user = await new User(this).update();
    // this.populate(user);
    // return this;
    throw new Error('Update of user job not implemented in API.');
  }

  /** @inheritdoc */
  async delete() {
    return User.delete(this.id);
  }
}

import Model from './model.js';

export default class ProjectDefaultLayer extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'projectdefaultlayer';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.project = null;
    this.user = null;
    this.hideByDefault = null;
  }

  /**
   * @override
   * @static Fetch a default layer
   *
   * @param {number} id       The identifier of the default layer
   * @param {number} project  The identifier of the project
   *
   * @returns {this} The fetched object
   */
  static async fetch(id, project) {
    return new this({id, project}).fetch();
  }

  /**
   * @override
   * @static Delete a default layer
   *
   * @param {number} id       The identifier of the default layer
   * @param {number} project  The identifier of the project
   */
  static async delete(id, project) {
    return new this({id, project}).delete();
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.project) {
      throw new Error('The URI cannot be constructed if the project is not set.');
    }

    if(this.isNew()) {
      return `project/${this.project}/defaultlayer.json`;
    }
    else {
      return `project/${this.project}/defaultlayer/${this.id}.json`;
    }
  }
}

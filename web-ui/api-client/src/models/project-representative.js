import Model from './model.js';

export default class ProjectRepresentative extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'projectrepresentativeuser';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.project = null;
    this.user = null;
  }

  /**
   * @override
   * @static Fetch a project representative
   *
   * @param {number} id       The identifier of the project representative
   * @param {number} project  The identifier of the project
   *
   * @returns {this} The fetched object
   */
  static async fetch(id, project) {
    return new this({id, project}).fetch();
  }

  /**
   * @override
   * @static Delete a project representative
   *
   * @param {number} id       The identifier of the project representative
   * @param {number} project  The identifier of the project
   */
  static async delete(id, project, user) {
    return new this({id, project, user}).delete();
  }

  /** @override */
  update() {
    throw new Error('A ProjectRepresentative instance cannot be updated.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.project) {
      throw new Error('The URI cannot be constructed if the project is not set.');
    }

    if(this.isNew()) {
      return `project/${this.project}/representative.json`;
    }
    else {
      if(this.id) return `project/${this.project}/representative/${this.id}.json`;
      else return `project/${this.project}/representative.json?user=${this.user}`;
    }
  }
}

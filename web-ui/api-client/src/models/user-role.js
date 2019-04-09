import Model from './model.js';

export default class UserRole extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'secusersecrole';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.role = null;

    this.authority = null; // name of the role, filled by backend based on role identifier
  }

  /**
   * @override
   * @static Fetch a userrole relation
   *
   * @param {number} user   The identifier of the user
   * @param {number} role  The identifier of the role
   *
   * @returns {this} The fetched object
   */
  static async fetch(user, role) {
    return new this({id: 0, user, role}).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /**
   * @override
   * @static Delete an association between a user and a role
   *
   * @param {number} user   The identifier of the user
   * @param {number} role  The identifier of the role
   */
  static async delete(user, role) {
    return new this({id: 0, user, role}).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @override */
  update() {
    throw new Error('A UserRole instance cannot be updated.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.user) {
      throw new Error('The URI cannot be constructed if the user is not set.');
    }

    if(this.isNew()) {
      return `user/${this.user}/role.json`;
    }
    else {
      if(!this.role) {
        throw new Error('The URI cannot be constructed if the role is not set.');
      }
      return `user/${this.user}/role/${this.role}.json`;
    }
  }
}

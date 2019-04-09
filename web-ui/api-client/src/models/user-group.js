import Model from './model.js';

export default class UserGroup extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'usergroup';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.group = null;
  }

  /**
   * @override
   * @static Fetch a usergroup relation
   *
   * @param {number} user   The identifier of the user
   * @param {number} group  The identifier of the group
   *
   * @returns {this} The fetched object
   */
  static async fetch(user, group) {
    return new this({id: 0, user, group}).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /**
   * @override
   * @static Delete an association between a user and a group
   *
   * @param {number} user   The identifier of the user
   * @param {number} group  The identifier of the group
   */
  static async delete(user, group) {
    return new this({id: 0, user, group}).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @override */
  update() {
    throw new Error('A UserGroup instance cannot be updated.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.user) {
      throw new Error('The URI cannot be constructed if the user is not set.');
    }

    if(this.isNew()) {
      return `user/${this.user}/group.json`;
    }
    else {
      if(!this.group) {
        throw new Error('The URI cannot be constructed if the group is not set.');
      }
      return `user/${this.user}/group/${this.group}.json`;
    }
  }
}


import Model from './model.js';

export default class ImageGroupImageInstance extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imagegroupimageinstance';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.group = null;
    this.image = null;
  }

  /** @override */
  update() {
    throw new Error('An ImageGroupImageInstance instance cannot be updated.');
  }

  /**
   * @override
   * @static Fetch an image group - image instance relation
   *
   * @param {number} group       The identifier of the group
   * @param {number} image       The identifier of the image
   *
   * @returns {this} The fetched object
   */
  static async fetch(group, image) {
    return new this({id: 0, group, image}).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /**
   * @override
   * @static Delete an image group - image instance relation
   *
   * @param {number} group       The identifier of the group
   * @param {number} image       The identifier of the image
   */
  static async delete(group, image) {
    return new this({id: 0, group, image}).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  get uri() {
    if(!this.group || !this.image) {
      throw new Error('Impossible to construct ImageGroupImageInstance URI with no group ID or image ID.');
    }

    return `imagegroup/${this.group}/imageinstance/${this.image}.json`;
  }
}

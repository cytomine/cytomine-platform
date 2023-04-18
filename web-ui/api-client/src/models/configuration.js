import Model from './model.js';

export default class Configuration extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'configuration';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();
    this.key = null;
    this.value = null;
    this.readingRole = null;
  }

  /**
   * @override
   * @static Fetch a configuration item
   *
   * @param {string} key The key of the configuration item to fetch
   *
   * @returns {this} The fetched configuration
   */
  static async fetch(key) {
    return new this({key}).fetch();
  }

  /** @override */
  isNew() { // to allow deletion and update even if id is not specified (key sufficient)
    return (!this.key);
  }

  /**
   * @override
   * @static Delete a configuration item
   *
   * @param {string} key The key of the configuration item to delete
   */
  static async delete(key) {
    return new this({key}).delete();
  }

  /** @override */
  get uri() {
    if(!this.key) {
      throw new Error('The URI cannot be constructed if the key is not set.');
    }
    return `${this.callbackIdentifier}/key/${this.key}.json`;
  }
}

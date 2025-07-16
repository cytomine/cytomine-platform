import DomainModel from './domain-model.js';

export default class Description extends DomainModel {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'description';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.data = null;
  }

  /** @inheritdoc */
  static async fetch(object) {
    return new this({id: 0}, object).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /** @inheritdoc */
  static async delete(object) {
    return new this({id: 0}, object).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  get uri() {
    return `domain/${this.domainClassName}/${this.domainIdent}/${this.callbackIdentifier}.json`;
  }
}

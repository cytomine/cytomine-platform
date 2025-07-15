import DomainModel from './domain-model.js';

export default class Property extends DomainModel {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'property';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.key = null;
    this.value = null;
  }

}

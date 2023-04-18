import Model from './model.js';

export default class JobParameter extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'jobparameter';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.job = null;
    this.softwareParameter = null;
    this.value = null;

    // following attributes retrieved from corresponding software parameter
    this.name = null;
    this.type = null;
    this.index = null;

    this.uri_ = null;
    this.uriPrintAttribut = null;
    this.uriSortAttribut = null;
  }
}

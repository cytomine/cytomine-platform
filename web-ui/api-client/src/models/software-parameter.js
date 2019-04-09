import Model from './model.js';

export default class SoftwareParameter extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'softwareparameter';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.software = null;
    this.name = null;
    this.type = null;

    this.index = null;
    this.setByServer = null;
    this.defaultParamValue = null;
    this.required = null;

    this.uri_ = null;
    this.uriPrintAttribut = null;
    this.uriSortAttribut = null;
  }
}

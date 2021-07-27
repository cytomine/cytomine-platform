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

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this.isNew()) {
      return 'software_parameter.json';
    }
    else {
      return `software_parameter/${this.id}.json`;
    }
  }
}

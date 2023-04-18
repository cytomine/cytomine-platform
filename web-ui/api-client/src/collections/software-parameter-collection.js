import Collection from './collection.js';
import SoftwareParameter from '../models/software-parameter.js';

export default class SoftwareParameterCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return SoftwareParameter;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null /* admin permission required */, 'software'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this._filter.key === 'software' && this._filter.value) {
      return `software/${this._filter.value}/software_parameter.json`;
    }
    else {
      return 'software_parameter.json';
    }
  }
}

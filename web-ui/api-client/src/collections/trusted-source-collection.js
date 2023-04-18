import Collection from './collection.js';
import TrustedSource from '../models/trusted-source.js';

export default class TrustedSourceCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return TrustedSource;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'software'];
  }

  get uriWithoutFilter() {
    return 'software_user_repository.json';
  }
}

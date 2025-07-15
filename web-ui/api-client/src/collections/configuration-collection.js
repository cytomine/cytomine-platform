import Collection from './collection.js';
import Configuration from '../models/configuration.js';

export default class ConfigurationCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Configuration;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

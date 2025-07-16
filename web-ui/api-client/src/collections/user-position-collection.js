import Collection from './collection.js';
import UserPosition from '../models/user-position.js';

export default class UserPositionCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.user = null;
    this.beforeThan = null;
    this.afterThan = null;
    this.showDetails = true; /** if false, only location, image and zoom properties will be set for the objects of
      * the collection. An additional "frequency" property will also be available. Moreover, location will be returned
      * as an array of coordinates instead of the usual WKT string. */
  }

  /** @inheritdoc */
  static get model() {
    return UserPosition;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['imageinstance'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get callbackIdentifier() {
    return 'positions';
  }
}

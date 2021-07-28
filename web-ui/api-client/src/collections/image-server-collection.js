import Collection from './collection.js';
import ImageServer from '../models/image-server.js';

export default class ImageServerCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ImageServer;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

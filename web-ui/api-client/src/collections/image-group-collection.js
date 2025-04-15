import Collection from './collection.js';
import ImageGroup from '../models/image-group.js';

export default class ImageGroupCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ImageGroup;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }
}

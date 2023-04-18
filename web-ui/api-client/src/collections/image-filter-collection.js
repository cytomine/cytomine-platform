import Collection from './collection.js';
import ImageFilter from '../models/image-filter.js';

export default class ImageFilterCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ImageFilter;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

import Collection from './collection.js';
import ImageGroupImageInstance from '../models/image-group-image-instance.js';

export default class ImageGroupImageInstanceCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ImageGroupImageInstance;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['imagegroup', 'imageinstance'];
  }
}

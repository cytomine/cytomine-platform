import Collection from './collection.js';
import ImageFilterProject from '../models/image-filter-project.js';

export default class ImageFilterProjectCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ImageFilterProject;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }
}

import Collection from './collection.js';
import ImageScore from '../models/image-score.js';

export default class ImageScoreCollection extends Collection {

  _initProperties() {
    this.imageInstance = null;
    this.project = null;
  }

  /** @inheritdoc */
  static get model() {
    return ImageScore;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if (this.imageInstance!=null) {
      return `imageinstance/${this.imageInstance}/image-score.json`;
    }
    else {
      return `project/${this.project}/image-score.json`;
    }

  }
}

import Collection from './collection.js';
import UploadedFile from '../models/uploaded-file.js';

export default class UploadedFileCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return UploadedFile;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

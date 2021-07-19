import Collection from './collection.js';
import CompanionFile from '../models/companion-file.js';

export default class CompanionFileCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return CompanionFile;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['abstractimage', 'uploadedfile'];
  }
}

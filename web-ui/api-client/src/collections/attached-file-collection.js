import DomainCollection from './domain-collection.js';
import AttachedFile from '../models/attached-file.js';

export default class AttachedFileCollection extends DomainCollection {

  /** @inheritdoc */
  static get model() {
    return AttachedFile;
  }
}

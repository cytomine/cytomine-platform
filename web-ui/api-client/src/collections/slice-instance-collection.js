import Collection from './collection.js';
import SliceInstance from '../models/slice-instance.js';

export default class SliceInstanceCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return SliceInstance;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['imageinstance'];
  }
}

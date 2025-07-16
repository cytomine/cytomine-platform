import Collection from './collection.js';
import Track from '../models/track.js';

export default class TrackCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Track;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project', 'imageinstance'];
  }
}

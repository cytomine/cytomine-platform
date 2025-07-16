import Collection from './collection.js';
import AnnotationTrack from '../models/annotation-track.js';

export default class AnnotationTrackCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AnnotationTrack;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['annotation', 'track'];
  }
}

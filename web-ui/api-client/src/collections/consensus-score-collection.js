import Collection from './collection.js';
import ConsensusScore from '../models/consensus-score.js';

export default class ConsensusScoreCollection extends Collection {

  _initProperties() {
    this.imageInstance = null;
    this.project = null;
  }

  /** @inheritdoc */
  static get model() {
    return ConsensusScore;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if (this.imageInstance!=null) {
      return `imageinstance/${this.imageInstance}/consensus-score.json`;
    }
    else {
      return `project/${this.project}/consensus-score.json`;
    }

  }
}

import Model from './model.js';

export default class ConsensusScoreValue extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'consensusscore';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.imageInstance = null;
    this.scoreValue = null;
    this.scoreValueName = null;
    this.user = null;
    this.score = null;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if (this.scoreValue!=null) {
      return `imageinstance/${this.imageInstance}/consensus/${this.score}/value/${this.scoreValue}.json`;
    }
    else {
      return `imageinstance/${this.imageInstance}/consensus/${this.score}.json`;
    }

  }
}

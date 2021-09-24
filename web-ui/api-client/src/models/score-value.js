import Model from './model.js';

export default class ScoreValue extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'score_value';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.score = null;
    this.value = null;
    this.index = null;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this.isNew()) {
      return 'score_value.json';
    }
    else {
      return `score_value/${this.id}.json`;
    }
  }
}

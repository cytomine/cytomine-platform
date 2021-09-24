import Collection from './collection.js';
import ScoreValue from '../models/score-value.js';

export default class ScoreValueCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ScoreValue;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null /* admin permission required */, 'score'];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this._filter.key === 'score' && this._filter.value) {
      return `score/${this._filter.value}/score_value.json`;
    }
    else {
      return 'score_value.json';
    }
  }
}

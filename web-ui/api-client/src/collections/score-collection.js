import Collection from './collection.js';
import Score from '../models/score.js';

export default class ScoreCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return Score;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project', null];
  }

  get uri() {
    if(this._filter.key === 'project' && this._filter.value) {
      return `project/${this._filter.value}/score.json`;
    }
    else {
      return 'score.json';
    }
  }
}

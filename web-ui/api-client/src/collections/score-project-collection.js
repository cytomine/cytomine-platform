import Collection from './collection.js';
import ScoreProject from '../models/score-project.js';

export default class ScoreProjectCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ScoreProject;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }

  get uri() {
    if(this._filter.key === 'project' && this._filter.value) {
      return `project/${this._filter.value}/score_project.json`;
    }
    else {
      return 'score_project.json';
    }
  }
}

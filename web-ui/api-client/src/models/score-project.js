import Model from './model.js';

export default class ImageFilterProject extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'scoreproject';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.score = null;
    this.project = null;

    // data of the score, filled by backend
    this.name = null;
    this.values = null;
  }

  /** @override */
  fetch() {
    throw new Error('A ScoreProject instance cannot be fetched.');
  }

  /** @override */
  update() {
    throw new Error('A ScoreProject instance cannot be updated.');
  }

  get uri() {
    if(this.isNew()) {
      return 'score_project.json';
    }
    else {
      return `score_project/${this.id}.json`;
    }
  }
}

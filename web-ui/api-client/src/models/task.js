import Model from './model.js';

export default class Task extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'task';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.progress = null;
    this.project = null;
    this.user = null;
    this.printInActivity = null;
    this.comments = null;
  }

  /** @override */
  update() {
    throw new Error('Update of tasks not implemented in API.');
  }

  /** @override */
  delete() {
    throw new Error('Deletion of tasks not implemented in API.');
  }
}

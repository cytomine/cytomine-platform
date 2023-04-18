import Job from './job.js';

export default class JobTemplate extends Job {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'jobtemplate';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
  }

  /** @override */
  execute() {
    throw new Error('Cannot execute a job template');
  }

  /** @override */
  fetchAllData() {
    throw new Error('Cannot fetch data associated to a job template');
  }

  /** @override */
  deleteAllData() {
    throw new Error('Cannot delete data associated to a job template');
  }
}

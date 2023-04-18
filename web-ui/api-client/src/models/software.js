import Cytomine from '../cytomine.js';
import Model from './model.js';
import SoftwareParameterCollection from '../collections/software-parameter-collection.js';

export default class Software extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'software';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.serviceName = null;
    this.resultName = null;
    this.description = null;
    this.executeCommand = null;
    this.defaultProcessingServer = null;
    this.deprecated = null;

    this.parameters = null;

    this.numberOfJob = null;
    this.numberOfNotLaunch = null;
    this.numberOfInQueue = null;
    this.numberOfRunning = null;
    this.numberOfSuccess = null;
    this.numberOfFailed = null;
    this.numberOfIndeterminate = null;
    this.numberOfWait = null;
    this.numberOfKilled = null;
  }

  /** @type {SoftwareParameterCollection} */
  get parameters() {
    return this._parameters;
  }

  set parameters(params) {
    this._setCollection(params, '_parameters', SoftwareParameterCollection);
  }

  /**
   * Fetch the statistics related to the software in the provided project
   *
   * @param {number} idProject The identifier of the project
   *
   * @returns {{numberOfJob, numberOfNotLaunch, numberOfInQueue, numberOfRunning, numberOfSuccess, numberOfFailed, numberOfIndeterminate, numberOfWait, numberOfKilled}}
   */
  async fetchStats(idProject) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the statistics of a software with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(`project/${idProject}/software/${this.id}/stats.json`);
    return data;
  }
}

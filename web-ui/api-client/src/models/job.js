import Cytomine from '../cytomine.js';
import Model from './model.js';
import JobParameterCollection from '../collections/job-parameter-collection.js';

/** Enum providing the job status handled in Cytomine */
export const JobStatus = Object.freeze({
  NOTLAUNCH: 0,
  INQUEUE: 1,
  RUNNING: 2,
  SUCCESS: 3,
  FAILED: 4,
  INDETERMINATE: 5,
  WAIT: 6,
  PREVIEWED: 7,
  KILLED: 8
});

export default class Job extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'job';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.software = null;
    this.project = null;
    this.progress = null;
    this.status = null;
    this.statusComment = null;
    this.rate = null;
    this.dataDeleted = null;
    this.algoType = null;

    this.userJob = null;
    this.username = null;

    this.jobParameters = null;

    // following attributes retrieved from corresponding software
    this.softwareName = null;
    this.number = null;
  }

  /** @type {JobParameterCollection} */
  get jobParameters() {
    return this._jobParameters;
  }

  set jobParameters(params) {
    this._setCollection(params, '_jobParameters', JobParameterCollection);
  }

  /** @inheritdoc */
  clone() {
    let clone = super.clone();
    // Reattribute the collection properties so that they are processed through _setCollection method
    clone.jobParameters = JSON.parse(JSON.stringify(this.jobParameters.array));
    // -----
    return clone;
  }

  /** @inheritdoc */
  async save() {
    if(this.isNew()) {
      let properties = this.getPublicProperties();
      if(this._jobParameters && this._jobParameters.length > 0) {
        let params = [];
        for(let param of this._jobParameters) {
          params.push({softwareParameter: param.softwareParameter, value: param.value});
        }
        properties.params = params;
      }
      let {data} = await Cytomine.instance.api.post(this.uri, properties);
      this.populate(data[this.callbackIdentifier]);
      Cytomine.instance.lastCommand = data.command;
      return this;
    }
    else {
      return this.update();
    }
  }

  /**
   * Copy the job and its parameter to create a new job
   *
   * @returns {this} The job created from the original
   */
  async copy() {
    if(this.isNew()) {
      throw new Error('Cannot copy a job with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/copy.json`);
    let job = new Job();
    job.populate(data[this.callbackIdentifier]);
    return job;
  }

  /**
   * Launch the execution of the job
   *
   * @returns {this} The job as returned by backend
   */
  async execute() {
    if(this.isNew()) {
      throw new Error('Cannot execute a job with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/execute.json`);
    this.populate(data[this.callbackIdentifier]);
    return this;
  }

  /**
   * Kill the execution of the job
   *
   * @returns {this} The job as returned by backend
   */
  async kill() {
    if(this.isNew()) {
      throw new Error('Cannot kill a job with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/kill.json`);
    this.populate(data[this.callbackIdentifier]);
    return this;
  }

  /**
   * Fetch the number of each type of data created by the job
   *
   * @returns {{annotations: Number, annotationsTerm: Number, jobDatas: Number, reviewed: Number}}
   */
  async fetchAllData() {
    if(this.isNew()) {
      throw new Error('Cannot fetch the data related to a job with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/alldata.json`);
    return data;
  }

  /**
   * Delete all data created by the job
   *
   * @param {Number} [task] The identifier of the task to associate to the job data deletion
   */
  async deleteAllData(task) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the data related to a job with no ID.');
    }

    await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/alldata.json`, {params: {task}});
    this.dataDeleted = true;
  }

  /**
   * Fetch the log created by the job
   *
   * @returns {AttachedFile}
   */
  async fetchLog() {
    if(this.isNew()) {
      throw new Error('Cannot fetch the log related to a job with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/log.json`);
    return data;
  }
}

import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import Job from '../models/job.js';

export default class JobCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.software = null;
    this.project = null;
    this.light = null;
  }

  /** @inheritdoc */
  static get model() {
    return Job;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  /**
   * @static Fetch bounds of the attributes of all jobs into a project
   *
   * @param {number}  [project]           Identifier of project to consider
   *
   * @returns {{software: {list: Array<String>}, username: {list: Array<String>}}}
   *          The max, min or list of all the jobs properties
   */
  static async fetchBounds({project}={}) {
    let {data} = await Cytomine.instance.api.get(`project/${project}/bounds/job.json`); 
    return data;
  }
}

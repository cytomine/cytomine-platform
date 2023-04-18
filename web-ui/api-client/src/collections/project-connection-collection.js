import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import ProjectConnection from '../models/project-connection.js';

export default class ProjectConnectionCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.user = null;
    this._project = null;
  }

  get project() {
    return this._project;
  }

  set project(value) {
    this._project = value;
    this.setFilter('project', value);
  }

  /** @override */
  setFilter(key, value) {
    super.setFilter(key, value);
    if(key === 'project') {
      this._project = value;
    }
  }

  /** @inheritdoc */
  static get model() {
    return ProjectConnection;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project'];
  }

  /**
   * @returns {String} The URL allowing to download the list of project connections
   */
  get downloadURL() {
    if(!this.user || !this.project) {
      return null;
    }

    return `${Cytomine.instance.host}${Cytomine.instance.basePath}${this.uri}?export=csv`;
  }

  /**
   * Fetches the average number of connections in given periods
   *
   * @param {number}  [project]       The identifier of the project to consider
   * @param {number}  [user]          The identifier of the user to consider
   * @param {number}  [afterThan]     If specified, only connections after this date will be taken into account
   * @param {number}  [beforeThan]       If specified, only connections before this date will be taken into account
   * @param {string}  [period=hour]   The periods to consider
   * @returns {Array<{time, frequency}>} The timestamps defining the period (depends on the period parameter) and the
   *                                     associated relative frequency
   */
  static async fetchAverageConnections({project, user, afterThan, beforeThan, period='hour'}={}) {
    let params = {project, user, afterThan, beforeThan, period};
    let {data} = await Cytomine.instance.api.get('averageConnections.json', {params});
    return data.collection;
  }

  /**
   * Fetches the number of connections in given periods
   *
   * @param {number}  [project]       The identifier of the project to consider
   * @param {number}  [user]          The identifier of the user to consider
   * @param {number}  [afterThan]     If specified, only connections after this date will be taken into account
   * @param {number}  [beforeThan]       If specified, only connections before this date will be taken into account
   * @param {string}  [period=hour]   The periods to consider
   * @returns {Array<{time, frequency}>} The timestamps defining the period and the associated numbers of connections
   */
  static async fetchConnectionsFrequency({project, user, afterThan, beforeThan, period='hour'}={}) {
    let uri = 'connectionFrequency.json';
    if(project) {
      if(user) {
        uri = `project/${project}/connectionFrequency/${user}.json`;
      }
      else {
        uri = `project/${project}/connectionFrequency.json`;
      }
    }
    let params = {project, user, afterThan, beforeThan, period};
    let {data} = await Cytomine.instance.api.get(uri, {params});
    return data.collection;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.user || !this.project) {
      throw new Error('The URI cannot be constructed if the user and/or the project is not set.');
    }

    return `project/${this.project}/connectionHistory/${this.user}.json`;
  }
}

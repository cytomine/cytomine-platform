import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import Project from '../models/project.js';

export default class ProjectCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.light = null; // used iff filter on user ; if true, only id and name properties will be filled
    this.admin = null; // used iff filter on user ; if true, only returns the projects in which user is admin
    this.withLastActivity = null;
    this.withMembersCount = null;
  }

  /** @inheritdoc */
  static get model() {
    return Project;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'user', 'software', 'ontology'];
  }

  /**
   * @static Fetch last opened projects
   *
   * @param {number} [max=0]    The maximum number of items to retrieve
   * @param {number} [offset=0] The offset
   *
   * @returns {Array<{id: Number, date: String, opened: Boolean}>} The last opened projects
   */
  static async fetchLastOpened(max=0, offset=0) {
    let {data} = await Cytomine.instance.api.get(`project/method/lastopened.json?max=${max}&offset=${offset}`);
    return data.collection;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this._filter.key === 'user' && this._filter.value && this.light) {
      return `user/${this._filter.value}/project/light.json`;
    }
    else {
      return super.uri;
    }
  }


  /**
   * @static Fetch bounds of the attributes of all projects
   *
   * @returns {{numberOfAnnotations: {min: Long, max: Long}, numberOfJobAnnotations: {min: Long, max: Long}, numberOfReviewedAnnotations: {min: Long, max: Long}, numberOfImages: {min: Long, max: Long}, members: {min: Long, max: Long}}}
   *          The max, min or list of all the projects properties. members is returned iif withMembersCount parameter is set to true
   */
  static async fetchBounds({withMembersCount=true}={}) {
    let suffix = withMembersCount ? '?withMembersCount=true' : '';
    let {data} = await Cytomine.instance.api.get('bounds/project.json'+ suffix);
    return data;
  }

}

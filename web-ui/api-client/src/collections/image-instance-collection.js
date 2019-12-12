import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import ImageInstance from '../models/image-instance.js';

export default class ImageInstanceCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.withLastActivity = null;
  }

  /** @inheritdoc */
  static get model() {
    return ImageInstance;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return ['project','user'];
  }

  /**
   * @static Fetch last opened image instances
   *
   * @param {number}  [project]           Identifier of project to consider (/!\ requires user parameter to be set)
   * @param {number}  [user]              Identifier of user to consider (/!\ requires project parameter to be set)
   * @param {boolean} [distinctImages]    If true, the result will include distinct images
   * @param {number}  [max=0]             The maximum number of items to retrieve
   * @param {number}  [offset=0]          The offset
   *
   * @returns {Array<{id: Number, date: String, thumb: String, instanceFilename: String, project: Number}>
   *           | Array<{created: String, user: Number, image: Number, time: Number, imageThumb: String,
   *                    imageName: String, countCreatedAnnotations: Number, project: Number}>}
   *          The last opened images (second format used iff project and user are specified)
   */
  static async fetchLastOpened({project, user, distinctImages=true, max=0, offset=0}={}) {
    let uri = project && user ? `project/${project}/user/${user}/imageconsultation.json`
      : 'imageinstance/method/lastopened.json';
    let {data} = await Cytomine.instance.api.get(`${uri}?max=${max}&offset=${offset}&distinctImages=${distinctImages}`);
    return data.collection;
  }

  /**
   * @static Fetch all image instances available to current user
   *
   * @param {number} [max=0]    The maximum number of items to retrieve
   * @param {number} [offset=0] The offset
   *
   * @returns {Array<{id: Number, filename: String, originalFilename: String, projectName: String, project: Number}>}
   *          The list of images (light format)
   */
  static async fetchAllLight({max=0, offset=0}={}) {
    let {data} = await Cytomine.instance.api.get(`user/0/imageinstance/light.json?max=${max}&offset=${offset}`); // {user} value ignored in backend => set to 0
    return data.collection;
  }

  /**
   * @static Fetch bounds of the attributes of all image instances into a project
   *
   * @param {number}  [project]           Identifier of project to consider
   *
   * @returns {{width: {min: Long, max: Long}, height: {min: Long, max: Long}, magnification: {min: Long, max: Long, list: Array<Long>}, resolution: {min: Long, max: Long, list: Array<Long>}, format: {list: Array<String>}, mimeType: {list: Array<String>}}}
   *          The max, min or list of all the image instances properties
   */

  static async fetchBounds({project}={}) {
    let {data} = await Cytomine.instance.api.get(`project/${project}/bounds/imageinstance.json`);
    return data;
  }
}

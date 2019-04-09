import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import AbstractImage from '../models/abstract-image.js';

export default class AbstractImageCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return AbstractImage;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }

  /**
   * @static Fetch all unused abstract images (that is images not used in any project)
   *
   * @returns {this}
   */
  static async fetchUnused() {
    return new this().fetchUnused();
  }

  /**
   * Fetch all unused abstract images (that is images not used in any project) and fills the collection with those
   *
   * @returns {this}
   */
  async fetchUnused() {
    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/unused.json`);
    this._data = data.collection;
    return this;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this._filter.key === 'project' && this._filter.value) {
      return `project/${this._filter.value}/image.json`;
    }
    else {
      return super.uri;
    }
  }
}

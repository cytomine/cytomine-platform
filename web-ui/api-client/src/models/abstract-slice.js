import Cytomine from '../cytomine.js';
import Model from './model.js';
import User from './user.js';

export default class AbstractSlice extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'abstractslice';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.uploadedFile = null;
    this.path = null;
    this.image = null;
    this.mime = null;

    this.channel = null;
    this.zStack = null;
    this.time = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.path}`;
  }

  /**
   * Get the user that uploaded the abstract slice
   *
   * @returns {User}
   */
  async fetchUploader() {
    if (this.isNew()) {
      throw new Error('Cannot get uploader for an abstract slice with no ID.');
    }

    if (!this._uploader) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/user.json`);
      this._uploader = new User(data);
    }

    return this._uploader;
  }
}

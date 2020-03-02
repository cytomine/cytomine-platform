import Cytomine from '../cytomine.js';
import Model from './model.js';
import User from './user.js';
import UploadedFile from './uploaded-file.js';

export default class AbstractImage extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'abstractimage';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.filename = null;
    this.originalFilename = null;
    this.path = null;
    this.fullPath = null;

    this.scanner = null;
    this.mime = null;
    this.sample = null;

    this.width = null;
    this.height = null;
    this.resolution = null;
    this.magnification = null;
    this.depth = null;

    this.thumb = null;
    this.preview = null;
    this.macroURL = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.originalFilename}`;
  }

  /**
  * @returns {String} the preview URL of the image with a specified size
  */
  previewURL(size) {
    return this.preview.replace(/maxSize=\d+/, 'maxSize='+size);
  }

  /**
   * Get the user that uploaded the abstract image
   *
   * @returns {User}
   */
  async fetchUploader() {
    if(this.isNew()) {
      throw new Error('Cannot get uploader for an abstract image with no ID.');
    }

    if(!this._uploader) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/user.json`);
      this._uploader = new User(data);
    }

    return this._uploader;
  }

  /**
   * Get the list of image servers (as URLs) associated with the abstract image
   *
   * @returns {Array<String>}
   */
  async fetchImageServers() {
    if(this.isNew()) {
      throw new Error('Cannot get image servers for an abstract image with no ID.');
    }

    if(!this._imageServers) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/imageservers.json`);
      this._imageServers = data.imageServersURLs;
    }

    return this._imageServers;
  }

  /**
   * Get the uploaded file associated to the image
   *
   * @returns {UploadedFile}
   */
  async fetchUploadedFile() {
    if(this.isNew()) {
      throw new Error('Cannot get the uploaded file for an abstract image with no ID.');
    }

    if(!this._uploadedFile) {
      let {data} = await Cytomine.instance.api.get(`uploadedfile/image/${this.id}.json`);
      this._uploadedFile = new UploadedFile(data);
    }

    return this._uploadedFile;
  }
}

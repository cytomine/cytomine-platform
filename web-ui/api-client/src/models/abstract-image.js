import Cytomine from '../cytomine.js';
import Model from './model.js';
import User from './user.js';

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

    this.scanner = null;
    this.sample = null;
    this.uploadedFile = null;

    this.width = null;
    this.height = null;
    this.depth = null;
    this.time = null;
    this.channels = null;
    this.resolution = null;
    this.magnification = null;
    this.physicalSizeX = null;
    this.physicalSizeY = null;
    this.physicalSizeZ = null;
    this.fps = null;
    this.zoom = null;
    this.contentType = null;

    this.thumb = null;
    this.preview = null;
    this.macroURL = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.originalFilename}`;
  }

  /**
  * Get the preview URL.
  *
  * @param maxSize the desired preview size along largest side
  * @param format the desired preview format (jpg, png, webp)
  * @param otherParameters optional other parameters to include in the preview URL
  * @returns {String} the preview URL of the image with a specified size
  */
  previewURL(maxSize = 256, format = 'jpg', otherParameters = {}) {
    if (this.preview === null) {
      return null;
    }
    let url = this.preview.split('?')[0].split('.').slice(0,-1).join('.');
    let parameters = {maxSize, ...otherParameters};
    let query = new URLSearchParams(parameters).toString();
    return `${url}.${format}?${query}`;
  }

  /**
  * Get the thumbnail URL.
  *
  * @param maxSize the desired thumb size along largest side
  * @param format the desired thumb format (jpg, png, webp)
  * @param otherParameters optional other parameters to include in the thumb URL
  * @returns {String} the thumb URL of the image with a specified size
  */
  thumbURL(maxSize = 256, format = 'jpg', otherParameters = {}) {
    if (this.thumb === null) {
      return null;
    }
    let url = this.thumb.split('?')[0].split('.').slice(0,-1).join('.');
    let parameters = {maxSize, ...otherParameters};
    let query = new URLSearchParams(parameters).toString();
    return `${url}.${format}?${query}`;
  }

  /**
  * Get the associated image URL.
  *
  * @param kind the associated type (macro, label)
  * @param maxSize the desired associated image size along largest side
  * @param format the desired associated image format (jpg, png, webp)
  * @param otherParameters optional other parameters to include in the associated image URL
  * @returns {String} the associated image URL of the image with a specified size
  */
  associatedImageURL(kind = 'macro', maxSize = 256, format = 'jpg', otherParameters = {}) {
    if (this.macroURL === null) {
      return null;
    }
    let url = this.macroURL.split('?')[0].split('.').slice(0,-1).join('.');
    url = url.substr(0, url.lastIndexOf('/'));
    let parameters = {maxSize, ...otherParameters};
    let query = new URLSearchParams(parameters).toString();
    return `${url}/${kind}.${format}?${query}`;
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
   * @returns {String} the download URL of the image (valid iff the identifier of the image was previously defined)
   */
  get downloadURL() {
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}abstractimage/${this.id}/download`;
  }
}

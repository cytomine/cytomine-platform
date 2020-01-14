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
   * Fetch the histogram statistics for the abstract image
   *
   * @returns {Array<sample: Number, min: Number, max: Number>} The histogram statistics
   */
  async fetchHistogramStats() {
    if (this.isNew()) {
      throw new Error('Cannot get histogram statistics for an abstract image with no ID.');
    }

    if(!this._histogramStats) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/histogram/stats.json`);
      this._histogramStats = data.collection;
    }

    return this._histogramStats;
  }

  async extractHistogram() {
    if (this.isNew()) {
      throw new Error('Cannot extract histogram for an image with no ID.');
    }

    await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/histogram/extract.json`);
    return this;
  }

  /**
   * @returns {String} the download URL of the image (valid iff the identifier of the image was previously defined)
   */
  get downloadURL() {
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}abstractimage/${this.id}/download`;
  }
}

import Cytomine from '../cytomine.js';
import Model from './model.js';

export default class JobData extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'jobdata';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.key = null;
    this.job = null;
    this.filename = null;
    this.size = null;

    this._file = null;
  }

  /** @type {File|Blob} */
  get file() {
    return this._file;
  }

  set file(f) {
    if(!(f instanceof Blob)) {
      throw new Error('The provided object should be a Blob instance');
    }
    this._file = f;
  }

  /**
   * @override
   * Save the object (if it is new, it is added; otherwise, it is updated)
   * If the associated file was set, this file is uploaded
   *
   * @returns {this}  The saved job data, as returned by backend
   */
  async save() {
    if(this.isNew()) {
      await super.save();
      if(this._file) {
        await this.upload();
      }
      return this;
    }
    else {
      return this.update();
    }
  }

  /**
   * Upload a file for the job data
   *
   * @param {File|Blob} [file] The file to upload ; if undefined, the file contained in file property will be used
   *
   * @returns {this} The job data, as returned by back-end
   */
  async upload(file) {
    if(this.isNew()) {
      throw new Error();
    }

    if(file) {
      this.file = file;
    }

    if(!this.file) {
      throw new Error('A file should be provided.');
    }

    let formData = new FormData();
    formData.append('files[]', this._file);
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/upload`, formData);
    this.populate(data);
    return this;
  }

  /**
   * @returns {String|null} The URL allowing to download the job data file | null if the ID of the job data is not set
   */
  get downloadURL() {
    if(this.isNew()) {
      return null;
    }
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}${this.callbackIdentifier}/${this.id}/download`;
  }

  /**
   * @returns {String|null} The URL allowing to view the job data file | null if the ID of the job data is not set
   */
  get viewURL() {
    if(this.isNew()) {
      return null;
    }
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}${this.callbackIdentifier}/${this.id}/view`;
  }
}

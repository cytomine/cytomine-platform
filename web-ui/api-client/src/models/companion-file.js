import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class CompanionFile extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'companionfile';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.uploadedFile = null;
    this.path = null;
    this.image = null;
    this.type = null;
    this.originalFilename = null;
    this.filename = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.path}`;
  }

  /**
   * @returns {String} the download URL of the file (valid iff the identifier of the image was previously defined)
   */
  get downloadURL() {
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}companionfile/${this.id}/download`;
  }
}

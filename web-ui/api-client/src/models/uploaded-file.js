import Cytomine from '../cytomine.js';
import Model from './model.js';

/** Enum providing the uploaded file status handled in Cytomine */
export const UploadedFileStatus = Object.freeze({
  UPLOADED: 0,
  DETECTING_FORMAT: 10,
  ERROR_FORMAT: 11,
  EXTRACTING_DATA: 20,
  ERROR_EXTRACTION: 21,
  CONVERTING: 30,
  ERROR_CONVERSION: 31,
  DEPLOYING: 40,
  ERROR_DEPLOYMENT: 41,
  DEPLOYED: 100,
  EXTRACTED: 102,
  CONVERTED: 104
});

export default class UploadedFile extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'uploadedfile';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.projects = null;
    this.storage = null;
    this.imageServer = null;

    this.path = null;
    this.filename = null;
    this.originalFilename = null;
    this.ext = null;

    this.contentType = null;
    this.size = null;

    this.storageId = null;
    this.userId = null;

    this.parent = null;
    this.thumbURL = null;

    this.status = null;
    this.statusText = null;
  }

  get downloadURL() {
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}uploadedfile/${this.id}/download`;
  }

}

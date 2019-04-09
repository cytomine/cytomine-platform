import Cytomine from '../cytomine.js';
import Model from './model.js';

/** Enum providing the uploaded file status handled in Cytomine */
export const UploadedFileStatus = Object.freeze({
  UPLOADED: 0,
  CONVERTED: 1,
  DEPLOYED: 2,
  ERROR_FORMAT: 3,
  ERROR_CONVERT: 4,
  UNCOMPRESSED: 5,
  TO_DEPLOY: 6,
  TO_CONVERT: 7,
  ERROR_DEPLOYMENT: 8
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
    this.storages = null;

    this.path = null;
    this.filename = null;
    this.originalFilename = null;
    this.ext = null;

    this.contentType = null;
    this.size = null;

    this.parent = null;
    this.image = null;
    this.thumbURL = null;

    this.status = null;
  }

  get downloadURL() {
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}uploadedfile/${this.id}/download`;
  }

}

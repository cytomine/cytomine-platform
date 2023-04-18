import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class ProjectConnection extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'imageconsultation';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.project = null;
    this.projectConnection = null;
    this.image = null;
    this.imageName = null;
    this.imageThumb = null;
    this.time = null;
    this.mode = 'view';
  }

  /** @override */
  async save() {
    if(this.isNew()) {
      let {data} = await Cytomine.instance.api.post(this.uri, this.getPublicProperties());
      this.populate(data);
      // TODO: store command ID (currently not returned by backend)
      return this;
    }
    else {
      this.update();
    }
  }

  /** @override */
  fetch() {
    throw new Error('Fetch of image consultation not implemented in API.');
  }

  /** @override */
  update() {
    throw new Error('Update of image consultation not implemented in API.');
  }

  /** @override */
  delete() {
    throw new Error('Deletion of image consultation not implemented in API.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.image) {
      throw new Error('The URI cannot be constructed if the image is not set.');
    }
    return `imageinstance/${this.image}/consultation.json`;
  }
}

import Model from './model.js';
import Cytomine from '../cytomine.js';

export default class ProjectConnection extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'userconnection';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.user = null;
    this.project = null;
    this.time = null;
    this.os = null;
    this.browser = null;
    this.browserVersion = null;
    this.countViewedImages = null;
    this.countCreatedAnnotations = null;
    this.online = null;
  }

  /** @override */
  async save() {
    const {detect} = require('detect-browser');
    const browser = detect();

    if(browser) {
      this.browser = browser.name;
      this.browserVersion = browser.version;
      this.os = browser.os;
    }

    // super.save(); // TODO: uncomment once lack of consistency in core is fixed

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
    throw new Error('Fetch of project connection not implemented in API.');
  }

  /** @override */
  update() {
    throw new Error('Update of project connection not implemented in API.');
  }

  /** @override */
  delete() {
    throw new Error('Deletion of project connection not implemented in API.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(!this.project) {
      throw new Error('The URI cannot be constructed if the project is not set.');
    }
    return `project/${this.project}/userconnection.json`;
  }
}

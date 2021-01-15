import Model from './model.js';

export default class ProcessingServer extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'processingserver';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.host = null;
    this.username = null;
    this.port = null;
  }

  /** @override */
  update() {
    throw new Error('Update of processing server not implemented in API.');
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this.isNew()) {
      return 'processing_server.json';
    }
    else {
      return `processing_server/${this.id}.json`;
    }
  }
}

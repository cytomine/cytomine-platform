import Collection from './collection.js';
import ProcessingServer from '../models/processing-server.js';

export default class ProcessingServerCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return ProcessingServer;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uriWithoutFilter() {
    return 'processing_server.json';
  }
}

import Cytomine from '../cytomine.js';
import Collection from './collection.js';
import ImageConsultation from '../models/image-consultation.js';

export default class ImageConsultationCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.resume = null; // if true, image consultations associated to the same image instance will be aggregated
    this.project = null;
    this.user = null;
    this.projectConnection = null;
  }

  /** @inheritdoc */
  static get model() {
    return ImageConsultation;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  /**
   * @returns {String} The URL allowing to download the list of image consultations
   */
  get downloadURL() {
    let strParam = 'export=csv';
    let paramFields = ['user', 'project'];
    paramFields.forEach(param => {
      if(this[param] != null) {
        strParam += `&${param}=${this[param]}`;
      }
    });
    return `${Cytomine.instance.host}${Cytomine.instance.basePath}${this.uri}?${strParam}`;
  }

  // HACK: remove (temporary hack due to lack of consistency in API endpoint)
  /** @inheritdoc */
  get uri() {
    if(this.resume) {
      return 'imageconsultation/resume.json';
    }

    if(this.projectConnection) {
      return `projectConnection/${this.projectConnection}.json`;
    }

    if(this.user || this.project) {
      return `project/${this.project}/user/${this.user}/imageconsultation.json`;
    }

    throw new Error('The URI cannot be constructed with the provided combination of parameters.');
  }
}

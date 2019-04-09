import Cytomine from '../cytomine.js';
import Model from './model.js';
import Job from './job.js';

export default class JobTemplateAnnotation extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'jobtemplateannotation';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.jobTemplate = null;
    this.annotationIdent = null;

    this.annotationClassName = null; // filled by backend using the annotationIdent value

    this._job = null;
  }

  /**
   * @override
   * Save the Job Template Annotation and create a new Job based on the provided job template and annotation
   *
   * @returns {Job} The job created from the provided job template and annotation
   */
  async save() {
    if(!this.isNew()) {
      throw new Error('This job template annotation already exists.');
    }

    let {data} = await Cytomine.instance.api.post(this.uri, this.getPublicProperties());
    this.populate(data[this.callbackIdentifier]);
    this._job = new Job(data.job);
    Cytomine.instance.lastCommand = data.command;
    return this._job;
  }

  get job() {
    return this._job;
  }

  update() {
    throw new Error('Cannot update a job template annotation.');
  }
}

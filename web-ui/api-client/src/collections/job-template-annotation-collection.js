import Collection from './collection.js';
import JobTemplateAnnotation from '../models/job-template-annotation.js';

export default class JobTemplateAnnotationCollection extends Collection {

  /** @inheritdoc */
  _initProperties() {
    this.jobtemplate = null;
    this.annotation = null;
  }

  /** @inheritdoc */
  static get model() {
    return JobTemplateAnnotation;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }
}

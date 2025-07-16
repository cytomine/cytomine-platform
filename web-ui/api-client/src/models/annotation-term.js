import AbstractAnnotationTerm from './abstract-annotation-term.js';

export default class AnnotationTerm extends AbstractAnnotationTerm {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'annotationterm';
  }

  // alias for annotation property
  get userannotation() {
    return this.annotation;
  }

  set userannotation(annot) {
    this.annotation = annot;
  }

  /** @inheritdoc */
  getPublicProperties() {
    let props = super.getPublicProperties();
    delete props.annotation;
    props.userannotation = this.annotation;
    return props;
  }
}

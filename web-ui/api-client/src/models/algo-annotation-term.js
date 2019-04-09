import AbstractAnnotationTerm from './abstract-annotation-term.js';

export default class AlgoAnnotationTerm extends AbstractAnnotationTerm {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'algoannotationterm';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.annotationClassName = null;
    this.expectedTerm = null;
    this.rate = null;
    this.project = null;
  }

  // alias for annotation property
  get annotationIdent() {
    return this.annotation;
  }

  set annotationIdent(annot) {
    this.annotation = annot;
  }

  /** @inheritdoc */
  getPublicProperties() {
    let props = super.getPublicProperties();
    delete props.annotation;
    props.annotationIdent = this.annotation;
    return props;
  }
}

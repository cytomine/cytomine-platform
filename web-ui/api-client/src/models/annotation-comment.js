import DomainModel from './domain-model.js';
import Annotation, {AnnotationType} from './annotation.js';

export default class AnnotationComment extends DomainModel {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'sharedannotation';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.comment = null;
    this.sender = null;
    this.receivers = null;

    // used for creation only
    this.subject = null;
    this.from = null;
    this.emails = null;
    this.annotationURL = null;
    this.shareAnnotationURL = null;
    // ---
  }

  // aliases
  get annotation() {
    return this._object;
  }
  set annotation(value) {
    this.object = value;
  }
  get annotationIdent() {
    return this.domainIdent;
  }
  set annotationIdent(value) {
    this.domainIdent = value;
  }
  get annotationClassName() {
    return this.domainClassName;
  }
  set annotationClassName(value) {
    this.domainClassName = value;
  }
  // ---

  /** @override */
  set object(obj) {
    if(!(obj instanceof Annotation)) {
      throw new Error('The object must be an annotation instance.');
    }

    if(obj.isNew() || !obj.class) {
      throw new Error('The object must be fetched or saved.');
    }

    if(obj.type !== AnnotationType.USER && obj.type !== AnnotationType.ALGO) {
      throw new Error('Comment functionality not available for this type of annotation.');
    }

    this._object = obj;
    this.domainIdent = obj.id;
    this.domainClassName = obj.class;
  }

  /** @override */
  update() {
    throw new Error('Update of image consultation not implemented in API.');
  }

  /** @override */
  delete() {
    throw new Error('Deletion of image consultation not implemented in API.');
  }

  /** @override */
  get uri() {
    if(!this.domainClassName || !this.domainIdent) {
      throw new Error('The reference object must be defined to construct the URI.');
    }

    let annotationUri = `${this.annotation.type.toLowerCase()}/${this.annotation.id}`;

    if(this.isNew()) {
      return annotationUri + '/comment.json';
    }
    else {
      return `${annotationUri}/comment/${this.id}.json`;
    }
  }

}

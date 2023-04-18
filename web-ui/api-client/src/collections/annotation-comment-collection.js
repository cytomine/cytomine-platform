import DomainCollection from './domain-collection.js';
import AnnotationComment from '../models/annotation-comment.js';

export default class AnnotationCommentCollection extends DomainCollection {

  /** @inheritdoc */
  static get model() {
    return AnnotationComment;
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
  get annotationClassName() {
    return this.domainClassName;
  }
  // ---

  /** @override */
  set object(obj) {
    new AnnotationComment({object: obj}); // same validity checks as for model
    this._object = obj;
  }

  /** @override */
  get uri() {
    if(!this.domainClassName || !this.domainIdent) {
      throw new Error('The reference object must be defined to construct the URI.');
    }

    return `${this.annotation.type.toLowerCase()}/${this.annotation.id}/comment.json`;
  }
}

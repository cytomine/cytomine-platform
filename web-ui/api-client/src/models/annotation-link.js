
import Model from './model.js';

export default class AnnotationLink extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'annotationlink';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.annotationClassName = null;
    this.annotation = null;
    this.group = null;
    this.image = null;
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

  /** @override */
  update() {
    throw new Error('An AnnotationLink instance cannot be updated.');
  }

  /**
   * @override
   * @static Fetch an annotation link
   *
   * @param {number} annotation The identifier of the annotation
   * @param {number} group       The identifier of the group
   *
   * @returns {this} The fetched object
   */
  static async fetch(annotation, group) {
    return new this({id: 0, annotation, group}).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /**
   * @override
   * @static Delete an annotation link
   *
   * @param {number} annotation The identifier of the annotation
   * @param {number} group       The identifier of the group
   */
  static async delete(annotation, group) {
    return new this({id: 0, annotation, group}).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  get uri() {
    if (!this.annotation || !this.group) {
      throw new Error('Impossible to construct Annotation Track URI with no annotation ID or group ID.');
    }

    if (this.isNew()) {
      return 'annotationlink.json';
    } else {
      return `annotationgroup/${this.group}/annotation/${this.annotation}.json`;
    }
  }
}

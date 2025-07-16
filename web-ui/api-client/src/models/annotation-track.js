
import Model from './model.js';

export default class AnnotationTrack extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'annotationtrack';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.annotationClassName = null;
    this.annotation = null;
    this.track = null;
    this.slice = null;
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
    throw new Error('An AnnotationTrack instance cannot be updated.');
  }

  /**
   * @override
   * @static Fetch an annotation track
   *
   * @param {number} annotation The identifier of the annotation
   * @param {number} track       The identifier of the track
   *
   * @returns {this} The fetched object
   */
  static async fetch(annotation, track) {
    return new this({id: 0, annotation, track}).fetch(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  async fetch() {
    this.id = 0; // ID set to 0 to bypass the isNew() verification
    return super.fetch();
  }

  /**
   * @override
   * @static Delete an annotation track
   *
   * @param {number} annotation The identifier of the annotation
   * @param {number} track       The identifier of the track
   */
  static async delete(annotation, track) {
    return new this({id: 0, annotation, track}).delete(); // ID set to 0 to bypass the isNew() verification
  }

  /** @inheritdoc */
  get uri() {
    if (!this.annotation || !this.track) {
      throw new Error('Impossible to construct Annotation Track URI with no annotation ID or track ID.');
    }

    if (this.isNew()) {
      return 'annotationtrack.json';
    } else {
      return `annotationtrack/${this.annotation}/${this.track}.json`;
    }
  }
}

import Cytomine from '../cytomine.js';
import Model from './model.js';

/** Enum providing the annotation types handled in Cytomine */
export const AnnotationType = Object.freeze({
  ALGO: 'AlgoAnnotation',
  USER: 'UserAnnotation',
  REVIEWED: 'ReviewedAnnotation',
  ROI: 'RoiAnnotation'
});

export default class Annotation extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'annotation';
  }

  /** @inheritdoc **/
  _initProperties() {
    super._initProperties();

    this.project = null;
    this.image = null;
    this.slice = null;
    this.user = null;

    this.location = null;
    this.geometryCompression = null;

    this.centroid = null;
    this.area = null;
    this.areaUnit = null;
    this.perimeter = null;
    this.perimeterUnit = null;

    this.reviewed = null;
    this.parentIdent = null; // only used for reviewed annotations
    // this.nbComments = null;

    this.term = null;

    this.imageURL = null;
    this.cropURL = null;
    this.smallCropURL = null;
  }

  /** @type {AnnotationType} */
  get type() {
    return this._type;
  }

  set type(value) {
    if(Object.values(AnnotationType).includes(value)) {
      this._type = value;
    }
    else {
      throw new Error('Invalid assignment: the provided annotation type does not exist.');
    }
  }

  /** @inheritdoc */
  populate(props) {
    super.populate(props);

    if(props && props.class) {
      for(let key in AnnotationType) {
        if(props.class.endsWith(AnnotationType[key])) {
          this._type = AnnotationType[key];
          break;
        }
      }
    }
  }

  /**
   * @override
   * @static Fetch an annotation
   *
   * @param {number} id   The identifier of the annotation to fetch
   * @param {AnnotationType} [annotationType=null]    The type of the annotation (optional but allows to speed up request)
   *
   * @returns {Annotation} The fetched annotation
   */
  static async fetch(id, annotationType = null) {
    let annotation = new this({id});
    if(annotationType) {
      annotation.type = annotationType;
    }
    return annotation.fetch();
  }

  /**
   * Get the profile of the annotation, if available
   */
  async fetchProfile() {
    if(this.isNew()) {
      throw new Error('Cannot get profile for an annotation with no ID.');
    }

    if(!this._profile) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/profile.json`);
      this._profile = data;
    }

    return this._profile;
  }

  /**
   * Get the projections of the annotation profile, if available.
   *
   * @param {null|string} axis The axis along which the projection is performed. If null, use highest order axis
   * @param {boolean} cache True if the result must be cached in the annotation object.
   *
   * @returns {Object} The annotation profile projection
   */
  async fetchProfileProjections(axis= null, cache= false) {
    if(this.isNew()) {
      throw new Error('Cannot get profile for an annotation with no ID.');
    }

    if (this._profileProjections && cache) {
      return this._profileProjections;
    }
    else {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/profile/projections.json`,
        {params: {axis}});
      if (cache) {
        this._profileProjections = data;
      }
      return data;
    }
  }

  /**
   * Record an action performed on the annotation
   *
   * @param {string} [action="select"] The action performed on the annotation (select, add, delete, update)
   *
   * @returns {Object} The annotation action
   */
  async recordAction(action='select') {
    if(this.isNew()) {
      throw new Error('Cannot record an action on an annotation with no ID.');
    }

    let {data} = await Cytomine.instance.api.post('annotationaction.json', {
      annotationIdent: this.id,
      action
    });
    return data;
  }

  /**
   * Create a reviewed annotation based on the current annotation
   *
   * @param {Array<Number>} terms The identifiers of the terms to associate to the annotation
   *
   * @returns {Annotation} The newly created reviewed annotation
   */
  async review(terms) {
    if(this.isNew()) {
      throw new Error('Cannot review an annotation with no ID.');
    }

    let {data} = await Cytomine.instance.api.put(`${this.callbackIdentifier}/${this.id}/review.json`, {terms});
    let reviewedAnnotation = new this.constructor(data['reviewedannotation']);
    Cytomine.instance.lastCommand = data.command;
    return reviewedAnnotation;
  }

  /**
   * Cancel the review of the current annotation (thus deleting associated reviewed annotations)
   */
  async cancelReview() {
    if(this.isNew()) {
      throw new Error('Cannot cancel review an annotation with no ID.');
    }

    await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/review.json`);
  }

  /**
   * Simplifies the annotation so that it is described with a number of points between minNbPoints and maxNbPoints (no
   * guarantee, best effort)
   *
   * @param {number} minNbPoints
   * @param {number} maxNbPoints
   *
   * @returns {Annotation} The simplified annotation
   */
  async simplify(minNbPoints, maxNbPoints) {
    if(this.isNew()) {
      throw new Error('Cannot simplify an annotation with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/simplify.json?
            minPoint=${minNbPoints}&maxPoint=${maxNbPoints}`);

    this.populate(data);
    return this;
  }

  /**
   * Fills the annotation
   *
   * @returns {Annotation} The filled annotation
   */
  async fill() {
    if(this.isNew()) {
      throw new Error('Cannot fill an annotation with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/fill`);
    this.populate(data.data.annotation || data.data.reviewedannotation);
    Cytomine.instance.lastCommand = data.command;
    return this;
  }

  /**
   * @static Correct annotation(s) with freehand drawing
   *
   * @param {number} image    The identifier of the image instance
   * @param {string} location The correcting geometric object, described in WKT format
   * @param {boolean} review  If set to true, the correction is to be applied on reviewed annotations if possible
   * @param {boolean} remove  If true, the correcting geometric object will be removed from the existing annotations
   *                          If false, it will be added to them
   * @param {number} annotation   The identifier of the annotation to correct (if specified, only the targetted
   *                              annotation will be affected by the correction)
   * @param {Array<Number>} layers   The identifiers of the layers to correct
   *
   * @returns {Annotation} One of the corrected annotations
   */
  static async correctAnnotations({image, location, review, remove, layers, annotation}) {
    let params = {image, location, review, remove, layers, annotation};
    let {data} = await Cytomine.instance.api.post('annotationcorrection.json', params);
    Cytomine.instance.lastCommand = data.command;
    return new this(data.annotation || data.reviewedannotation);
  }

  async repeat(slice, number) {
    if(this.isNew()) {
      throw new Error('Cannot repeat an annotation with no ID.');
    }

    let {data} = await Cytomine.instance.api.post(`userannotation/${this.id}/repeat.json`, {
      slice,
      repeat: number
    });
    return data;
  }

  /** @inheritdoc */
  get uri() { // cannot override callbackIdentifier because in response data, model is returned in "annotation" prop
    if(!this._type) {
      return super.uri;
    }
    else {
      let name = this._type.toLowerCase();
      if(this.isNew()) {
        return `${name}.json`;
      }
      else {
        return `${name}/${this.id}.json`;
      }
    }
  }
}

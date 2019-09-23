import Model from '../models/model.js';
import Collection from './collection.js';

export default class DomainCollection extends Collection {

  /**
   * Create a collection containing metadata objects characterizing a reference existing model instance
   *
   * @param {Model}   object          The reference model instance
   * @param {number}  [nbPerPage=0]   The maximum number of items fetched at a time (if set to 0, all items will be fetched at once)
   * @param {string}  [filterKey]     The filter key
   * @param {number}  [filterValue]   The filter value (an identifier)
   * @param {Object}  [props]         Properties of the collection to set (the allowed props are model-dependent and
   *                                  defined in _initProperties())
   */
  constructor({object, nbPerPage=0, filterKey, filterValue, ...props}={}) {
    if (new.target === DomainCollection) {
      throw new Error('DomainCollection is an abstract class and cannot be constructed directly.');
    }

    super({nbPerPage, filterKey, filterValue, ...props});
    if(object) {
      this.object = object;
    }
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null];
  }

  /**
   * Set the reference object
   *
   * @param {Model} obj The reference object
   */
  set object(obj) {
    if(!(obj instanceof Model)) {
      throw new Error('The object must be a model instance.');
    }

    if(obj.isNew() || !obj.class) {
      throw new Error('The object must be fetched or saved.');
    }

    this._object = obj;
  }

  /**
   * The class name of the reference object
   * @type {string}
   */
  get domainClassName() {
    return this._object ? this._object.class : null;
  }

  /**
   * The identifier of the reference object
   * @type {number}
   */
  get domainIdent() {
    return this._object.id;
  }

  /** @inheritdoc */
  get uri() {
    if(!this.domainClassName || !this.domainIdent) {
      throw new Error('The reference object must be defined to construct the URI.');
    }

    return `domain/${this.domainClassName}/${this.domainIdent}/${this.callbackIdentifier}.json`;
  }

}

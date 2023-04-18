import Cytomine from '../cytomine.js';

export default class Model {

  /**
   * @param {Object} [props]    Object containing the properties of the object to set
   */
  constructor(props) {
    if (new.target === Model) {
      throw new Error('Model is an abstract class and cannot be constructed directly.');
    }

    this._initProperties();
    this.populate(props);
  }

  /**
   * Initialize the properties allowed for current object (the children must override this method to initialize their
   * custom properties)
   */
  _initProperties() {
    this.id = null;
    this.class = null; // the Cytomine class
    this.created = null;
    this.updated = null;
    this.deleted = null;
  }

  toString() {
    let str = `[${this.callbackIdentifier}] ${this.id}`;
    if(this.name) {
      str += `: ${this.name}`;
    }
    return str;
  }

  /**
   * Populate the instance with the properties of the provided object
   *
   * @param {Object} props Object containing the properties to set
   */
  populate(props) {
    if(props) {
      for(let key in props) {
        let value = props[key];
        if(key === 'uri') { // special handling to avoid conflict with uri property
          key = 'uri_';
        }
        this[key] = value;
      }
    }
  }

  /**
   * Deep clone the object
   *
   * @returns {this} the clone of the object
   */
  clone() {
    return new this.constructor(JSON.parse(JSON.stringify(this)));
  }

  /**
   * Set a property of the model as a collection of other models
   *
   * @param {null|Collection|Array<Model>} value  The value to assign to the property
   * @param {string} property         The name of the property that contains the collection
   * @param {Class} collectionClass   The expected collection class
   */
  _setCollection(value, property, collectionClass) {
    if(!value) {
      this[property] = new collectionClass();
    }
    else if(value instanceof collectionClass) {
      this[property] = value;
    }
    else if(Array.isArray(value)) {
      let collection = new collectionClass();
      value.forEach(obj => {
        if(!(obj instanceof collectionClass.model)) {
          obj = new collectionClass.model(obj);
        }
        collection.push(obj);
      });
      this[property] = collection;
    }
    else {
      throw new Error(`${property} value must be null, a ${collectionClass.name} instance
                or an array of ${collectionClass.model.name} instances.`);
    }
  }

  /**
   * Return an object containing only the public properties of the current object
   *
   * @returns {Object} Object with public properties only
   */
  getPublicProperties() {
    let props = {};
    for(let key in this) {
      let value = this[key];
      if(!key.startsWith('_') && value != null) {
        if(key === 'uri_') { // special handling of uri_ to avoid conflict with uri property
          key = 'uri';
        }
        props[key] = value;
      }
    }
    return props;
  }

  /**
   * @static Fetch an object
   *
   * @param {number} id The identifier of the object to fetch
   *
   * @returns {this} The fetched object
   */
  static async fetch(id) {
    return new this({id}).fetch();
  }

  /**
   * Fetch from database the properties of the model and update the model with those properties
   *
   * @returns {this} The object with fetched properties
   */
  async fetch() {
    if(this.isNew()) {
      throw new Error('Cannot fetch a model with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(this.uri);

    this.populate(data);
    return this;
  }

  /**
   * Save the object (if it is new, it is added; otherwise, it is updated)
   *
   * @returns {this} The saved object, as returned by backend
   */
  async save() {
    if(this.isNew()) {
      let {data} = await Cytomine.instance.api.post(this.uri, this.getPublicProperties());
      this.populate(data[this.callbackIdentifier]);
      Cytomine.instance.lastCommand = data.command;
      return this;
    }
    else {
      return this.update();
    }
  }

  /**
   * Update the object
   *
   * @returns {this} The updated object, as returned by backend
   */
  async update() { // allow to provide parameters ?
    if(this.isNew()) {
      throw new Error('Cannot update a model with no ID.');
    }

    let {data} = await Cytomine.instance.api.put(this.uri, this.getPublicProperties());
    this.populate(data[this.callbackIdentifier]);
    Cytomine.instance.lastCommand = data.command;
    return this;
  }

  /**
   * @static Delete an object
   *
   * @param {number} id The identifier of the object to delete
   */
  static async delete(id) {
    return new this({id}).delete();
  }

  /**
   * Delete the object
   */
  async delete() {
    if(this.isNew()) {
      throw new Error('Cannot delete a model with no ID.');
    }

    let {data} = await Cytomine.instance.api.delete(this.uri);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * @returns {boolean} whether or not the object is new (not yet added to the database)
   */
  isNew() {
    return (this.id == null);
  }

  /**
   * @returns {string} API URI to use to perform operations on the object
   */
  get uri() {
    if(this.isNew()) {
      return `${this.callbackIdentifier}.json`;
    }
    else {
      return `${this.callbackIdentifier}/${this.id}.json`;
    }
  }

  /**
   * @abstract
   * @returns {string} The callback identifier of the model used in API requests
   */
  static get callbackIdentifier() {
    throw new Error('Abstract getter callbackIdentifier() not overriden in child.');
    // return this.name.toLowerCase(); not used to allow minification
    // (see https://stackoverflow.com/questions/29310530/get-the-class-name-of-es6-class-instance#39522406)
  }

  get callbackIdentifier() {
    return this.constructor.callbackIdentifier;
  }
}

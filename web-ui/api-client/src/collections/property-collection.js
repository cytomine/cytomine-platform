import Cytomine from '../cytomine.js';
import DomainCollection from './domain-collection.js';
import Property from '../models/property.js';

export default class PropertyCollection extends DomainCollection {

  /** @inheritdoc */
  static get model() {
    return Property;
  }

  /**
   * @static Fetch the keys (or the keys/users associations) of all annotation properties in a project or image
   *
   * @param {number} [idProject]  The project identifier (mandatory if idImage is null)
   * @param {number} [idImage]    The image identifier (mandatory if idProject is null)
   * @param {boolean} [user=false]    If true, will fetch the key/user association instead of the keys only
   * @param {number} [max=0]          The maximum number of items to return (if set to 0, all items will be returned)
   * @param {number} [offset=0]       The offset (0=first record)
   *
   * @returns {Array<{key: String, userId: String}> | Array<String>}
   */
  static async fetchKeysAnnotationProperties(idProject, idImage, user=false, max=0, offset=0) {
    let {data} = await Cytomine.instance.api.get('annotation/property/key.json', {params: {
      idProject, idImage, user, max, offset
    }});
    return data.collection;
  }

  /**
   * @static Fetch the keys of all image properties in a project
   *
   * @param {number} idProject    The project identifier
   * @param {number} [max=0]      The maximum number of items to return (if set to 0, all items will be returned)
   * @param {number} [offset=0]   The offset (0=first record)
   *
   * @returns {Array<String>}
   */
  static async fetchKeysImageProperties(idProject, max=0, offset=0) {
    let {data} = await Cytomine.instance.api.get('imageinstance/property/key.json', {params: {idProject, max, offset}});
    return data.collection;
  }

  /**
   * @static Fetch the values and positions of all properties with the provided key in a given image and layer
   *
   * @param {number}   idUser     The user identifier (targetting the layer)
   * @param {number}   idImage    The image identifier
   * @param {number}   key        The key
   * @param {string} [bbox]       The bbox to consider (properties related to annotations outside this bbox will be ignored)
   * @param {number} [max=0]      The maximum number of items to return (if set to 0, all items will be returned)
   * @param {number} [offset=0]   The offset (0=first record)
   *
   * @returns {Array<{idAnnotation: Number, x: Number, y: Number, value: String}>}
   */
  static async fetchPropertiesValuesAndPositions(idUser, idImage, key, bbox, max=0, offset=0) {
    let params = {key};
    if(bbox) {
      params.bbox = bbox;
    }
    if(max) {
      params.max = max;
    }
    if(offset) {
      params.offset = offset;
    }
    let {data} = await Cytomine.instance.api.get(`user/${idUser}/imageinstance/${idImage}/annotationposition.json`, {params});
    return data.collection;
  }
}

import Collection from './collection.js';
import User from '../models/user.js';
import Cytomine from '../cytomine.js';
export default class UserCollection extends Collection {

  /** @inheritdoc */
  static get model() {
    return User;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project', 'ontology'];
  }

  /**
   * @static Fetch followers
   *
   * @param {String} userId  The user id of user who is followed
   * @param {String} imageId The image id
   *
   * @returns {UserCollection} Collection of users following user (userId) on image (imageId)
   */
  static async fetchFollowers(userId, imageId) {
    let {data} = await Cytomine.instance.api.get(`imageinstance/${imageId}/followers/${userId}.json`);
    return data.collection;
  }
}
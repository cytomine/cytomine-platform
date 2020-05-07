import Cytomine from '../cytomine.js';
import Model from './model.js';
import User from './user.js';
import UserCollection from '../collections/user-collection.js';
import {AnnotationType} from './annotation.js';
import axios from 'axios';

/** Enum providing the project member roles handled in Cytomine */
export const ProjectMemberRole = Object.freeze({
  CONTRIBUTOR: 'contributor',
  MANAGER: 'manager',
  REPRESENTATIVE: 'representative'
});

export default class Project extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'project';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.name = null;
    this.ontology = null;
    this.ontologyName = null;
    this.discipline = null;
    this.disciplineName = null;

    this.numberOfSlides = null;
    this.numberOfImages = null;
    this.numberOfAnnotations = null;
    this.numberOfJobAnnotations = null;
    this.numberOfReviewedAnnotations = null;

    this.retrievalDisable = null;
    this.retrievalAllOntology = null;
    this.retrievalProjects = null;

    this.blindMode = null;
    this.isClosed = null;
    this.isReadOnly = null;
    this.isRestricted = null;

    this.hideUsersLayers = null;
    this.hideAdminsLayers = null;
  }

  /**
   * Fetch the user that created the project
   *
   * @returns {User}
   */
  async fetchCreator() {
    if(this.isNew()) {
      throw new Error('Cannot get creator of a project with no ID.');
    }

    if(!this._creator) {
      let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/creator.json`);
      this._creator = new User(data.collection[0]);
    }

    return this._creator;
  }

  /**
   * Fetch all project users
   *
   * @param {boolean} [online]    If true, only online users are included in response
   * @param {boolean} [showJob]   If true, the user jobs are included in response
   *
   * @returns {UserCollection} the collection of project users
   */
  async fetchUsers(online, showJob) {
    if(this.isNew()) {
      throw new Error('Cannot fetch users of a project with no ID.');
    }

    let params = {};
    if(online != null) {
      params.online = online;
    }
    if(showJob != null) {
      params.showJob = showJob;
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/user.json`, {params});
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Fetch the connected project users and their current positions (opened images in this project)
   *
   * @returns {Array<{id: , position: Array<{id: , image: , filename: , originalFilename: , date: }>}>}
   */
  async fetchConnectedUsers() {
    if(this.isNew()) {
      throw new Error('Cannot fetch connected users of a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/online/user.json`);
    return data.collection;
  }

  /**
   * Fetch all project administrators
   *
   * @returns {UserCollection} the collection of project administrators
   */
  async fetchAdministrators() {
    if(this.isNew()) {
      throw new Error('Cannot fetch administrators of a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/admin.json`);
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Fetch all project representatives
   * (compared to ProjectRepresentativeCollection, it returns the User instances rather than the ProjectRepresentative
   * instances - which only contain the ID of the users, not their data)
   *
   * @returns {UserCollection} the collection of project representatives
   */
  async fetchRepresentatives() {
    if(this.isNew()) {
      throw new Error('Cannot fetch representatives of a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/users/representative.json`);
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Fetch the user layers available in the project
   *
   * @param {number} [image]  Identifier of the image: if this parameter is set, the result will also contain user job
   *                          layers available in this image
   *
   * @returns {UserCollection} the collection of users having a layer in the project (and image)
   */
  async fetchUserLayers(image) {
    if(this.isNew()) {
      throw new Error('Cannot fetch user layers of a project with no ID.');
    }

    let params = {};
    if(image) {
      params.image = image;
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/userlayer.json`, {params});
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Add a user to the project
   *
   * @param {number} idUser identifier of the user
   */
  async addUser(idUser) {
    if(this.isNew()) {
      throw new Error('Cannot add a user to a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/user/${idUser}.json`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Add users to the project
   *
   * @param {array} idUsers identifiers of the users
   */
  async addUsers(idUsers) {
    if(this.isNew()) {
      throw new Error('Cannot add a user to a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/user.json?users=${idUsers.join(',')}`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Delete a user from the project
   *
   * @param {number} idUser identifier of the user
   */
  async deleteUser(idUser) {
    if(this.isNew()) {
      throw new Error('Cannot delete a user from a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/user/${idUser}.json`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Delete users from the project
   *
   * @param {array} idUsers identifiers of the users
   */
  async deleteUsers(idUsers) {
    if(this.isNew()) {
      throw new Error('Cannot delete a user from a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/user.json?users=${idUsers.join(',')}`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Add an admin to the project
   *
   * @param {number} idUser identifier of the user
   */
  async addAdmin(idUser) {
    if(this.isNew()) {
      throw new Error('Cannot add an admin to a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/user/${idUser}/admin.json`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Delete an admin from the project
   *
   * @param {number} idUser identifier of the user
   */
  async deleteAdmin(idUser) {
    if(this.isNew()) {
      throw new Error('Cannot delete an admin from a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/user/${idUser}/admin.json`);
    Cytomine.instance.lastCommand = data.command;
  }

  /**
   * Fetch all users of a project with their last activity
   *
   * @returns {UserCollection}    The collection of project users. The following attributes are set for each user:
   *                              id, username, firstname, lastname, email, LDAP, lastImageId, lastImageName,
   *                              lastConnection, frequency
   */
  async fetchUsersActivity() {
    if(this.isNew()) {
      throw new Error('Cannot fetch users activity on a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/usersActivity.json`);
    let collection = new UserCollection();
    data.collection.forEach(item => collection.push(new User(item)));
    return collection;
  }

  /**
   * Fetch the UI config of the project
   * @returns {Object} Set of properties describing which parts of the UI to display, depending on the role of the
   *                   user (each property is an object {CONTRIBUTOR_PROJECT: boolean, ADMIN_PROJECT: boolean})
   */
  async fetchUIConfig() {
    if(this.isNew()) {
      throw new Error('Cannot fetch UI configuration of a project with no ID.');
    }

    let {data} = await axios.get(`${Cytomine.instance._host}/custom-ui/project/${this.id}.json`,
      {withCredentials: true});
    return data;
  }

  /**
   * Sets the UI config of the project
   * @param   {Object} config Set of properties describing which parts of the UI to display, depending on the role of the
   *                          user (each property is an object {CONTRIBUTOR_PROJECT: boolean, ADMIN_PROJECT: boolean})
   * @returns {Object}        Resulting UI config as returned by backend (same structure as input)
   */
  async saveUIConfig(config) {
    if(this.isNew()) {
      throw new Error('Cannot save UI configuration of a project with no ID.');
    }

    let {data} = await axios.post(`${Cytomine.instance.host}/custom-ui/project/${this.id}.json`, config,
      {withCredentials: true});
    return data;
  }

  /**
   * Fetch the last actions in the project
   *
   * @param {number}  [max]       The maximum number of actions to return
   * @param {number}  [offset]    Offset of first record
   * @param {number}  [user]      If specified, only actions from this user will be returned
   * @param {boolean} [fullData]  Specifies whether or not the response should include the full JSON of the data field
   * @param {number}  [startDate] If specified, only actions performed after this date will be returned
   * @param {number}  [endDate]   If specified, only actions performed before this date will be returned
   * @returns {Array<{id, created, message, prefix, prefixAction, user, project, data, serviceName, className, action}>}
   *                              The list of actions (properites data, serviceName, className and action will only be
   *                              provided if fullData is set to true)
   */
  async fetchCommandHistory({max, offset, user, fullData, startDate, endDate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch command history of a project with no ID.');
    }
    return this.constructor.fetchCommandHistory({project: this.id, max, offset, user, fullData, startDate, endDate});
  }

  /**
   * Fetch the last actions performed in the instance
   *
   * @param {number}  [project]   If specified, only actions in this project will be returned
   * @param {number}  [max]       The maximum number of actions to return
   * @param {number}  [offset]    Offset of first record
   * @param {number}  [user]      If specified, only actions from this user will be returned
   * @param {boolean} [fullData]  Specifies whether or not the response should include the full JSON of the data field
   * @param {number}  [startDate] If specified, only actions performed after this date will be returned
   * @param {number}  [endDate]   If specified, only actions performed before this date will be returned
   * @returns {Array<{id, created, message, prefix, prefixAction, user, project, data, serviceName, className, action}>}
   *                              The list of actions (properites data, serviceName, className and action will only be
   *                              provided if fullData is set to true)
   */
  static async fetchCommandHistory({project, max, offset, user, fullData, startDate, endDate}={}) {
    let uri = !project ? 'commandhistory.json' : `project/${project}/commandhistory.json`;
    let {data} = await Cytomine.instance.api.get(uri, {params: {
      max,
      offset,
      user,
      fullData,
      startDate,
      endDate
    }});

    return data;
  }

  /**
   * Fetches the number of connections in this project
   *
   * @param {number}  [startDate] If specified, only connections after this date will be counted
   * @param {number}  [endDate]   If specified, only connections before this date will be counted
   * @returns {number} The number of connections
   */
  async fetchNbConnections({startDate, endDate}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the number of connections in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/userconnection/count.json`,
      {params: {startDate, endDate}}
    );
    return data.total;
  }

  /**
   * Fetches the evolution of connections in this project
   *
   * @param {number}  [startDate]     If specified, only connections after this date will be counted
   * @param {number}  [endDate]       If specified, only connections before this date will be counted
   * @param {number}  [daysRange]     The durations of the periods to consider
   * @param {boolean} [accumulate]    Whether or not the count should be accumulated across periods
   * @returns {Array<{date, endDate, size}>} The number of connections for each period
   */
  async fetchConnectionsEvolution({startDate, endDate, daysRange, accumulate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the evolution of connections in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/stats/connectionsevolution.json`,
      {params: {startDate, endDate, daysRange, accumulate}}
    );
    return data.collection;
  }

  /**
   * Fetches the number of image consultations in this project
   *
   * @param {number}  [startDate] If specified, only consultations after this date will be counted
   * @param {number}  [endDate]   If specified, only consultations before this date will be counted
   * @returns {number} The number of image consultations
   */
  async fetchNbImageConsultations({startDate, endDate}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the number of image consultations in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/imageconsultation/count.json`,
      {params: {startDate, endDate}}
    );
    return data.total;
  }

  /**
   * Fetches the evolution of image consultations in this project
   *
   * @param {number}  [startDate]     If specified, only consultations after this date will be counted
   * @param {number}  [endDate]       If specified, only consultations before this date will be counted
   * @param {number}  [daysRange]     The durations of the periods to consider
   * @param {boolean} [accumulate]    Whether or not the count should be accumulated across periods
   * @returns {Array<{date, endDate, size}>} The number of image consultations for each period
   */
  async fetchImageConsultationsEvolution({startDate, endDate, daysRange, accumulate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the evolution of image consultations in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/stats/imageconsultationsevolution.json`,
      {params: {startDate, endDate, daysRange, accumulate}}
    );
    return data.collection;
  }

  /**
   * Fetches the number of annotation actions in this project
   *
   * @param {number}  [startDate] If specified, only actions after this date will be counted
   * @param {number}  [endDate]   If specified, only actions before this date will be counted
   * @param {string}  [type]    The type of annotation action to take into account
   * @returns {number} The number of annotation actions
   */
  async fetchNbAnnotationActions({startDate, endDate, type}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the number of annotation actions in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/annotation_action/count.json`,
      {params: {startDate, endDate, type}}
    );
    return data.total;
  }

  /**
   * Fetches the evolution of annotation actions in this project
   *
   * @param {number}  [startDate]     If specified, only actions after this date will be counted
   * @param {number}  [endDate]       If specified, only actions before this date will be counted
   * @param {number}  [daysRange]     The durations of the periods to consider
   * @param {boolean} [accumulate]    Whether or not the count should be accumulated across periods
   * @param {string}  [type]        The type of annotation action to take into account
   * @returns {Array<{date, endDate, size}>} The number of annotation actions for each period
   */
  async fetchAnnotationActionsEvolution({startDate, endDate, daysRange, accumulate, type}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the evolution of annotation actions in a project with no ID.');
    }
    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/stats/annotationactionsevolution.json`,
      {params: {startDate, endDate, daysRange, accumulate, type}}
    );
    return data.collection;
  }

  /**
   * Fetches the number of annotations in this project
   *
   * @param {number}  [startDate] If specified, only annotations created after this date will be counted
   * @param {number}  [endDate]   If specified, only annotations created before this date will be counted
   * @param {string} annotationType The annotation type to count (see AnnotationType for allowed values)
   * @returns {number} The number of annotations
   */
  async fetchNbAnnotations({startDate, endDate, annotationType}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the number of annotations in a project with no ID.');
    }

    let uri = null;
    switch(annotationType) {
    case AnnotationType.USER:
      uri = 'userannotation';
      break;
    case AnnotationType.ALGO:
      uri = 'algoannotation';
      break;
    case AnnotationType.REVIEWED:
      uri = 'reviewedannotation';
      break;
    }

    if(!uri) {
      throw new Error('This annotation type is not handled.');
    }

    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/${uri}/count.json`,
      {params: {startDate, endDate}}
    );
    return data.total;
  }

  /** Fetches the evolution of annotations in this project
   *
   * @param {number}  [startDate]     If specified, only annotations created after this date will be counted
   * @param {number}  [endDate]       If specified, only annotations created before this date will be counted
   * @param {number}  [daysRange]     The durations of the periods to consider
   * @param {boolean} [accumulate]    Whether or not the count should be accumulated across periods
   * @param {boolean} [reverseOrder]  If true, the latest period will be returned as first element of the array
   * @param {number}  [term]          The identifier of the term to consider
   * @param {string}  annotationType The annotation type to count (see AnnotationType for allowed values)
   * @returns {Array<{date, endDate, size}>} The number of annotations for each period
   */
  async fetchAnnotationsEvolution({startDate, endDate, annotationType, daysRange, accumulate, reverseOrder, term}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch the evolution of annotations in a project with no ID.');
    }

    let uri = null;
    switch(annotationType) {
    case AnnotationType.USER:
      uri = 'annotationevolution';
      break;
    case AnnotationType.ALGO:
      uri = 'algoannotationevolution';
      break;
    case AnnotationType.REVIEWED:
      uri = 'reviewedannotationevolution';
      break;
    }

    if(!uri) {
      throw new Error('This annotation type is not handled.');
    }

    let {data} = await Cytomine.instance.api.get(
      `project/${this.id}/stats/${uri}.json`,
      {params: {startDate, endDate, daysRange, accumulate, reverseOrder, term}}
    );
    return data.collection;
  }

  /**
   * Fetches the number of annotations for each term
   *
   * @param {number}  [startDate]     If specified, only associations after this date will be counted
   * @param {number}  [endDate]       If specified, only associations before this date will be counted
   * @param {boolean}  [leafsOnly]    If true, only leafs terms will be returned in the statistics
   * @returns {Array<{id, key, color, value}>} The terms, with their associated count (value property)
   */
  async fetchStatsTerms({startDate, endDate, leafsOnly}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch terms statistics in a project with no ID.');
    }

    let params = {startDate, endDate, leafsOnly};
    let {data} = await Cytomine.instance.api.get(`project/${this.id}/stats/term.json`, {params});
    return data.collection;
  }

  /**
   * Fetches for each term the number of images having annotations associated with this term
   *
   * @param {number}  [startDate]     If specified, only associations after this date will be counted
   * @param {number}  [endDate]       If specified, only associations before this date will be counted
   * @returns {Array<{id, key, color, value}>} The terms, with their associated count (value property)
   */
  async fetchStatsAnnotatedImagesByTerm({startDate, endDate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch annotated images statistics in a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`project/${this.id}/stats/termslide.json`, {startDate, endDate});
    return data.collection;
  }

  /**
   * Fetches the number of annotations for each contributor
   *
   * @param {number}  [startDate]     If specified, only annotations created after this date will be counted
   * @param {number}  [endDate]       If specified, only annotations created before this date will be counted
   * @returns {Array<{id, key, username, value}>} The contributors (key=firstName + lastName), with their associated count (value property)
   */
  async fetchStatsAnnotationCreators({startDate, endDate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch annotation creators statistics in a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`project/${this.id}/stats/user.json`, {startDate, endDate});
    return data.collection;
  }

  /**
   * Fetches the number of annotated images for each contributor
   *
   * @param {number}  [startDate]     If specified, only annotations created after this date will be counted
   * @param {number}  [endDate]       If specified, only annotations created before this date will be counted
   * @returns {Array<{id, key, value}>} The contributors (key=firstName + lastName), with their associated count (value property)
   */
  async fetchStatsAnnotatedImagesByCreator({startDate, endDate}={}) {
    if(this.isNew()) {
      throw new Error('Cannot fetch annotated images statistics in a project with no ID.');
    }

    let {data} = await Cytomine.instance.api.get(`project/${this.id}/stats/userslide.json`, {startDate, endDate});
    return data.collection;
  }
}

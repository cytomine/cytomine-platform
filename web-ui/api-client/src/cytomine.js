import axios from 'axios';

export default class Cytomine {

  /**
   * @param {string}   host             The Cytomine host
   * @param {string} [basePath=/api/]   The base path to perform API requests
   *
   * @returns {this} The singleton instance
   */
  constructor(host, basePath = '/api/') {
    if(!Cytomine._instance) {
      if(!host.startsWith('http://') && !host.startsWith('https://')) {
        host = 'http://' + host;
      }
      if(host.endsWith('/')) {
        host = host.slice(0, -1);
      }

      if(!basePath.startsWith('/')) {
        basePath = '/' + basePath;
      }

      if(!basePath.endsWith('/')) {
        basePath = basePath + '/';
      }

      this._host = host;
      this._basePath = basePath;

      this.api = axios.create({
        baseURL: this._host + this._basePath,
        withCredentials: true
      });

      this.api.interceptors.response.use(function (response) {
        return response;
      }, function (error) {
        error.message += ' - Response data: ' + JSON.stringify(error.response.data);
        return Promise.reject(error);
      });

      this.lastCommand = null;

      Cytomine._instance = this;
    }

    return Cytomine._instance;
  }

  /**
   * @returns {this} The singleton instance
   */
  static get instance() {
    if(!Cytomine._instance) {
      throw new Error('No Cytomine instance was created.');
    }
    return Cytomine._instance;
  }

  /**
   * @returns {string} The host
   */
  get host() {
    return this._host;
  }

  /**
   * @returns {string} The base path
   */
  get basePath() {
    return this._basePath;
  }

  /**
   * Ping the server to get info
   *
   * @param {number}  [project]   The identifier of the active project
   * @returns {{alive, authenticated, version, serverURL, serverID, user}} The data returned by the server
   */
  async ping(project) {
    let {data} = await axios.post(`${this._host}/server/ping.json`, {project}, {withCredentials: true});
    return data;
  }

  /**
   * Login to Cytomine with the provided credentials
   *
   * @param {string}  username            The username
   * @param {string}  password            The password
   * @param {boolean} [rememberMe=true]   Whether or not to remember the user
   */
  async login(username, password, rememberMe=true) {
    let params = new URLSearchParams();
    params.append('j_username', username);
    params.append('j_password', password);
    params.append('remember_me', rememberMe ? 'on' : 'off');
    params.append('ajax', true);
    await axios.post(`${this._host}/j_spring_security_check`, params, {withCredentials: true});
  }

  /**
   * Login to Cytomine with a token
   *
   * @param {string}  username    The username
   * @param {string}  tokenKey    The token
   */
  async loginWithToken(username, tokenKey) {
    let params = {username, tokenKey};
    await axios.get(`${this._host}/login/loginWithToken`, {withCredentials: true, params});
  }

  /**
   * Impersonate another user
   *
   * @param {String} username The username of the user to impersonate
   */
  async switchUser(username) {
    let params = new URLSearchParams();
    params.append('j_username', username);
    params.append('ajax', true);
    await axios.post(`${this._host}/j_spring_security_switch_user`, params, {withCredentials: true});
  }

  /**
   * Stops impersonating another user
   */
  async stopSwitchUser() {
    await axios.get(`${this._host}/j_spring_security_exit_user`, {withCredentials: true});
  }

  /**
   * Logout from Cytomine
   */
  async logout() {
    await axios.get(`${this._host}/logout`, {withCredentials: true});
  }

  /**
   * Send a mail to the provided email address with the associated username
   *
   * @param {string}  email   The email address
   */
  async forgotUsername(email) {
    let params = new URLSearchParams();
    params.append('j_email', email);
    await axios.post(`${this._host}/login/forgotUsername`, params);
  }

  /**
   * Send a mail to the email associated to the provided username to reset password
   *
   * @param {string}  username    The username
   */
  async forgotPassword(username) {
    let params = new URLSearchParams();
    params.append('j_username', username);
    await axios.post(`${this._host}/login/forgotPassword`, params);
  }

  /**
   * Open an admin session
   * @returns {boolean} True if the current user is now connected as admin
   */
  async openAdminSession() {
    let {data} = await axios.get(`${this._host}/session/admin/open.json`, {withCredentials: true});
    return data.adminByNow;
  }

  /**
   * Close an admin session
   * @returns {boolean} True if the current user is no longer connected as admin
   */
  async closeAdminSession() {
    let {data} = await axios.get(`${this._host}/session/admin/close.json`, {withCredentials: true});
    return !data.adminByNow;
  }

  /**
   * Fetch the UI configuration for the current user
   * @param {number} [project] The identifier of the project to consider (if specified, in addition to the general UI
   *                           config, the UI config of the specified project will be returned)
   * @returns {Object} Set of properties describing which parts of the UI to display
   */
  async fetchUIConfigCurrentUser(project) {
    let params = {};
    if(project) {
      params.project = project;
    }

    let {data} = await axios.get(`${this._host}/custom-ui/config.json`, {params, withCredentials: true});
    return data;
  }

  /**
   * Fetch a signature string for the current user
   * @param {string} method The request method action
   * @param {string} uri The request URI
   * @param {string} [queryString] The request query string
   * @param {string} [date] The request date
   * @param {string} [contentMD5] The request content MD5
   * @param {string} [contentType] The request content type
   * @returns {string} The generated signature
   */
  async fetchSignature({method, uri, queryString, date, contentMD5, contentType}={}) {
    let params = {method, forwardURI: uri, queryString, date, 'content-MD5': contentMD5, 'content-type': contentType};

    let {data} = await this.api.get('signature.json', {params});
    return data.signature;
  }

  /**
   * Fetch total count of each model
   *
   * @returns {{users, projects, images, userAnnotations, jobAnnotations, terms, ontologies, softwares, jobs}}
   *          The total count for each model
   */
  async fetchTotalCounts() {
    let {data} = await this.api.get('stats/all.json');
    return data;
  }

  /**
   * Fetch stats of current activity
   *
   * @returns {{users, projects, mostActiveProject}} Stats related to current activity
   */
  async fetchCurrentStats() {
    let {data} = await this.api.get('stats/currentStats.json');
    return data;
  }

  /**
   * Fetch stats related to storage space
   *
   * @returns {{total, available, used, usedP}} Stats related to the storage
   */
  async fetchStorageStats() {
    let {data} = await this.api.get('stats/imageserver/total.json');
    return data;
  }

  /**
   * Undo a command
   *
   * @param {number} command The identifier of the command to undo ; if null, the last command will be undone
   *
   * @returns {Array<Object>} The collection of affected models
   */
  async undo(command = null) {
    let {data} = await this.api.get(`command/${command ? command + '/' : ''}undo.json`);
    return data.collection;
  }

  /**
   * Redo a command
   *
   * @param {number} command The identifier of the command to redo ; if null, the last undone command will be redone
   *
   * @returns {Array<Object>} The collection of affected models
   */
  async redo(command = null) {
    let {data} = await this.api.get(`command/${command ? command + '/' : ''}redo.json`);
    return data.collection;
  }
}

import axios from "axios";

export default class Cytomine {

    /**
     * @param {string}   host             The Cytomine host
     * @param {string} [basePath=/api/]   The base path to perform API requests
     *
     * @returns {this} The singleton instance
     */
    constructor(host, basePath = "/api/") {
        if(!Cytomine._instance) {
            if(!host.startsWith("http://") && !host.startsWith("https://")) {
                host = "http://" + host;
            }
            if(host.endsWith("/")) {
                host = host.slice(0, -1);
            }

            if(!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }

            if(!basePath.endsWith("/")) {
                basePath = basePath + "/";
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
                error.message += " - Response data: " + JSON.stringify(error.response.data);
                return Promise.reject(error);
            });

            Cytomine._instance = this;
        }

        return Cytomine._instance;
    }

    /**
     * @returns {this} The singleton instance
     */
    static get instance() {
        if(!Cytomine._instance) {
            throw new Error("No Cytomine instance was created.");
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
     * @returns {{alive, authenticated, version, serverURL, serverID, user}} The data returned by the server
     */
    async ping() {
        let {data} = await axios.get(`${this._host}/server/ping.json`, {withCredentials: true});
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
        params.append("j_username", username);
        params.append("j_password", password);
        params.append("remember_me", rememberMe ? "on" : "off");
        await axios.post(`${this._host}/j_spring_security_check`, params, {withCredentials: true});
    }

    /**
     * Logout from Cytomine
     */
    async logout() {
        await axios.get(`${this._host}/logout`, {withCredentials: true});
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
        if(project != null) {
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
        let params = {method, forwardURI: uri, queryString, date, "content-MD5": contentMD5, "content-type": contentType};

        let {data} = await this.api.get("signature.json", {params});
        return data.signature;
    }
}

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
     * @param {string}  [email=""]          The email of the user // QUESTION: is it needed?
     * @param {boolean} [rememberMe=true]   Whether or not to remember the user
     */
    async login(username, password, email="", rememberMe=true) {
        let params = new URLSearchParams();
        params.append("j_username", username);
        params.append("j_password", password);
        params.append("j_email", email);
        params.append("remember_me", rememberMe ? "on" : "off");
        await axios.post(`${this._host}/j_spring_security_check`, params, {withCredentials: true});
    }

    /**
     * Logout from Cytomine
     */
    async logout() {
        await axios.put(`${this._host}/j_spring_security_logout`);
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
}

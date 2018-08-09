import Cytomine from "../cytomine.js";
import Model from "./model.js";
import User from "./user.js";
import UserCollection from "../collections/user-collection.js";

export default class Project extends Model {
    /** @inheritdoc */
    static get callbackIdentifier() {
        return "project";
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
     * @static Record a connection of the current user to a project
     *
     * @param {number} id The identifier of the project
     */
    static async recordUserConnection(id) {
        new this({id}).recordUserConnection();
    }

    /**
     * Record a connection of the current user to the project
     */
    async recordUserConnection() {
        if(this.isNew()) {
            throw new Error("Cannot record user connection to project with no ID.");
        }

        const { detect } = require("detect-browser");
        const browser = detect();

        await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/userconnection.json`, {
            browser: browser ? browser.name : "Unknown",
            browserVersion: browser ? browser.verion : "Unknown",
            os: browser ? browser.os : "Unknown",
            project: this.id
        });
    }

    /**
     * Fetch the user that created the project
     *
     * @returns {User}
     */
    async fetchCreator() {
        if(this.isNew()) {
            throw new Error("Cannot get creator of a project with no ID.");
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
            throw new Error("Cannot fetch users of a project with no ID.");
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
            throw new Error("Cannot fetch connected users of a project with no ID.");
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
            throw new Error("Cannot fetch administrators of a project with no ID.");
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
            throw new Error("Cannot fetch representatives of a project with no ID.");
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
            throw new Error("Cannot fetch user layers of a project with no ID.");
        }

        let params = {};
        if(image != null) {
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
            throw new Error("Cannot add a user to a project with no ID.");
        }
        await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/user/${idUser}.json`);
    }

    /**
     * Delete a user from the project
     *
     * @param {number} idUser identifier of the user
     */
    async deleteUser(idUser) {
        if(this.isNew()) {
            throw new Error("Cannot delete a user from a project with no ID.");
        }
        await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/user/${idUser}.json`);
    }

    /**
     * Add an admin to the project
     *
     * @param {number} idUser identifier of the user
     */
    async addAdmin(idUser) {
        if(this.isNew()) {
            throw new Error("Cannot add an admin to a project with no ID.");
        }
        await Cytomine.instance.api.post(`${this.callbackIdentifier}/${this.id}/user/${idUser}/admin.json`);
    }

    /**
     * Delete an admin from the project
     *
     * @param {number} idUser identifier of the user
     */
    async deleteAdmin(idUser) {
        if(this.isNew()) {
            throw new Error("Cannot delete an admin from a project with no ID.");
        }
        await Cytomine.instance.api.delete(`${this.callbackIdentifier}/${this.id}/user/${idUser}/admin.json`);
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
            throw new Error("Cannot fetch users activity on a project with no ID.");
        }

        let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/usersActivity.json`);
        let collection = new UserCollection();
        data.collection.forEach(item => collection.push(new User(item)));
        return collection;
    }
}

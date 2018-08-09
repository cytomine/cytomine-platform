import Cytomine from "../cytomine.js";
import Model from "./model.js";

export default class UserPosition extends Model {
    /** @inheritdoc */
    static get callbackIdentifier() {
        return "userposition";
    }

    /** @inheritdoc */
    _initProperties() {
        super._initProperties();

        this.user = null;
        this.image = null;
        this.project = null;

        this.location = null;
        this.x = null;
        this.y = null;
        this.zoom = null;
    }

    /** @override */
    static async fetch() {
        throw new Error("UserPosition instances cannot be fetched.");
    }

    /** @override */
    async fetch() {
        throw new Error("UserPosition instances cannot be fetched.");
    }

    static async fetchLastPosition(imageInstance, user) {
        let {data} = await Cytomine.instance.api.get(`imageinstance/${imageInstance}/position/${user}.json`);
        return new this(data);
    }

    /** @override */
    async save() {
        throw new Error("A UserPosition instance cannot be saved. Use the static method create instead.");
    }

    /**
     * @static Record the position of the current user on an image
     *
     * @param {{image, topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, zoom}} position
     *
     * @returns {this} The created position
     */
    static async create(position) {
        if(position == null || position.image == null) {
            throw new Error("The position parameter should have an image property.");
        }
        let image = position.image;
        let {data} = await Cytomine.instance.api.post(`imageinstance/${image}/position.json`, position);
        return new this(data);
    }

    /** @override */
    async update() {
        throw new Error("A UserPosition instance cannot be updated.");
    }

    /** @override */
    async delete() {
        throw new Error("A UserPosition instance cannot be deleted.");
    }
}

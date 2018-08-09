import Collection from "./collection.js";
import UserPosition from "../models/user-position.js";

export default class UserPositionCollection extends Collection {

    constructor(props, nbPerPage, filterKey, filterValue) {
        super(nbPerPage, filterKey, filterValue);
        this.user = null;
        this.beforeThan = null;
        this.afterThan = null;
        this.showDetails = true; /** if false, only location, image and zoom properties will be set for the objects of
        * the collection. An additional "frequency" property will also be available. Moreover, location will be returned
        * as an array of coordinates instead of the usual WKT string. */
        this.setProps(props);
    }

    /**
     * @override
     * @static Fetch all available items
     *
     * @param {Object} [props]  Object containing the properties of the collection to set
     * @param {number} [nbPerPage] The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */
    static async fetch(props, nbPerPage) {
        return new this(props).fetch(nbPerPage);
    }

    /**
     * @static Fetch all available items fitting provided filter
     *
     * @param {string} key      The filter key
     * @param {number} value    The filter value (an identifier)
     * @param {Object} [props]  Object containing the properties of the collection to set
     * @param {number} [nbPerPage] The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */
    static async fetchWithFilter(key, value, props, nbPerPage) {
        return new this(props, nbPerPage, key, value).fetch();
    }

    /** @inheritdoc */
    static get model() {
        return UserPosition;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return ["imageinstance"];
    }

    // HACK: remove (temporary hack due to lack of consistency in API endpoint)
    /** @inheritdoc */
    get callbackIdentifier() {
        return "positions";
    }
}

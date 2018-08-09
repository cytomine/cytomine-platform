import Collection from "./collection.js";
import Job from "../models/job.js";

export default class JobCollection extends Collection {

    constructor(props, nbPerPage, filterKey, filterValue) {
        super(nbPerPage, filterKey, filterValue);

        this.software = null;
        this.project = null;
        this.light = null;

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
        let collection = new this(props);
        return collection.fetch(nbPerPage);
    }

    /** @inheritdoc */
    static get model() {
        return Job;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null];
    }
}

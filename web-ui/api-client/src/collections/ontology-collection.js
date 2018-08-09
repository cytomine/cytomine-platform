import Collection from "./collection.js";
import Ontology from "../models/ontology.js";

export default class OntologyCollection extends Collection {

    constructor(props, nbPerPage, filterKey, filterValue) {
        super(nbPerPage, filterKey, filterValue);
        this.light = null; // if true, only id and name properties will be filled
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
        return Ontology;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null];
    }
}

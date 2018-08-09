import Model from "../models/model.js";
import Collection from "./collection.js";

export default class DomainCollection extends Collection {

    /**
     * Create a collection containing metadata objects characterizing an existing model instance
     *
     * @param {Model} object            The reference object
     * @param {number}  [nbPerPage=0]   The maximum number of items fetched at a time (if set to 0, all items will be fetched at once)
     * @param {string}  [filterKey]       The filter key
     * @param {number}  [filterValue]     The filter value (an identifier)
     */
    constructor(object, nbPerPage=0, filterKey, filterValue) {
        if (new.target === DomainCollection) {
            throw new Error("DomainCollection is an abstract class and cannot be constructed directly.");
        }
        super(nbPerPage, filterKey, filterValue);

        if(object == null) {
            throw new Error("The reference object must be defined.");
        }

        this.object = object;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null];
    }

    /**
     * @static Fetch all available items
     *
     * @param {Model} object         The reference object
     * @param {number} [nbPerPage]   The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */
    static async fetch(object, nbPerPage) {
        let collection = new this(object);
        return collection.fetch(nbPerPage);
    }

    /**
     * Set the reference object
     *
     * @param {Model} obj The reference object
     */
    set object(obj) {
        if(!(obj instanceof Model)) {
            throw new Error("The object must be a model instance.");
        }

        if(obj.isNew() || !obj.class) {
            throw new Error("The object must be fetched or saved.");
        }

        this._object = obj;
    }

    /**
     * The class name of the reference object
     * @type {string}
     */
    get domainClassName() {
        return this._object.class;
    }

    /**
     * The identifier of the reference object
     * @type {number}
     */
    get domainIdent() {
        return this._object.id;
    }

    /** @inheritdoc */
    get uri() {
        return `domain/${this.domainClassName}/${this.domainIdent}/${this.callbackIdentifier}.json`;
    }

}

import Cytomine from "../cytomine.js";

export default class Collection {

    /**
     * @param {number}  [nbPerPage=0] The maximum number of items fetched at a time (if set to 0, all items will be fetched at once)
     * @param {string}  [filterKey]    The filter key
     * @param {number}  [filterValue]  The filter value (an identifier)
     */
    constructor(nbPerPage=0, filterKey, filterValue) {
        if (new.target === Collection) {
            throw new Error("Collection is an abstract class and cannot be constructed directly.");
        }

        this._data = [];
        this._filter = {};
        if(filterKey != null && filterValue != null) {
            this.setFilter(filterKey, filterValue);
        }

        this.max = nbPerPage;
        this.offset = 0;

        this._total = null;
        this._nbPages = null;
        this._curPage = 0;
    }

    toString() {
        return `[${this.callbackIdentifier} collection] ${this.length} objects`;
    }

    [Symbol.iterator]() {
        return this._data.values();
    }

    get array() {
        return this._data;
    }

    forEach(callback, thisArg) {
        this._data.forEach(callback, thisArg);
    }

    /**
     * Populate the instance with the properties of the provided object (useful for collections accepting custom
     * parameters in addition to max and offset)
     *
     * @param {Object} props Object containing the properties to set
     */
    setProps(props) {
        for(let key in props) {
            if(!key.startsWith("_")) {
                this[key] = props[key];
            }
        }
    }

    /**
     * Set the filter
     *
     * @param {string} key   The filter key
     * @param {number} value The filter value (an identifier)
     */
    setFilter(key, value) {
        if(!this.constructor.allowedFilters.includes(key)) {
            throw new Error(`${key} filter not allowed for a ${this.callbackIdentifier} collection.`);
        }
        this._filter.key = key;
        this._filter.value = value;
    }

    /** @type {number} */
    get curPage() {
        return this._curPage;
    }

    set curPage(value) {
        this._curPage = value;
    }

    async _fetch(append=false) {

        if(this._filter.key == null && !this.constructor.allowedFilters.includes(null)) {
            throw new Error(`A ${this.callbackIdentifier} collection cannot be fetched without filter.`);
        }

        let {data} = await Cytomine.instance.api.get(this.uri, {params: this.getParameters()});

        if(!append) {
            this._data = [];
        }

        this._total = data.size;
        this._nbPages = data.totalPages;

        let collection = data.collection;
        collection.forEach(elem => {
            let model = new this.constructor.model(elem);
            this.push(model);
        });

        return this;
    }

    getParameters() {
        let params = {};
        for(let key in this) {
            let value = this[key];
            if(!key.startsWith("_") && value != null) {
                if(Array.isArray(value)) {
                    value = value.join();
                }
                params[key] = value;
            }
        }
        return params;
    }

    /**
     * @static Fetch all available items
     *
     * @param {number} [nbPerPage] The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */
    static async fetch(nbPerPage) {
        return new this(nbPerPage).fetch();
    }

    /**
     * Fetch all available items and fill the collection with them
     *
     * @returns {this} collection containing all available items
     */
    async fetch() {
        if(this.max > 0) {
            this.offset = 0;
            await this.fetchPage();
            while(this.length < this._total) {
                await this.fetchNextPage(true);
            }
            return this;
        }
        else {
            return this._fetch();
        }
    }

    /**
     * @static Set a filter and fetch all available items (shorthand for setFilter() followed by fetch())
     *
     * @param {number} [nbPerPage] The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */

    /**
     * @static Fetch all available items fitting provided filter
     *
     * @param {string} key      The filter key
     * @param {number} value    The filter value (an identifier)
     * @param {number} [nbPerPage] The maximum number of items to fetch per request
     *
     * @returns {this} collection containing all available items
     */
    static async fetchWithFilter(key, value, nbPerPage) {
        return new this(nbPerPage, key, value).fetch();
    }

    /**
     * Fetch a page of the items
     *
     * @param {number}  [numPage=0]     The number of the page to fetch (first page has number 0)
     * @param {boolean} [append=false]  Whether the fetched data should be appended to the existing collection, or should
     *                                  replace the existing content
     *
     * @returns {this} collection containing the fetched items
     */
    async fetchPage(numPage=0, append=false) {
        if(numPage != null) {
            this._curPage = numPage;
        }

        let oobError = new Error("The page you are trying to fetch is out of bounds.");
        // if the nb of pages is not known yet, the verification on the upper bound will be performed after the fetch
        let postVerif = (this._nbPages == null);

        if(this._curPage < 0 || (!postVerif && this._curPage >= this._nbPages && this._curPage > 0)) {
            throw oobError;
        }

        this.offset = this._curPage*this.max;
        await this._fetch(append);

        if(postVerif && this._curPage >= this._nbPages && this._curPage > 0) {
            throw oobError;
        }

        return this;
    }

    /**
     * Fetch the next page of items
     *
     * @param {boolean} [append=false]  Whether the fetched data should be appended to the existing collection, or should
     *                                  replace the existing content
     *
     * @returns {this} collection containing the fetched items
     */
    async fetchNextPage(append=false) {
        return this.fetchPage(this._curPage + 1, append);
    }

    /**
     * Fetch the previous page of items
     *
     * @param {boolean} [append=false]  Whether the fetched data should be appended to the existing collection, or should
     *                                  replace the existing content
     *
     * @returns {this} collection containing the fetched items
     */
    async fetchPreviousPage(append=false) {
        return this.fetchPage(this._curPage - 1, append);
    }

    /**
     * Add an item to the collection (this item is not automatically persisted in the database)
     *
     * @param {Model} model The item to add
     */
    push(model) {
        if(!(model instanceof this.constructor.model)) {
            throw new Error(`A ${this.callbackIdentifier} collection can only contain ${this.callbackIdentifier} instances.`);
        }
        this._data.push(model);
    }

    /**
     * Get the item of the collection at the provided index
     *
     * @param {number} idx The index of the item to get
     *
     * @returns {Model} The item
     */
    get(idx) {
        return this._data[idx];
    }

    /**
     * Persist the items of the collection in the database
     */
    async save() {
        if(!this.constructor.isSaveAllowed) {
            throw new Error(`A ${this.callbackIdentifier} collection cannot be saved through the API.`);
        }
        await Cytomine.instance.api.post(this.uriWithoutFilter, this._data);
        // Remark: not possible to return the collection of saved objects (core only returns a success message)
    }

    /**
     * @returns {number} The number of items contained in the collection
     */
    get length() {
        return this._data.length;
    }

    /**
     * @returns {string} The callback identifier of the collection used in API requests
     */
    get callbackIdentifier() {
        return this.constructor.model.callbackIdentifier;
    }

    /**
     * @returns {string} API URI to use to perform operations on the collection (including filter path parameters if a
     * filter was set)
     */
    get uri() {
        if(this._filter.key != null && this._filter.value != null) {
            return `${this._filter.key}/${this._filter.value}/${this.uriWithoutFilter}`;
        }
        else {
            return this.uriWithoutFilter;
        }
    }

    /**
     * @returns {string} API URI to use to perform operations on the collection, without filter path parameters
     */
    get uriWithoutFilter() {
        return `${this.callbackIdentifier}.json`;
    }

    /**
     * @static The class of the objects that are allowed in the collection.
     * @abstract
     *
     * @returns {class}
     */
    static get model() {
        throw new Error("Abstract method not overriden in child.");
    }

    /**
     * @abstract
     * @static Defines the list of filters allowed for the collection. If the collection can be fetched without filter,
     * the list must include null.
     *
     *
     * @returns {Array<String>} The allowed filters
     */
    static get allowedFilters() {
        throw new Error("Abstract method not overriden in child.");
    }

    /**
     * @static Defines whether or not the API allows to save the collection. It should be overridden in children
     * collections for which it is allowed
     */
    static get isSaveAllowed() {
        return false;
    }
}

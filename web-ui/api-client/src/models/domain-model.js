import Model from "./model.js";

export default class DomainModel extends Model {

    /**
     * Create a metadata object characterizing an existing model instance
     *
     * @param {Object} [props]  Object containing the properties of the object to set
     * @param {Model} object    The reference object
     */
    constructor(props, object) {
        if (new.target === DomainModel) {
            throw new Error("DomainModel is an abstract class and cannot be constructed directly.");
        }

        super(props);
        if(object != null) {
            this.object = object;
        }

        if(this.domainClassName == null || this.domainIdent == null) {
            throw new Error("The reference object must be defined.");
        }
    }

    /** @inheritdoc */
    _initProperties() {
        super._initProperties();
        this._object = null;
        this.domainClassName = null; // preferably, set by changing object property
        this.domainIdent = null; // preferably, set by changing object property
    }

    /**
     * @override
     * @static Fetch a metadata object
     *
     * @param {number} id       The identifier of the metadata to fetch
     * @param {Model} object    The reference object
     *
     * @returns {this} The fetched object
     */
    static async fetch(id, object) {
        return new this({id}, object).fetch();
    }

    /**
     * @override
     * @static Delete a metadata object
     *
     * @param {number} id       The identifier of the metadata to delete
     * @param {Model} object    The reference object
     */
    static async delete(id, object) {
        return new this({id}, object).delete();
    }

    /** @type {Model} */
    get object() {
        return this._object;
    }

    set object(obj) {
        if(!(obj instanceof Model)) {
            throw new Error("PLOUF The object must be a model instance.");
        }

        if(obj.isNew() || !obj.class) {
            throw new Error("The object must be fetched or saved.");
        }

        this._object = obj;
        this.domainIdent = obj.id;
        this.domainClassName = obj.class;
    }

    /** @inheritdoc */
    get uri() {
        if(this.isNew()) {
            return `domain/${this.domainClassName}/${this.domainIdent}/${this.callbackIdentifier}.json`;
        }
        else {
            return `domain/${this.domainClassName}/${this.domainIdent}/${this.callbackIdentifier}/${this.id}.json`;
        }
    }

}

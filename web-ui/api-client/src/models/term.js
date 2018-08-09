import Model from "./model.js";

export default class Term extends Model {
    /** @inheritdoc */
    static get callbackIdentifier() {
        return "term";
    }

    /** @inheritdoc */
    _initProperties() {
        super._initProperties();

        this.name = null;
        this.comment = null;
        this.ontology = null;
        // this.rate = null;
        // this.parent = null;
        this.color = null;
    }
}

import Collection from "./collection.js";
import Annotation from "../models/annotation.js";

export default class AnnotationCollection extends Collection {

    constructor(props, nbPerPage, filterKey, filterValue) {
        super(nbPerPage, filterKey, filterValue);

        this.showDefault = true;
        this.showBasic = null;
        this.showMeta = null;
        this.showWKT = null;
        this.showGIS = null;
        this.showTerm = null;
        this.showAlgo = null;
        this.showUser = null;
        this.showImage = null;

        this.reviewed = null;
        this.notReviewedOnly = null;

        this.project = null;
        this.image = null;
        this.images = null;

        this.job = null;
        this.user = null;
        this.users = null;
        this.includeAlgo = null;

        this.kmeans = null;

        this.term = null;
        this.terms = null;
        this.suggestedTerm = null;
        this.userForTermAlgo = null;
        this.jobForTermAlgo = null;
        this.noTerm = null;
        this.noAlgoTerm = null;
        this.multipleTerm = null;

        this.bbox = null;
        this.bboxAnnotation = null;
        this.baseAnnotation = null;
        this.maxDistanceBaseAnnotation = null;

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

    // QUESTION: add "included"? (discuss whether it is needed or not with Renaud)

    /** @inheritdoc */
    static get model() {
        return Annotation;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null];
    }

    /** @inheritdoc */
    static get isSaveAllowed() {
        return true;
    }
}

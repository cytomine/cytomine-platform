import Collection from "./collection.js";
import Annotation from "../models/annotation.js";

export default class AnnotationCollection extends Collection {

    /** @inheritdoc */
    _initProperties() {
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
    }

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

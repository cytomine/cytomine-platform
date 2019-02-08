import Cytomine from "../cytomine.js";
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

    /**
     * Returns the URL for downloading the collection under the provided format
     * @param   {String} [format="pdf"] The format of the file to download ("csv", "xls" or "pdf")
     * @returns {String} The download URL
     */
    getDownloadURL(format="pdf") {
        if(this.project == null) {
            throw new Error("Cannot construct download URL if no project ID is provided.");
        }
        let strParam = `format=${format}`;
        let paramFields = ["reviewed", "terms", "users", "images", "noTerm", "multipleTerms"];
        paramFields.forEach(param => {
            if(this[param] != null) {
                strParam += `&${param}=${this[param]}`;
            }
        });
        let uri = `project/${this.project}/annotation/download?${strParam}`;
        return Cytomine.instance.host + Cytomine.instance.basePath + uri;
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

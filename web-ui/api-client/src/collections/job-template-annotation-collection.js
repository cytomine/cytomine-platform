import Collection from "./collection.js";
import JobTemplateAnnotation from "../models/job-template-annotation.js";

export default class JobTemplateAnnotationCollection extends Collection {

    constructor(props, nbPerPage, filterKey, filterValue) {
        super(nbPerPage, filterKey, filterValue);

        this.jobtemplate = null;
        this.annotation = null;

        this.setProps(props);
    }

    static async fetch(props, nbPerPage) {
        let collection = new this(props);
        return collection.fetch(nbPerPage);
    }

    /** @inheritdoc */
    static get model() {
        return JobTemplateAnnotation;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null];
    }
}

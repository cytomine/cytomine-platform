import Cytomine from "../cytomine.js";
import Collection from "./collection.js";
import Project from "../models/project.js";

export default class ProjectCollection extends Collection {

    /** @inheritdoc */
    _initProperties() {
        this.light = null; // used iff filter on user ; if true, only id and name properties will be filled
        this.admin = null; // used iff filter on user ; if true, only returns the projects in which user is admin
        this.withLastActivity = null;
        this.withMembersCount = null;
    }

    /** @inheritdoc */
    static get model() {
        return Project;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return [null, "user", "software", "ontology"];
    }

    /**
     * @static Fetch last opened projects
     *
     * @param {number} [max=0]    The maximum number of items to retrieve
     * @param {number} [offset=0] The offset
     *
     * @returns {Array<{id: Number, date: String, opened: Boolean}>} The last opened projects
     */
    static async fetchLastOpened(max=0, offset=0) {
        let {data} = await Cytomine.instance.api.get(`project/method/lastopened.json?max=${max}&offset=${offset}`);
        return data.collection;
    }

    // HACK: remove (temporary hack due to lack of consistency in API endpoint)
    /** @inheritdoc */
    get uri() {
        if(this._filter.key == "user" && this._filter.value != null && this.light) {
            return `user/${this._filter.value}/project/light.json`;
        }
        else {
            return super.uri;
        }
    }
}

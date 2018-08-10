import Cytomine from "../cytomine.js";
import Model from "./model.js";
import User from "./user.js";
import ProjectCollection from "../collections/project-collection.js";
import TermCollection from "../collections/term-collection.js";

export default class Ontology extends Model {
    /** @inheritdoc */
    static get callbackIdentifier() {
        return "ontology";
    }

    /** @inheritdoc */
    _initProperties() {
        super._initProperties();

        this.name = null;
        this.user = null;

        this.projects = null;
        this.children = null;
    }

    /** @type {ProjectCollection} */
    get projects() {
        return this._projects;
    }

    set projects(projects) {
        this._setCollection(projects, "_projects", ProjectCollection);
    }

    /** @type {TermCollection} */
    get children() {
        return this._children;
    }

    set children(children) {
        this._setCollection(children, "_children", TermCollection);
    }

    /**
     * @type {TermCollection}
     * Alias for children property
     * */
    get terms() {
        return this._children;
    }

    set terms(terms) {
        this._setCollection(terms, "_children", TermCollection);
    }

    /** @inheritdoc */
    clone() {
        let clone = super.clone();
        // Reattribute the collection properties so that they are processed through _setCollection method
        clone.projects = JSON.parse(JSON.stringify(this.projects.array));
        clone.children = JSON.parse(JSON.stringify(this.children.array));
        // -----
        return clone;
    }

    /**
     * Get the user that created the ontology // QUESTION: how is it different from user field?
     *
     * @returns {User}
     */
    async fetchCreator() {
        if(this.isNew()) {
            throw new Error("Cannot get creator of an ontology with no ID.");
        }

        if(!this._creator) {
            let {data} = await Cytomine.instance.api.get(`${this.callbackIdentifier}/${this.id}/creator.json`);
            this._creator = new User(data.collection[0]);
        }

        return this._creator;
    }
}

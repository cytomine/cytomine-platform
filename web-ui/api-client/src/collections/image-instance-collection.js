import Cytomine from "../cytomine.js";
import Collection from "./collection.js";
import ImageInstance from "../models/image-instance.js";

export default class ProjectCollection extends Collection {

    /** @inheritdoc */
    static get model() {
        return ImageInstance;
    }

    /** @inheritdoc */
    static get allowedFilters() {
        return ["project"];
    }

    /**
     * @static Fetch last opened image instances
     *
     * @param {number} [max=0]    The maximum number of items to retrieve
     * @param {number} [offset=0] The offset
     *
     * @returns {Array<{id: Number, date: String, thumb: String, instanceFilename: String, project: Number}>}
     *          The last opened images
     */
    static async fetchLastOpened(max=0, offset=0) {
        let {data} = await Cytomine.instance.api.get(`imageinstance/method/lastopened.json?max=${max}&offset=${offset}`);
        return data.collection;
    }

    /**
     * @static Fetch all image instances available to current user
     *
     * @param {number} [max=0]    The maximum number of items to retrieve
     * @param {number} [offset=0] The offset
     *
     * @returns {Array<{id: Number, filename: String, originalFilename: String, projectName: String, project: Number}>}
     *          The list of images (light format)
     */
    static async fetchAllLight(max=0, offset=0) {
        let {data} = await Cytomine.instance.api.get(`user/0/imageinstance/light.json?max=${max}&offset=${offset}`); // {user} value ignored in backend => set to 0
        return data.collection;
    }
}

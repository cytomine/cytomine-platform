import Cytomine from '../cytomine.js';
import DomainModel from './domain-model.js';

export default class AttachedFile extends DomainModel {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'attachedfile';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.url = null;
    this.filename = null;
    this._file = null;
  }

  /** @inheritdoc */
  async save() {
    if(this.isNew()) {
      if(!this._file) {
        throw new Error('A file should be provided.');
      }

      let formData = new FormData();
      let props = this.getPublicProperties();
      for(let key in props) {
        formData.append(key, props[key]);
      }
      formData.append('files[]', this._file);
      let {data} = await Cytomine.instance.api.post(this.uri, formData);
      this.populate(data);
      // TODO: store command ID (currently not returned by backend)
      return this;
    }
    else {
      return this.update();
    }

  }

  /** @override */
  update() {
    throw new Error('Update of attached files not implemented in API.');
  }

  /** @inheritdoc */
  get uri() {
    if(this.isNew()) {
      return `${this.callbackIdentifier}.json`;
    }
    else {
      return `${this.callbackIdentifier}/${this.id}.json`;
    }
  }

  /** @type {File|Blob} */
  get file() {
    return this._file;
  }

  set file(f) {
    if(!(f instanceof Blob)) {
      throw new Error('The provided object should be a Blob instance');
    }
    this._file = f;
  }

}

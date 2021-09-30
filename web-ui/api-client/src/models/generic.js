import Model from './model.js';

export default class Generic extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'generic';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();
  }

  populate(props) {
    if(props) {
      for(let key in props) {
        let value = props[key];
        if(key === 'uri') { // special handling to avoid conflict with uri property
          key = 'uri_';
        }
        this[key] = value;
      }
    }
  }


  /** @override */
  update() {
    throw new Error('A generic domain cannot be updated.');
  }

  /** @override */
  delete() {
    throw new Error('A generic domain cannot be deleted.');
  }
}

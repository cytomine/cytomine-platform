import Cytomine from '../cytomine.js';

export default class MyAccount {
  constructor(props) {
    this._initProperties();
    this.populate(props);
  }

  _initProperties() {
    this.id = null;

    this.username = null;
    this.email = null;

    this.firstName = null;
    this.lastName = null;

    this.locale = null;
    this.isDeveloper = null;
  }

  populate(props) {
    if (props) {
      for (let key in props) {
        const value = props[key];
        if (key === 'attributes') {
          if ('locale' in props[key]) {
            const x = props[key]['locale'];
            this['locale'] = Array.isArray(x) && x.length === 1 ? x[0] : null;
          }

          if ('isDeveloper' in props[key]) {
            const x = props[key]['isDeveloper'];
            this['isDeveloper'] = Array.isArray(x) && x.length === 1 ? Boolean(Number(x[0])) : null;
          }
        }
        this[key] = value;
      }
    }
  }

  getPublicProperties() {
    let attributes = {};
    let props = {};
    for (let key in this) {
      let value = this[key];
      if (value !== null) {
        if (key === 'locale') {
          attributes[key] = [value];
        } else if (key === 'isDeveloper') {
          attributes[key] = [String(Number(value))];
        } else {
          props[key] = value;
        }
      }
    }
    props['attributes'] = {...props['attributes'], ...attributes};
    return props;
  }

  clone() {
    return new this.constructor(JSON.parse(JSON.stringify(this)));
  }

  static async fetch() {
    let {data} = await Cytomine.instance.iam.get('account');
    return new this(data);
  }

  async update() {
    await Cytomine.instance.iam.post('account', this.getPublicProperties());
    return this;
  }

  static async fetchCredentials() {
    let {data} = await Cytomine.instance.iam.get('account/credentials');
    return data;
  }
}

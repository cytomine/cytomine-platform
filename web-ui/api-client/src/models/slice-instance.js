import Model from './model.js';

export default class SliceInstance extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'sliceinstance';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.uploadedFile = null;
    this.path = null;
    this.image = null;
    this.mime = null;

    this.channel = null;
    this.zStack = null;
    this.time = null;
  }

  /** @inheritdoc */
  toString() {
    return `[${this.callbackIdentifier}] ${this.id}: ${this.path}`;
  }
}

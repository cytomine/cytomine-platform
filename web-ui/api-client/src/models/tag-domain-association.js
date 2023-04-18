import DomainModel from './domain-model.js';

export default class TagDomainAssociation extends DomainModel {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return 'tagdomainassociation';
  }

  /** @inheritdoc */
  _initProperties() {
    super._initProperties();

    this.tag = null;
  }

  /** @inheritdoc */
  get uri() {

    if(this.isNew()) {
      if(!this.domainClassName || !this.domainIdent) {
        throw new Error('The reference object must be defined to construct the URI.');
      }
      return `domain/${this.domainClassName}/${this.domainIdent}/tag_domain_association.json`;
    }
    else {
      if(!this.domainClassName || !this.domainIdent) {
        return `tag_domain_association/${this.id}.json`;
      }
      return `domain/${this.domainClassName}/${this.domainIdent}/tag_domain_association/${this.id}.json`;
    }
  }

}

import DomainCollection from './domain-collection.js';
import TagDomainAssociation from '../models/tag-domain-association.js';

export default class TagDomainAssociationCollection extends DomainCollection {

  /** @inheritdoc */
  static get model() {
    return TagDomainAssociation;
  }

  /** @inheritdoc */
  static get allowedFilters() {
    return [null, 'project'];
  }

  /** @inheritdoc */
  get uri() {
    if(this.domainClassName && this.domainIdent) {
      return `domain/${this.domainClassName}/${this.domainIdent}/${this.uriWithoutFilter}.json`;
    }
    else {
      return this.uriWithoutFilter;
    }
  }

  /** @inheritdoc */
  get uriWithoutFilter() {
    return 'tag_domain_association.json';
  }

}

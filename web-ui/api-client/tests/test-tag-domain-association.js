import * as utils from './utils.js';
import {Tag, TagDomainAssociation, TagDomainAssociationCollection} from '@';

describe('TagDomainAssociation', function() {

  let tag;
  let project;
  let association = null;
  let id = 0;

  before(async function() {
    await utils.connect();
    let name = utils.randomString();
    let user = await utils.getUser();
    tag = new Tag({name, user});
    ({id: tag} = await tag.save());
    project = await utils.getProject();
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      association = new TagDomainAssociation({tag}, project);
      association = await association.save();
      id = association.id;
      expect(id).to.exist;
      expect(association.tag).to.equal(tag);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedAssociation = await TagDomainAssociation.fetch(id);
      expect(fetchedAssociation).to.be.an.instanceof(TagDomainAssociation);
      expect(fetchedAssociation.tag).to.equal(tag);
    });

    it('Fetch with instance method', async function() {
      let fetchedAssociation = await new TagDomainAssociation({id}).fetch();
      expect(fetchedAssociation).to.be.an.instanceof(TagDomainAssociation);
      expect(fetchedAssociation.tag).to.equal(tag);
    });

    it('Fetch with wrong ID', function() {
      expect(TagDomainAssociation.fetch(0)).to.be.rejected;
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await TagDomainAssociation.delete(id);
    });

    it('Fetch deleted', function() {
      expect(TagDomainAssociation.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('TagDomainAssociationCollection', function() {

    let nbAssociations = 3;
    let associations;
    let totalNb = 0;

    before(async function() {
      try {
        let associationPromises = [];
        for(let i = 0; i < nbAssociations; i++) {
          associationPromises.push(new TagDomainAssociation({tag}, project).save());
        }
        associations = await Promise.all(associationPromises);
      }
      catch(error) {
        console.log('Tag domain association already exists');
      }

    });

    after(async function() {
      let deletionPromises = associations.map(association => TagDomainAssociation.delete(association.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new TagDomainAssociationCollection().fetchAll();
        expect(collection).to.be.an.instanceof(TagDomainAssociationCollection);
        expect(collection).to.have.lengthOf.at.least(nbAssociations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll();
        expect(collection).to.be.an.instanceof(TagDomainAssociationCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll({nbPerPage: 1});
        expect(collection).to.be.an.instanceof(TagDomainAssociationCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll();
        for(let association of collection) {
          expect(association).to.be.an.instanceof(TagDomainAssociation);
        }
      });

      it('Add item to the collection', function() {
        let collection = new TagDomainAssociationCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new TagDomainAssociation());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new TagDomainAssociationCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Filtering', function() {
      it('Filter on project', async function() {
        let project2 = await utils.getProject();
        await new TagDomainAssociation({tag}, project2).save();

        let collection = await new TagDomainAssociationCollection({object: project2}).fetchAll();
        expect(collection).to.have.lengthOf(1);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      /*it('Fetch next page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      /*it('Fetch previous page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });*/
    });

  });

});

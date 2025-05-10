import * as utils from './utils.js';
import {TagDomainAssociation, TagDomainAssociationCollection} from '@';

describe('TagDomainAssociation', function() {

  let tag;
  let project;
  let association = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
    utils.randomString();
    await utils.getUser();
    ({id: tag} = await utils.getTag());
    project = await utils.getProject();
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      association = new TagDomainAssociation({tag}, project);
      association = await association.save();
      id = association.id;
      expect(id).toBeDefined();
      expect(association.tag).toEqual(tag);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedAssociation = await TagDomainAssociation.fetch(id);
      expect(fetchedAssociation).toBeInstanceOf(TagDomainAssociation);
      expect(fetchedAssociation.tag).toEqual(tag);
    });

    it('Fetch with instance method', async function() {
      let fetchedAssociation = await new TagDomainAssociation({id}).fetch();
      expect(fetchedAssociation).toBeInstanceOf(TagDomainAssociation);
      expect(fetchedAssociation.tag).toEqual(tag);
    });

    it('Fetch with wrong ID', function() {
      expect(TagDomainAssociation.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await TagDomainAssociation.delete(id);
    });

    it('Fetch deleted', function() {
      expect(TagDomainAssociation.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('TagDomainAssociationCollection', function() {

    let nbAssociations = 3;
    let associations;
    let totalNb = 0;

    beforeAll(async function() {
      let associationPromises = [];
      for(let i = 0; i < nbAssociations; i++) {
        let tag;
        ({id: tag} = await utils.getTag({forceCreation: true}));
        associationPromises.push(new TagDomainAssociation({tag}, project).save());
      }
      associations = await Promise.all(associationPromises);
    });

    afterAll(async function() {
      let deletionPromises = associations.map(association => TagDomainAssociation.delete(association.id));
      await Promise.all(deletionPromises);
      await utils.cleanData();
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new TagDomainAssociationCollection().fetchAll();
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection).toBeGreaterThanOrEqual(nbAssociations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll();
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll({nbPerPage: 1});
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await TagDomainAssociationCollection.fetchAll();
        for(let association of collection) {
          expect(association).toBeInstanceOf(TagDomainAssociation);
        }
      });

      it('Add item to the collection', function() {
        let collection = new TagDomainAssociationCollection();
        expect(collection).toHaveLength(0);
        collection.push(new TagDomainAssociation());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new TagDomainAssociationCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', function() {
      it('Filter on project', async function() {
        let project2 = await utils.getProject();
        await new TagDomainAssociation({tag}, project2).save();

        let collection = await new TagDomainAssociationCollection({object: project2}).fetchAll();
        expect(collection).toHaveLength(1);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      /*it('Fetch next page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      /*it('Fetch previous page', async function() {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });*/
    });

  });

});

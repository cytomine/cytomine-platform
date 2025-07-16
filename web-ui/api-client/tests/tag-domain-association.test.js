import * as utils from './utils.js';
import {TagDomainAssociation, TagDomainAssociationCollection} from '@/index.js';

describe('TagDomainAssociation', () => {

  let tag;
  let project;
  let association = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
    utils.randomString();
    await utils.getUser();
    ({id: tag} = await utils.getTag());
    project = await utils.getProject();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      association = new TagDomainAssociation({tag}, project);
      association = await association.save();
      id = association.id;
      expect(id).toBeDefined();
      expect(association.tag).toEqual(tag);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedAssociation = await TagDomainAssociation.fetch(id);
      expect(fetchedAssociation).toBeInstanceOf(TagDomainAssociation);
      expect(fetchedAssociation.tag).toEqual(tag);
    });

    it('Fetch with instance method', async () => {
      let fetchedAssociation = await new TagDomainAssociation({id}).fetch();
      expect(fetchedAssociation).toBeInstanceOf(TagDomainAssociation);
      expect(fetchedAssociation.tag).toEqual(tag);
    });

    it('Fetch with wrong ID', async () => {
      await expect(TagDomainAssociation.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await TagDomainAssociation.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(TagDomainAssociation.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('TagDomainAssociationCollection', () => {

    let nbAssociations = 3;
    let associations;
    let totalNb = 0;

    beforeAll(async () => {
      let associationPromises = [];
      for (let i = 0; i < nbAssociations; i++) {
        let tag;
        ({id: tag} = await utils.getTag({forceCreation: true}));
        associationPromises.push(new TagDomainAssociation({tag}, project).save());
      }
      associations = await Promise.all(associationPromises);
    });

    afterAll(async () => {
      let deletionPromises = associations.map(association => TagDomainAssociation.delete(association.id));
      await Promise.all(deletionPromises);
      await utils.cleanData();
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new TagDomainAssociationCollection().fetchAll();
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbAssociations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await TagDomainAssociationCollection.fetchAll();
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await TagDomainAssociationCollection.fetchAll({nbPerPage: 1});
        expect(collection).toBeInstanceOf(TagDomainAssociationCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await TagDomainAssociationCollection.fetchAll();
        for (let association of collection) {
          expect(association).toBeInstanceOf(TagDomainAssociation);
        }
      });

      it('Add item to the collection', () => {
        let collection = new TagDomainAssociationCollection();
        expect(collection).toHaveLength(0);
        collection.push(new TagDomainAssociation());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new TagDomainAssociationCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', () => {
      it('Filter on project', async () => {
        let project2 = await utils.getProject();
        await new TagDomainAssociation({tag}, project2).save();

        let collection = await new TagDomainAssociationCollection({object: project2}).fetchAll();
        expect(collection).toHaveLength(1);
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      /*it('Fetch next page', async () => {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      /*it('Fetch previous page', async () => {
        let collection = new TagDomainAssociationCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });*/
    });

  });

});

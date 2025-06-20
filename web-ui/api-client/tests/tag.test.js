import * as utils from './utils.js';
import {Tag, TagCollection} from '@/index.js';

describe('Tag', () => {

  let name = utils.randomString();
  let user;
  let tag = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
    user = await utils.getUser();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      tag = new Tag({name, user});
      tag = await tag.save();
      id = tag.id;
      expect(id).toBeDefined();
      expect(tag.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedTag = await Tag.fetch(id);
      expect(fetchedTag).toBeInstanceOf(Tag);
      expect(fetchedTag.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedTag = await new Tag({id}).fetch();
      expect(fetchedTag).toBeInstanceOf(Tag);
      expect(fetchedTag.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Tag.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      tag.name = newName;
      tag = await tag.update();
      expect(tag).toBeInstanceOf(Tag);
      expect(tag.name).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Tag.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Tag.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('TagCollection', () => {

    let nbTags = 3;
    let tags;
    let totalNb = 0;

    beforeAll(async () => {
      let tagPromises = [];
      for (let i = 0; i < nbTags; i++) {
        tagPromises.push(new Tag({name: utils.randomString(), user}).save());
      }
      tags = await Promise.all(tagPromises);
    });

    afterAll(async () => {
      let deletionPromises = tags.map(tag => Tag.delete(tag.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new TagCollection().fetchAll();
        expect(collection).toBeInstanceOf(TagCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbTags);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await TagCollection.fetchAll();
        expect(collection).toBeInstanceOf(TagCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await TagCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(TagCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await TagCollection.fetchAll();
        for (let tag of collection) {
          expect(tag).toBeInstanceOf(Tag);
        }
      });

      it('Add item to the collection', () => {
        let collection = new TagCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Tag());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new TagCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new TagCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new TagCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new TagCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

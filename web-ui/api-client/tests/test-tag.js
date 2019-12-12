import * as utils from './utils.js';
import {Tag, TagCollection} from '@';

describe('Tag', function() {

  let name = utils.randomString();
  let user;
  let tag = null;
  let id = 0;

  before(async function() {
    await utils.connect();
    user = await utils.getUser();
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      tag = new Tag({name, user});
      tag = await tag.save();
      id = tag.id;
      expect(id).to.exist;
      expect(tag.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedTag = await Tag.fetch(id);
      expect(fetchedTag).to.be.an.instanceof(Tag);
      expect(fetchedTag.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedTag = await new Tag({id}).fetch();
      expect(fetchedTag).to.be.an.instanceof(Tag);
      expect(fetchedTag.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Tag.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      tag.name = newName;
      tag = await tag.update();
      expect(tag).to.be.an.instanceof(Tag);
      expect(tag.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Tag.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Tag.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('TagCollection', function() {

    let nbTags = 3;
    let tags;
    let totalNb = 0;

    before(async function() {
      let tagPromises = [];
      for(let i = 0; i < nbTags; i++) {
        tagPromises.push(new Tag({name: utils.randomString(), user}).save());
      }
      tags = await Promise.all(tagPromises);
    });

    after(async function() {
      let deletionPromises = tags.map(tag => Tag.delete(tag.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new TagCollection().fetchAll();
        expect(collection).to.be.an.instanceof(TagCollection);
        expect(collection).to.have.lengthOf.at.least(nbTags);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await TagCollection.fetchAll();
        expect(collection).to.be.an.instanceof(TagCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await TagCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(TagCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await TagCollection.fetchAll();
        for(let tag of collection) {
          expect(tag).to.be.an.instanceof(Tag);
        }
      });

      it('Add item to the collection', function() {
        let collection = new TagCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Tag());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new TagCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new TagCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new TagCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new TagCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

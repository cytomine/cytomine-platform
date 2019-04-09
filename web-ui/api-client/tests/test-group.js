import * as utils from './utils.js';
import {Group, GroupCollection} from '@';

describe('Group', function() {
  let name = utils.randomString();

  let group = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
  });

  describe('Create', function() {
    it('Create', async function() {
      group = new Group({name});
      group = await group.save();
      id = group.id;
      expect(group).to.be.an.instanceof(Group);
      expect(group.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedGroup = await Group.fetch(id);
      expect(fetchedGroup).to.be.an.instanceof(Group);
      expect(fetchedGroup.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedGroup = await new Group({id}).fetch();
      expect(fetchedGroup).to.be.an.instanceof(Group);
      expect(fetchedGroup.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Group.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      group.name = newName;
      group = await group.update();
      expect(group).to.be.an.instanceof(Group);
      expect(group.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Group.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Group.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('GroupCollection', function() {

    let nbGroups = 3;
    let groups;
    let totalNb = 0;

    before(async function() {
      let groupPromises = [];
      for(let i = 0; i < nbGroups; i++) {
        groupPromises.push(new Group({name: utils.randomString()}).save());
      }
      groups = await Promise.all(groupPromises);
    });

    after(async function() {
      let deletionPromises = groups.map(group => Group.delete(group.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new GroupCollection().fetchAll();
        expect(collection).to.be.an.instanceof(GroupCollection);
        expect(collection).to.have.lengthOf.at.least(nbGroups);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await GroupCollection.fetchAll();
        expect(collection).to.be.an.instanceof(GroupCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await GroupCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(GroupCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await GroupCollection.fetchAll();
        for(let group of collection) {
          expect(group).to.be.an.instanceof(Group);
        }
      });

      it('Add item to the collection', function() {
        let collection = new GroupCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Group());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new GroupCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new GroupCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new GroupCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new GroupCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

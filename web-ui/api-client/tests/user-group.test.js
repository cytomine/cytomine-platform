import * as utils from './utils.js';

describe('UserGroup', () => {

  beforeAll(async () => {
    await utils.connect(true);
    await utils.getUser();
    //({id: group} = await utils.getGroup());
  });

  /*afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      userGroup = new UserGroup({group, user});
      userGroup = await userGroup.save();
      expect(userGroup).toBeInstanceOf(UserGroup);
      expect(userGroup.id).toBeDefined();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedUserGroup = await UserGroup.fetch(user, group);
      expect(fetchedUserGroup).toBeInstanceOf(UserGroup);
      expect(fetchedUserGroup).toEqual(userGroup);
    });

    it('Fetch with instance method', async () => {
      let fetchedUserGroup = await new UserGroup({user, group}).fetch();
      expect(fetchedUserGroup).toBeInstanceOf(UserGroup);
      expect(fetchedUserGroup).toEqual(userGroup);
    });

    it('Fetch with wrong ID', () => {
      expect(UserGroup.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await UserGroup.delete(user, group);
    });

    it('Fetch deleted', () => {
      expect(UserGroup.fetch(user, group)).rejects.toThrow();
    });
  });

  // --------------------

  describe('UserGroupCollection', () => {

    let nbUserGroups = 3;
    let userGroups;

    beforeAll(async () => {
      async function createGroupAndUserGroup() {
        let tempGroup = await utils.getGroup();
        let userGroup = new UserGroup({user, group: tempGroup.id});
        await userGroup.save();
        return userGroup;
      }

      let userGroupPromises = [];
      for(let i = 0; i < nbUserGroups; i++) {
        userGroupPromises.push(createGroupAndUserGroup());
      }
      userGroups = await Promise.all(userGroupPromises);
    });

    afterAll(async () => {
      let deletionPromises = userGroups.map(userGroup => UserGroup.delete(userGroup.user, userGroup.group));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new UserGroupCollection({filterKey: 'user', filterValue: user}).fetchAll();
        expect(collection).toBeInstanceOf(UserGroupCollection);
        expect(collection).toHaveLength(nbUserGroups);
      });

      it('Fetch (static method)', async () => {
        let collection = await UserGroupCollection.fetchAll({filterKey: 'user', filterValue: user});
        expect(collection).toBeInstanceOf(UserGroupCollection);
        expect(collection).toHaveLength(nbUserGroups);
      });

      it('Fetch with several requests', async () => {
        let collection = await UserGroupCollection.fetchAll({nbPerPage: 1,
          filterKey: 'user', filterValue: user});
        expect(collection).toBeInstanceOf(UserGroupCollection);
        expect(collection).toHaveLength(nbUserGroups);
      });

      it('Fetch without filter', async () => {
        let collection = new UserGroupCollection();
        expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await UserGroupCollection.fetchAll({filterKey: 'user', filterValue: user});
        for(let userGroup of collection) {
          expect(userGroup).toBeInstanceOf(UserGroup);
        }
      });

      it('Add item to the collection', () => {
        let collection = new UserGroupCollection();
        expect(collection).toHaveLength(0);
        collection.push(new UserGroup());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new UserGroupCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new UserGroupCollection({nbPerPage, filterKey: 'user', filterValue: user});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new UserGroupCollection({nbPerPage, filterKey: 'user', filterValue: user});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new UserGroupCollection({nbPerPage, filterKey: 'user', filterValue: user});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });*/

});

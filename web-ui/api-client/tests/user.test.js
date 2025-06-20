import * as utils from './utils.js';
import {User, UserCollection, RoleCollection} from '@/index.js';
import config from './config.js';

describe('User', () => {

  let name = utils.randomString();
  let email = name + '@cytomine.coop';

  let project;

  let user = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    project = await utils.getProject();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      user = new User({username: name, password: name, firstname: name, lastname: name, email});
      user = await user.save();
      id = user.id;
      expect(id).toBeDefined();
      expect(user.username).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedUser = await User.fetch(id);
      expect(fetchedUser).toBeInstanceOf(User);
      expect(fetchedUser.username).toEqual(user.username);
    });

    it('Fetch with instance method', async () => {
      let fetchedUser = await new User({id}).fetch();
      expect(fetchedUser).toBeInstanceOf(User);
      expect(fetchedUser.username).toEqual(user.username);
    });

    it('Fetch with wrong ID', async () => {
      await expect(User.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', () => {
    let role;

    beforeAll(async () => {
      ({id: role} = await utils.getRole());
    });

    it('Fetch current user', async () => {
      let currentUser = await User.fetchCurrent();
      expect(currentUser).toBeInstanceOf(User);
    });

    it('Fetch number of annotations user', async () => {
      let result = await user.fetchNbAnnotations(false);
      expect(Number.isFinite(result)).toBe(true);
      result = await user.fetchNbAnnotations(true);
      expect(Number.isFinite(result)).toBe(true);
    });

    it.skip('Fetch keys', async () => { // Bug in backend
      let keys = await user.fetchKeys();
      expect(keys.publicKey).toBeDefined();
      expect(keys.privateKey).toBeDefined();
    });

    it('Regenerate keys', async () => {
      await user.regenerateKeys();
      // TODO: once bug in backend preventing from fetching keys is solved, check the values of the keys
    });

    it('Fetch friends', async () => {
      let friends = await user.fetchFriends();
      expect(friends).toBeInstanceOf(UserCollection);
    });

    it('Fetch activity resume', async () => {
      let activity = await user.fetchResumeActivity(project.id);
      expect(activity.firstConnection).toBeNull();
      expect(activity.lastConnection).toBeNull();
      expect(activity.totalAnnotations).toEqual(0);
      expect(activity.totalConnections).toEqual(0);
    });

    it('Lock', async () => {
      await user.lock();
      expect(user.enabled).toBe(false);
    });

    it('Unlock', async () => {
      await user.unlock();
      expect(user.enabled).toBe(true);
    });

    it('Define role', async () => {
      let roles = await user.defineRole(role);
      expect(roles).toBeInstanceOf(RoleCollection);
    });

    it('Change password', async () => {
      let newPassword = utils.randomString();
      await user.savePassword(newPassword);
    });

    it('Check password of current user', async () => {
      let result = await User.checkCurrentPassword(config.password);
      expect(result).toBe(true);
      result = await User.checkCurrentPassword(utils.randomString());
      expect(result).toBe(false);
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      user.username = newName;
      user = await user.update();
      expect(user).toBeInstanceOf(User);
      expect(user.username).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await User.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(User.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('UserCollection', () => {

    let nbUsers = 3;
    let users;
    let totalNb = 0;

    beforeAll(async () => {
      let userPromises = [];
      for (let i = 0; i < nbUsers; i++) {
        let name = utils.randomString();
        let email = name + '@cytomine.coop';
        userPromises.push(new User({username: name, password: name, firstname: name, lastname: name, email}).save());
      }
      users = await Promise.all(userPromises);
    });

    afterAll(async () => {
      let deletionPromises = users.map(user => User.delete(user.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new UserCollection().fetchAll();
        expect(collection).toBeInstanceOf(UserCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbUsers);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await UserCollection.fetchAll();
        expect(collection).toBeInstanceOf(UserCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await UserCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(UserCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await UserCollection.fetchAll();
        for (let user of collection) {
          expect(user).toBeInstanceOf(User);
        }
      });

      it('Add item to the collection', () => {
        let collection = new UserCollection();
        expect(collection).toHaveLength(0);
        collection.push(new User());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new UserCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', () => {
      it('Filter on project', async () => {
        await UserCollection.fetchAll({filterKey: 'project', filterValue: project.id});
      });

      it('Filter on ontology', async () => {
        await new UserCollection({nbPerPage: 10, filterKey: 'ontology', filterValue: project.ontology}).fetchAll();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new UserCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new UserCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new UserCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

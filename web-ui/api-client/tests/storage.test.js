import * as utils from './utils.js';
import {Storage, StorageCollection} from '@/index.js';

describe('Storage', () => {

  let name = utils.randomString();
  let user;

  let storageUser = null;
  let storage = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    ({id: user} = await utils.getUser());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    // skipped because a storage for the user seems to be created automatically and this method throws error if the user already possesses a storage
    it.skip('Create for user', async () => {
      storageUser = await Storage.create(user);
      expect(storageUser).toBeInstanceOf(Storage);
      expect(storageUser.user).toEqual(user);
    });

    it('Create', async () => {
      storage = new Storage({name, user});
      storage = await storage.save();
      id = storage.id;
      expect(storage).toBeInstanceOf(Storage);
      expect(id).toBeDefined();
      expect(storage.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedStorage = await Storage.fetch(id);
      expect(fetchedStorage).toBeInstanceOf(Storage);
      expect(fetchedStorage).toEqual(storage);
    });

    it('Fetch with instance method', async () => {
      let fetchedStorage = await new Storage({id}).fetch();
      expect(fetchedStorage).toBeInstanceOf(Storage);
      expect(fetchedStorage).toEqual(storage);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Storage.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      storage.name = newName;
      storage = await storage.update();
      expect(storage).toBeInstanceOf(Storage);
      expect(storage.name).toEqual(newName);
    });
  });

  describe.skip('Delete', () => { // TODO: Remove skip once bug in core is fixed
    it('Delete', async () => {
      await Storage.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Storage.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('StorageCollection', () => {

    let nbStorages = 3;
    let totalNb = 0;

    beforeAll(async () => {
      let storagePromises = [];
      for (let i = 0; i < nbStorages - 1; i++) {
        let str = utils.randomString();
        storagePromises.push(new Storage({name: str, user}).save());
      }
      await Promise.all(storagePromises);
    });

    // remark: not required to clean manually the created storages ; the deletion of the user will lead to their deletions

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new StorageCollection({all: true}).fetchAll();
        expect(collection).toBeInstanceOf(StorageCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbStorages);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await StorageCollection.fetchAll({all: true});
        expect(collection).toBeInstanceOf(StorageCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await StorageCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3), all: true});
        expect(collection).toBeInstanceOf(StorageCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await StorageCollection.fetchAll();
        for (let storage of collection) {
          expect(storage).toBeInstanceOf(Storage);
        }
      });

      it('Add item to the collection', () => {
        let collection = new StorageCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Storage());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new StorageCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;
      let all = true;

      it('Fetch arbitrary page', async () => {
        let collection = new StorageCollection({nbPerPage, all});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new StorageCollection({nbPerPage, all});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new StorageCollection({nbPerPage, all});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

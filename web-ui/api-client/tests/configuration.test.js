import * as utils from './utils.js';
import {Configuration, ConfigurationCollection} from '@/index.js';

describe('Configuration', () => {

  let key = 'KEY';
  let value = utils.randomString();
  let readingRole = 'ALL';

  let config = null;

  beforeAll(async () => {
    await utils.connect(true);
  });

  describe('Create', () => {
    it('Create', async () => {
      config = new Configuration({key, value, readingRole});
      config = await config.save();
      expect(config).toBeInstanceOf(Configuration);
      expect(config.value).toEqual(value);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedConfiguration = await Configuration.fetch(key);
      expect(fetchedConfiguration).toBeInstanceOf(Configuration);
      expect(fetchedConfiguration.value).toEqual(value);
    });

    it('Fetch with instance method', async () => {
      let fetchedConfiguration = await new Configuration({key}).fetch();
      expect(fetchedConfiguration).toBeInstanceOf(Configuration);
      expect(fetchedConfiguration.value).toEqual(value);
    });

    it('Fetch with unknown key', async () => {
      await expect(Configuration.fetch(utils.randomString())).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newValue = utils.randomString();
      config.value = newValue;
      config = await config.update();
      expect(config).toBeInstanceOf(Configuration);
      expect(config.value).toEqual(newValue);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Configuration.delete(key);
    });

    it('Fetch deleted', async () => {
      await expect(Configuration.fetch(key)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ConfigurationCollection', () => {

    let nbConfigurations = 3;
    let configs;
    let totalNb = 0;

    beforeAll(async () => {
      let configPromises = [];
      for (let i = 0; i < nbConfigurations; i++) {
        let str = utils.randomString();
        configPromises.push(new Configuration({key: str, value: str, readingRole}).save());
      }
      configs = await Promise.all(configPromises);
    });

    afterAll(async () => {
      let deletionPromises = configs.map(config => Configuration.delete(config.key));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ConfigurationCollection().fetchAll();
        expect(collection).toBeInstanceOf(ConfigurationCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbConfigurations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await ConfigurationCollection.fetchAll();
        expect(collection).toBeInstanceOf(ConfigurationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await ConfigurationCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(ConfigurationCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ConfigurationCollection.fetchAll();
        for (let config of collection) {
          expect(config).toBeInstanceOf(Configuration);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ConfigurationCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Configuration());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ConfigurationCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ConfigurationCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ConfigurationCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ConfigurationCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

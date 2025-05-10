import * as utils from './utils.js';
import {Configuration, ConfigurationCollection} from '@';

describe('Configuration', function() {

  let key = 'KEY';
  let value = utils.randomString();
  let readingRole = 'ALL';

  let config = null;

  before(async function() {
    await utils.connect(true);
  });

  describe('Create', function() {
    it('Create', async function() {
      config = new Configuration({key, value, readingRole});
      config = await config.save();
      expect(config).to.be.an.instanceof(Configuration);
      expect(config.value).to.equal(value);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedConfiguration = await Configuration.fetch(key);
      expect(fetchedConfiguration).to.be.an.instanceof(Configuration);
      expect(fetchedConfiguration.value).to.equal(value);
    });

    it('Fetch with instance method', async function() {
      let fetchedConfiguration = await new Configuration({key}).fetch();
      expect(fetchedConfiguration).to.be.an.instanceof(Configuration);
      expect(fetchedConfiguration.value).to.equal(value);
    });

    it('Fetch with unknown key', function() {
      expect(Configuration.fetch(utils.randomString())).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newValue = utils.randomString();
      config.value = newValue;
      config = await config.update();
      expect(config).to.be.an.instanceof(Configuration);
      expect(config.value).to.equal(newValue);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Configuration.delete(key);
    });

    it('Fetch deleted', function() {
      expect(Configuration.fetch(key)).to.be.rejected;
    });
  });

  // --------------------

  describe('ConfigurationCollection', function() {

    let nbConfigurations = 3;
    let configs;
    let totalNb = 0;

    before(async function() {
      let configPromises = [];
      for(let i = 0; i < nbConfigurations; i++) {
        let str = utils.randomString();
        configPromises.push(new Configuration({key: str, value: str, readingRole}).save());
      }
      configs = await Promise.all(configPromises);
    });

    after(async function() {
      let deletionPromises = configs.map(config => Configuration.delete(config.key));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ConfigurationCollection().fetchAll();
        expect(collection).to.be.an.instanceof(ConfigurationCollection);
        expect(collection).to.have.lengthOf.at.least(nbConfigurations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ConfigurationCollection.fetchAll();
        expect(collection).to.be.an.instanceof(ConfigurationCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ConfigurationCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(ConfigurationCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ConfigurationCollection.fetchAll();
        for(let config of collection) {
          expect(config).to.be.an.instanceof(Configuration);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ConfigurationCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Configuration());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ConfigurationCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ConfigurationCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ConfigurationCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ConfigurationCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

import * as utils from './utils.js';
import {SoftwareParameter, SoftwareParameterCollection} from '@';

describe.skip('SoftwareParameter', function() {

  let name = utils.randomString();
  let type = 'String';
  let software;

  let softwareParameter = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({id: software} = await utils.getSoftware());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it.skip('Create', async function() {
      softwareParameter = new SoftwareParameter({software, name, type});
      softwareParameter = await softwareParameter.save();
      id = softwareParameter.id;
      expect(softwareParameter).to.be.an.instanceof(SoftwareParameter);
      expect(softwareParameter.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it.skip('Fetch with static method', async function() {
      let fetchedSoftwareParameter = await SoftwareParameter.fetch(id);
      expect(fetchedSoftwareParameter).to.be.an.instanceof(SoftwareParameter);
      expect(fetchedSoftwareParameter).to.deep.equal(softwareParameter);
    });

    it.skip('Fetch with instance method', async function() {
      let fetchedSoftwareParameter = await new SoftwareParameter({id}).fetch();
      expect(fetchedSoftwareParameter).to.be.an.instanceof(SoftwareParameter);
      expect(fetchedSoftwareParameter).to.deep.equal(softwareParameter);
    });

    it.skip('Fetch with wrong ID', function() {
      expect(SoftwareParameter.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it.skip('Update', async function() {
      let newName = utils.randomString();
      softwareParameter.name = newName;
      softwareParameter = await softwareParameter.update();
      expect(softwareParameter).to.be.an.instanceof(SoftwareParameter);
      expect(softwareParameter.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it.skip('Delete', async function() {
      await SoftwareParameter.delete(id);
    });

    it.skip('Fetch deleted', function() {
      expect(SoftwareParameter.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('SoftwareParameterCollection', function() {

    let nbSoftwareParameters = 3;
    let softwareParameters;
    let totalNb = 0;

    before(async function() {
      let softwareParameterPromises = [];
      for(let i = 0; i < nbSoftwareParameters; i++) {
        let param = new SoftwareParameter({name: utils.randomString(), software, type});
        softwareParameterPromises.push(param.save());
      }
      softwareParameters = await Promise.all(softwareParameterPromises);
    });

    after(async function() {
      let deletionPromises = softwareParameters.map(softwareParameter => SoftwareParameter.delete(softwareParameter.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it.skip('Fetch (instance method)', async function() {
        let collection = await new SoftwareParameterCollection().fetchAll();
        expect(collection).to.be.an.instanceof(SoftwareParameterCollection);
        expect(collection).to.have.lengthOf.at.least(nbSoftwareParameters);
        totalNb = collection.length;
      });

      it.skip('Fetch (static method)', async function() {
        let collection = await SoftwareParameterCollection.fetchAll();
        expect(collection).to.be.an.instanceof(SoftwareParameterCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it.skip('Fetch with several requests', async function() {
        let collection = await SoftwareParameterCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(SoftwareParameterCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Filtering', function() {
      it.skip('Filter on software (static method)', async function() {
        let collection = await SoftwareParameterCollection.fetchAll({filterKey: 'software', filterValue: software});
        expect(collection).to.have.lengthOf(nbSoftwareParameters);
      });

      it.skip('Filter on software (instance method)', async function() {
        let collection = new SoftwareParameterCollection(0);
        collection.setFilter('software', software);
        await collection.fetchAll();
        expect(collection).to.have.lengthOf(nbSoftwareParameters);
      });
    });

    describe('Working with the collection', function() {
      it.skip('Iterate through', async function() {
        let collection = await SoftwareParameterCollection.fetchAll();
        for(let softwareParameter of collection) {
          expect(softwareParameter).to.be.an.instanceof(SoftwareParameter);
        }
      });

      it.skip('Add item to the collection', function() {
        let collection = new SoftwareParameterCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new SoftwareParameter());
        expect(collection).to.have.lengthOf(1);
      });

      it.skip('Add arbitrary object to the collection', function() {
        let collection = new SoftwareParameterCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it.skip('Fetch arbitrary page', async function() {
        let collection = new SoftwareParameterCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it.skip('Fetch next page', async function() {
        let collection = new SoftwareParameterCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it.skip('Fetch previous page', async function() {
        let collection = new SoftwareParameterCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

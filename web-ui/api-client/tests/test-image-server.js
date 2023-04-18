import * as utils from './utils.js';
import {ImageServer, ImageServerCollection} from '@';

describe('ImageServer', function() {
  let totalNb = 0;

  let imageServer;

  before(async function() {
    await utils.connect(true);
  });

  describe('Fetch collection', function() {
    it('Fetch (instance method)', async function() {
      let collection = await new ImageServerCollection().fetchAll();
      expect(collection).to.be.an.instanceof(ImageServerCollection);
      totalNb = collection.length;
      imageServer = collection.get(0);
    });

    it('Fetch (static method)', async function() {
      let collection = await ImageServerCollection.fetchAll();
      expect(collection).to.be.an.instanceof(ImageServerCollection);
      expect(collection).to.have.lengthOf(totalNb);
    });

    it('Fetch with several requests', async function() {
      let collection = await ImageServerCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
      expect(collection).to.be.an.instanceof(ImageServerCollection);
      expect(collection).to.have.lengthOf(totalNb);
    });
  });

  describe('Working with the collection', function() {
    it('Iterate through', async function() {
      let collection = await ImageServerCollection.fetchAll();
      for(let imageServer of collection) {
        expect(imageServer).to.be.an.instanceof(ImageServer);
      }
    });

    it('Add item to the collection', function() {
      let collection = new ImageServerCollection();
      expect(collection).to.have.lengthOf(0);
      collection.push(new ImageServer());
      expect(collection).to.have.lengthOf(1);
    });

    it('Add arbitrary object to the collection', function() {
      let collection = new ImageServerCollection();
      expect(collection.push.bind(collection, {})).to.throw();
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedImageServer = await ImageServer.fetch(imageServer.id);
      expect(fetchedImageServer).to.be.an.instanceof(ImageServer);
      expect(fetchedImageServer).to.deep.equal(imageServer);
    });

    it('Fetch with instance method', async function() {
      let fetchedImageServer = await new ImageServer({id: imageServer.id}).fetch();
      expect(fetchedImageServer).to.be.an.instanceof(ImageServer);
      expect(fetchedImageServer).to.deep.equal(imageServer);
    });

    it('Fetch with wrong ID', function() {
      expect(ImageServer.fetch(0)).to.be.rejected;
    });
  });

});

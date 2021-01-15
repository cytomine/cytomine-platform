import * as utils from './utils.js';
import {ProcessingServer, ProcessingServerCollection} from '@';

describe('Processing server', function() {

  let name = utils.randomString();
  let host = utils.randomString();
  let username = utils.randomString();
  let port = 5555;

  let processingServer = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
  });

  describe('Create', function() {
    it('Create', async function() {
      processingServer = new ProcessingServer({name, host, username, port});
      processingServer = await processingServer.save();
      id = processingServer.id;
      expect(processingServer).to.be.an.instanceof(ProcessingServer);
      expect(processingServer.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedProcessingServer = await ProcessingServer.fetch(id);
      expect(fetchedProcessingServer).to.be.an.instanceof(ProcessingServer);
      expect(fetchedProcessingServer.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedProcessingServer = await new ProcessingServer({id}).fetch();
      expect(fetchedProcessingServer).to.be.an.instanceof(ProcessingServer);
      expect(fetchedProcessingServer.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(ProcessingServer.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      expect(processingServer.update.bind(processingServer)).to.throw();
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ProcessingServer.delete(id);
    });

    it('Fetch deleted', function() {
      expect(ProcessingServer.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('ProcessingServerCollection', function() {

    let nbProcessingServers = 3;
    let processingServers;
    let totalNb = 0;

    before(async function() {
      let processingServerPromises = [];
      for(let i = 0; i < nbProcessingServers; i++) {
        processingServerPromises.push(new ProcessingServer({name: utils.randomString(),host, username, port}).save());
      }
      processingServers = await Promise.all(processingServerPromises);
    });

    after(async function() {
      let deletionPromises = processingServers.map(processingServer => ProcessingServer.delete(processingServer.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ProcessingServerCollection().fetchAll();
        expect(collection).to.be.an.instanceof(ProcessingServerCollection);
        expect(collection).to.have.lengthOf.at.least(nbProcessingServers);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ProcessingServerCollection.fetchAll();
        expect(collection).to.be.an.instanceof(ProcessingServerCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ProcessingServerCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(ProcessingServerCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ProcessingServerCollection.fetchAll();
        for(let processingServer of collection) {
          expect(processingServer).to.be.an.instanceof(ProcessingServer);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ProcessingServerCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new ProcessingServer());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ProcessingServerCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ProcessingServerCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ProcessingServerCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ProcessingServerCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

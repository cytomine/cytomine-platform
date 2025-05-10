import * as utils from './utils.js';
import {ProjectConnection, ProjectConnectionCollection, User} from '@';

describe('ProjectConnection', function() {
  let project;
  let projectConnection = null;

  beforeAll(async function() {
    await utils.connect();
    ({id: project} = await utils.getProject());
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      projectConnection = new ProjectConnection({project});
      await projectConnection.save();
      expect(projectConnection).toBeInstanceOf(ProjectConnection);
      expect(projectConnection.id).toBebove(0);
    });
  });

  describe('Fetch', function() {
    it('Fetch', function() {
      expect(projectConnection.fetch.bind(projectConnection)).toThrow();
    });
  });

  describe('Update', function() {
    it('Update', function() {
      expect(projectConnection.update.bind(projectConnection)).toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', function() {
      expect(projectConnection.delete.bind(projectConnection)).toThrow();
    });
  });


  // --------------------

  describe('ProjectConnectionCollection', function() {

    let nbConnections = 3;
    let totalNb = 0;
    let user;

    beforeAll(async function() {
      ({id: user} = await User.fetchCurrent());

      let connectionsPromise = [];
      for(let i = 0; i < nbConnections; i++) {
        connectionsPromise.push(new ProjectConnection({project}).save());
      }
      connectionsPromise = await Promise.all(connectionsPromise);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ProjectConnectionCollection({user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection).toBeGreaterThanOrEqual(nbConnections);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ProjectConnectionCollection.fetchAll({user, project});
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch with several requests', async function() { // incorrect values returned for size and totalPages
        let collection = await ProjectConnectionCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without filter', async function() {
        let collection = new ProjectConnectionCollection();
        expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ProjectConnectionCollection.fetchAll({user, project});
        for(let connection of collection) {
          expect(connection).toBeInstanceOf(ProjectConnection);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ProjectConnectionCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ProjectConnection());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ProjectConnectionCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });

      it('Download URL', async function() {
        let collection = new ProjectConnectionCollection({user, project});
        expect(collection.downloadURL).toBe('string');
      });
    });

    describe('Specific operations', function() {
      it('Fetch average connections', async function() {
        let result = await ProjectConnectionCollection.fetchAverageConnections({project, beforeThan: new Date().getTime()});
        expect(result).toBeInstanceOf(Array);
      });

      it('Fetch connections frequency', async function() {
        let result = await ProjectConnectionCollection.fetchConnectionsFrequency({project, beforeThan: new Date().getTime()});
        expect(result).toBeInstanceOf(Array);
      });
    });


    describe.skip('Pagination', function() { // incorrect values returned for size and totalPages
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

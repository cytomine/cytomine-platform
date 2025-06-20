import * as utils from './utils.js';
import {ProjectConnection, ProjectConnectionCollection, User} from '@/index.js';

describe('ProjectConnection', () => {
  let project;
  let projectConnection = null;

  beforeAll(async () => {
    await utils.connect();
    ({id: project} = await utils.getProject());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      projectConnection = new ProjectConnection({project});
      await projectConnection.save();
      expect(projectConnection).toBeInstanceOf(ProjectConnection);
      expect(projectConnection.id).toBeGreaterThan(0);
    });
  });

  describe('Fetch', () => {
    it('Fetch', () => {
      expect(projectConnection.fetch.bind(projectConnection)).toThrow();
    });
  });

  describe('Update', () => {
    it('Update', () => {
      expect(projectConnection.update.bind(projectConnection)).toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', () => {
      expect(projectConnection.delete.bind(projectConnection)).toThrow();
    });
  });

  // --------------------

  describe('ProjectConnectionCollection', () => {

    let nbConnections = 3;
    let totalNb = 0;
    let user;

    beforeAll(async () => {
      ({id: user} = await User.fetchCurrent());

      let connectionsPromise = [];
      for (let i = 0; i < nbConnections; i++) {
        connectionsPromise.push(new ProjectConnection({project}).save());
      }
      connectionsPromise = await Promise.all(connectionsPromise);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ProjectConnectionCollection({user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbConnections);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await ProjectConnectionCollection.fetchAll({user, project});
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch with several requests', async () => { // incorrect values returned for size and totalPages
        let collection = await ProjectConnectionCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(ProjectConnectionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without filter', async () => {
        let collection = new ProjectConnectionCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ProjectConnectionCollection.fetchAll({user, project});
        for (let connection of collection) {
          expect(connection).toBeInstanceOf(ProjectConnection);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ProjectConnectionCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ProjectConnection());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ProjectConnectionCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });

      it('Download URL', async () => {
        let collection = new ProjectConnectionCollection({user, project});
        expect(typeof collection.downloadURL).toBe('string');
      });
    });

    describe('Specific operations', () => {
      it('Fetch average connections', async () => {
        let result = await ProjectConnectionCollection.fetchAverageConnections({project, beforeThan: new Date().getTime()});
        expect(result).toBeInstanceOf(Array);
      });

      it('Fetch connections frequency', async () => {
        let result = await ProjectConnectionCollection.fetchConnectionsFrequency({project, beforeThan: new Date().getTime()});
        expect(result).toBeInstanceOf(Array);
      });
    });

    describe.skip('Pagination', () => { // incorrect values returned for size and totalPages
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ProjectConnectionCollection({user, project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });
  });
});

import * as utils from './utils.js';
import {UserPosition, UserPositionCollection, User} from '@/index.js';

// WARNING the user positions created by these tests are not deleted
describe('UserPosition', () => {

  let image;

  let userPosition = null;

  beforeAll(async () => {
    await utils.connect();
    ({id: image} = await utils.getImageInstance());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      userPosition = await UserPosition.create({
        image, topLeftX: 0, topLeftY: 10, topRightX: 10, topRightY: 10,
        bottomLeftX: 0, bottomLeftY: 0, bottomRightX: 10, bottomRightY: 0
      });
      expect(userPosition).toBeInstanceOf(UserPosition);
      expect(userPosition.location).toBeDefined();
      expect(userPosition.x).toEqual(5);
      expect(userPosition.y).toEqual(5);
    });
  });

  describe('Fetch', () => {
    it('Fetch last position', async () => {
      let currentUser = await User.fetchCurrent();
      let fetchedUserPosition = await UserPosition.fetchLastPosition(image, currentUser.id);
      expect(fetchedUserPosition).toBeInstanceOf(UserPosition);
      expect(fetchedUserPosition.location).toEqual(userPosition.location);
    });
  });

  // --------------------

  describe('UserPositionCollection', () => {

    let nbUserPositions = 3;
    let totalNb = 0;

    beforeAll(async () => {
      let userPositionPromises = [];
      for (let i = 0; i < nbUserPositions; i++) {
        userPositionPromises.push(UserPosition.create({
          image, topLeftX: 0, topLeftY: 10, topRightX: 10, topRightY: 10,
          bottomLeftX: 0, bottomLeftY: 0, bottomRightX: 10, bottomRightY: 0
        }));
      }
      await Promise.all(userPositionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new UserPositionCollection({filterKey: 'imageinstance', filterValue: image}).fetchAll();
        expect(collection).toBeInstanceOf(UserPositionCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbUserPositions);
        totalNb = collection.length;
      });

      it('Fetch (instance method) - without details', async () => {
        let collection = await new UserPositionCollection({
          showDetails: false,
          filterKey: 'imageinstance',
          filterValue: image,
        }).fetchAll();
        expect(collection).toBeInstanceOf(UserPositionCollection);
        expect(collection.length).toBeLessThanOrEqual(totalNb); // some userpositions are aggregated
        expect(collection.length).toBeGreaterThanOrEqual(1);
      });

      it('Fetch (static method)', async () => {
        let collection = await UserPositionCollection.fetchAll({filterKey: 'imageinstance', filterValue: image});
        expect(collection).toBeInstanceOf(UserPositionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch with several requests', async () => {
        let collection = await UserPositionCollection.fetchAll({
          nbPerPage: Math.ceil(totalNb / 3),
          filterKey: 'imageinstance', filterValue: image
        });
        expect(collection).toBeInstanceOf(UserPositionCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without filter', async () => {
        let collection = new UserPositionCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await UserPositionCollection.fetchAll({filterKey: 'imageinstance', filterValue: image});
        for (let userPosition of collection) {
          expect(userPosition).toBeInstanceOf(UserPosition);
        }
      });

      it('Add item to the collection', () => {
        let collection = new UserPositionCollection();
        expect(collection).toHaveLength(0);
        collection.push(new UserPosition());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new UserPositionCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe.skip('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new UserPositionCollection({nbPerPage, filterKey: 'imageinstance', filterValue: image});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new UserPositionCollection({nbPerPage, filterKey: 'imageinstance', filterValue: image});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new UserPositionCollection({nbPerPage, filterKey: 'imageinstance', filterValue: image});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

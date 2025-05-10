import * as utils from './utils.js';
import { ImageServer, ImageServerCollection } from '@/index.js';

describe('ImageServer', function () {
  let totalNb = 0;

  let imageServer;

  beforeAll(async function () {
    await utils.connect(true);
  });

  describe('Fetch collection', function () {
    it('Fetch (instance method)', async function () {
      let collection = await new ImageServerCollection().fetchAll();
      expect(collection).toBeInstanceOf(ImageServerCollection);
      totalNb = collection.length;
      imageServer = collection.get(0);
    });

    it('Fetch (static method)', async function () {
      let collection = await ImageServerCollection.fetchAll();
      expect(collection).toBeInstanceOf(ImageServerCollection);
      expect(collection).toHaveLength(totalNb);
    });

    it('Fetch with several requests', async function () {
      let collection = await ImageServerCollection.fetchAll({ nbPerPage: Math.ceil(totalNb / 3) });
      expect(collection).toBeInstanceOf(ImageServerCollection);
      expect(collection).toHaveLength(totalNb);
    });
  });

  describe('Working with the collection', function () {
    it('Iterate through', async function () {
      let collection = await ImageServerCollection.fetchAll();
      for (let imageServer of collection) {
        expect(imageServer).toBeInstanceOf(ImageServer);
      }
    });

    it('Add item to the collection', function () {
      let collection = new ImageServerCollection();
      expect(collection).toHaveLength(0);
      collection.push(new ImageServer());
      expect(collection).toHaveLength(1);
    });

    it('Add arbitrary object to the collection', function () {
      let collection = new ImageServerCollection();
      expect(collection.push.bind(collection, {})).toThrow();
    });
  });

  describe('Fetch', function () {
    it('Fetch with static method', async function () {
      let fetchedImageServer = await ImageServer.fetch(imageServer.id);
      expect(fetchedImageServer).toBeInstanceOf(ImageServer);
      expect(fetchedImageServer).toEqual(imageServer);
    });

    it('Fetch with instance method', async function () {
      let fetchedImageServer = await new ImageServer({ id: imageServer.id }).fetch();
      expect(fetchedImageServer).toBeInstanceOf(ImageServer);
      expect(fetchedImageServer).toEqual(imageServer);
    });

    it('Fetch with wrong ID', function () {
      expect(ImageServer.fetch(0)).rejects.toThrow();
    });
  });

});

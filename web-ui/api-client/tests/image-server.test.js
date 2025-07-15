import * as utils from './utils.js';
import {ImageServer, ImageServerCollection} from '@/index.js';

describe('ImageServer', function () {
  beforeAll(async function () {
    await utils.connect(true);
  });

  describe('Working with the collection', function () {
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
});

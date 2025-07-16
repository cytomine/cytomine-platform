import * as utils from './utils.js';
import {ImageFilter, ImageFilterCollection} from '@/index.js';

// Skipped
describe.skip('Image filter', () => {
  let name = utils.randomString();
  let id;
  let imageFilter;

  beforeAll(async () => {
    await utils.connect(true);
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      imageFilter = new ImageFilter({name, baseUrl: 'path/'});
      imageFilter = await imageFilter.save();
      id = imageFilter.id;
      expect(imageFilter).toBeInstanceOf(ImageFilter);
      expect(imageFilter.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedImageFilter = await ImageFilter.fetch(id);
      expect(fetchedImageFilter).toBeInstanceOf(ImageFilter);
      expect(fetchedImageFilter.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedImageFilter = await new ImageFilter({id}).fetch();
      expect(fetchedImageFilter).toBeInstanceOf(ImageFilter);
      expect(fetchedImageFilter.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(ImageFilter.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      expect(imageFilter.update.bind(imageFilter)).toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await ImageFilter.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(ImageFilter.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ImageFilterCollection', () => {

    let nbImageFilters = 3;
    let imageFilters;
    let totalNb = 0;

    beforeAll(async () => {
      let imageFilterPromises = [];
      for (let i = 0; i < nbImageFilters; i++) {
        imageFilterPromises.push(new ImageFilter({
          name: utils.randomString(),
          baseUrl: 'path/'
        }).save());
      }
      imageFilters = await Promise.all(imageFilterPromises);
    });

    afterAll(async () => {
      let deletionPromises = imageFilters.map(imageFilter => ImageFilter.delete(imageFilter.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ImageFilterCollection().fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toBeGreaterThanOrEqual(nbImageFilters);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await ImageFilterCollection.fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await ImageFilterCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(ImageFilterCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ImageFilterCollection.fetchAll();
        for (let imageFilter of collection) {
          expect(imageFilter).toBeInstanceOf(ImageFilter);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ImageFilterCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageFilter());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ImageFilterCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ImageFilterCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

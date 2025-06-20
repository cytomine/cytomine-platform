import * as utils from './utils.js';
import {User} from '@/index';
import {AbstractImage, AbstractImageCollection} from '@/index.js';

describe('AbstractImageCollection', () => {
  let originalFilename = utils.randomString();
  let uploadedFile;
  let user;
  let storage;

  let abstractImage = null;
  let totalNb = 0;

  beforeAll(async () => {
    await utils.connect();

    ({id: user} = await User.fetchCurrent());
    ({id: storage} = await utils.getStorage({user}));
    ({id: uploadedFile} = await utils.getUploadedFile({storage, originalFilename}));

    await utils.getProject();
    abstractImage = new AbstractImage({originalFilename, uploadedFile, width: 1000, height: 1000});
    abstractImage = await abstractImage.save();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Fetch', () => {
    it('Fetch the whole collection', async () => {
      let collection = await AbstractImageCollection.fetchAll();
      expect(collection).toBeInstanceOf(AbstractImageCollection);
      totalNb = collection.length;
    });

    it('Fetch with several requests', async () => {
      let collection = await AbstractImageCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
      expect(collection).toBeInstanceOf(AbstractImageCollection);
      expect(collection).toHaveLength(totalNb);
    });

    it('Fetch unused', async () => {
      let collection = await AbstractImageCollection.fetchUnused();
      expect(collection).toBeInstanceOf(AbstractImageCollection);
      expect(collection.length).toBeLessThanOrEqual(totalNb);
    });
  });

  describe('Working with the collection', () => {
    it('Iterate through abstract images', async () => {
      let collection = await new AbstractImageCollection({nbPerPage: 10}).fetchPage();
      for (let image of collection) {
        expect(image).toBeInstanceOf(AbstractImage);
      }
    });

    it('Add an image to the collection', () => {
      let collection = new AbstractImageCollection();
      expect(collection).toHaveLength(0);
      collection.push(new AbstractImage());
      expect(collection).toHaveLength(1);
    });

    it('Add arbitrary object to the collection', () => {
      let collection = new AbstractImageCollection();
      expect(collection.push.bind(collection, {})).toThrow();
    });
  });

  describe('Pagination', () => {
    let nbPerPage = 1;

    it('Fetch arbitrary page', async () => {
      let collection = new AbstractImageCollection({nbPerPage});
      await collection.fetchPage(0);
      expect(collection).toHaveLength(nbPerPage);
    });

    it('Fetch next page', async () => {
      let collection = new AbstractImageCollection({nbPerPage});
      try {
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      } catch (oobError) {
        // ignore, may happen as we have only 1 image
      }
    });

    it('Fetch previous page', async () => {
      let collection = new AbstractImageCollection({nbPerPage});
      collection.curPage = 1;
      await collection.fetchPreviousPage();
      expect(collection).toHaveLength(nbPerPage);
    });
  });
});

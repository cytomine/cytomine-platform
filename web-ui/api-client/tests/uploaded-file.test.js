import * as utils from './utils.js';
import {UploadedFile, UploadedFileCollection, User} from '@/index.js';

describe('UploadedFile', function() {

  let storage;
  let user;
  let ext = '.ext';
  let filename = utils.randomString() + ext;
  let contentType = 'contentType';

  let uploadedFile = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect(true);
    ({id: user} = await User.fetchCurrent());
    ({id: storage} = await utils.getStorage({user}));
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      uploadedFile = new UploadedFile({
        storage, user, filename, originalFilename: filename, contentType, ext
      });
      uploadedFile = await uploadedFile.save();
      id = uploadedFile.id;
      expect(uploadedFile).toBeInstanceOf(UploadedFile);
      expect(uploadedFile.filename).toEqual(filename);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedUploadedFile = await UploadedFile.fetch(id);
      expect(fetchedUploadedFile).toBeInstanceOf(UploadedFile);
      expect(fetchedUploadedFile).toEqual(uploadedFile);
    });

    it('Fetch with instance method', async function() {
      let fetchedUploadedFile = await new UploadedFile({id}).fetch();
      expect(fetchedUploadedFile).toBeInstanceOf(UploadedFile);
      expect(fetchedUploadedFile).toEqual(uploadedFile);
    });

    it('Fetch with wrong ID', function() {
      expect(UploadedFile.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', function() {
    it('Download URL', async function() {
      expect(uploadedFile.downloadURL).toBe('string');
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newFilename = utils.randomString();
      uploadedFile.filename = newFilename;
      uploadedFile = await uploadedFile.update();
      expect(uploadedFile).toBeInstanceOf(UploadedFile);
      expect(uploadedFile.filename).toEqual(newFilename);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await UploadedFile.delete(id);
    });

    it('Fetch deleted', function() {
      expect(UploadedFile.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('UploadedFileCollection', function() {
    let nbUploadedFiles = 3;
    let totalNb = 0;

    let uploadedFiles;

    beforeAll(async function() {
      let uploadedFilePromises = [];
      for(let i = 0; i < nbUploadedFiles; i++) {
        let tmp = utils.randomString();
        uploadedFilePromises.push(new UploadedFile({
          storage, user, filename: tmp, originalFilename: tmp, contentType, ext
        }).save());
      }
      uploadedFiles = await Promise.all(uploadedFilePromises);
    });

    afterAll(async function() {
      let deletionPromises = uploadedFiles.map(uf => UploadedFile.delete(uf.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new UploadedFileCollection().fetchAll();
        expect(collection).toBeInstanceOf(UploadedFileCollection);
        expect(collection).toBeGreaterThanOrEqual(nbUploadedFiles);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await UploadedFileCollection.fetchAll();
        expect(collection).toBeInstanceOf(UploadedFileCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await UploadedFileCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(UploadedFileCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await UploadedFileCollection.fetchAll();
        for(let uploadedFile of collection) {
          expect(uploadedFile).toBeInstanceOf(UploadedFile);
        }
      });

      it('Add item to the collection', function() {
        let collection = new UploadedFileCollection();
        expect(collection).toHaveLength(0);
        collection.push(new UploadedFile());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new UploadedFileCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

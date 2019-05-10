import * as utils from './utils.js';
import {UploadedFile, UploadedFileCollection, User} from '@';

describe('UploadedFile', function() {

  let storage;
  let user;
  let imageServer;
  let ext = '.ext';
  let filename = utils.randomString() + ext;
  let contentType = 'contentType';

  let uploadedFile = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({id: storage} = await utils.getStorage());
    ({id: user} = await User.fetchCurrent());
    ({id: imageServer} = await utils.getImageServer());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      uploadedFile = new UploadedFile({
        storage, user, imageServer, filename, originalFilename: filename, contentType, ext
      });
      uploadedFile = await uploadedFile.save();
      id = uploadedFile.id;
      expect(uploadedFile).to.be.an.instanceof(UploadedFile);
      expect(id).to.exist;
      expect(uploadedFile.filename).to.equal(filename);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedUploadedFile = await UploadedFile.fetch(id);
      expect(fetchedUploadedFile).to.be.an.instanceof(UploadedFile);
      expect(fetchedUploadedFile).to.deep.equal(uploadedFile);
    });

    it('Fetch with instance method', async function() {
      let fetchedUploadedFile = await new UploadedFile({id}).fetch();
      expect(fetchedUploadedFile).to.be.an.instanceof(UploadedFile);
      expect(fetchedUploadedFile).to.deep.equal(uploadedFile);
    });

    it('Fetch with wrong ID', function() {
      expect(UploadedFile.fetch(0)).to.be.rejected;
    });
  });

  describe('Specific operations', function() {
    it('Download URL', async function() {
      expect(uploadedFile.downloadURL).to.be.a('string');
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newFilename = utils.randomString();
      uploadedFile.filename = newFilename;
      uploadedFile = await uploadedFile.update();
      expect(uploadedFile).to.be.an.instanceof(UploadedFile);
      expect(uploadedFile.filename).to.equal(newFilename);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await UploadedFile.delete(id);
    });

    it('Fetch deleted', function() {
      expect(UploadedFile.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('UploadedFileCollection', function() {
    let nbUploadedFiles = 3;
    let totalNb = 0;

    let uploadedFiles;

    before(async function() {
      let uploadedFilePromises = [];
      for(let i = 0; i < nbUploadedFiles - 1; i++) {
        let tmp = utils.randomString();
        uploadedFilePromises.push(new UploadedFile({
          storage, user, imageServer, filename: tmp, originalFilename: tmp, contentType, ext
        }).save());
      }
      uploadedFiles = await Promise.all(uploadedFilePromises);
    });

    after(async function() {
      let deletionPromises = uploadedFiles.map(uf => UploadedFile.delete(uf.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new UploadedFileCollection().fetchAll();
        expect(collection).to.be.an.instanceof(UploadedFileCollection);
        expect(collection).to.have.lengthOf.at.least(nbUploadedFiles);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await UploadedFileCollection.fetchAll();
        expect(collection).to.be.an.instanceof(UploadedFileCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await UploadedFileCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(UploadedFileCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await UploadedFileCollection.fetchAll();
        for(let uploadedFile of collection) {
          expect(uploadedFile).to.be.an.instanceof(UploadedFile);
        }
      });

      it('Add item to the collection', function() {
        let collection = new UploadedFileCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new UploadedFile());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new UploadedFileCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new UploadedFileCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

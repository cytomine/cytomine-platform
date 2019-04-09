import * as utils from './utils.js';
import {ImageFilter, ImageFilterCollection} from '@';

describe('Image filter', function() {
  let processingServer;
  let name = utils.randomString();

  let imageFilter = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    processingServer = await utils.getProcessingServer();
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      imageFilter = new ImageFilter({name, processingServer: processingServer.url, baseUrl: 'path/'});
      imageFilter = await imageFilter.save();
      id = imageFilter.id;
      expect(imageFilter).to.be.an.instanceof(ImageFilter);
      expect(imageFilter.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedImageFilter = await ImageFilter.fetch(id);
      expect(fetchedImageFilter).to.be.an.instanceof(ImageFilter);
      expect(fetchedImageFilter.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedImageFilter = await new ImageFilter({id}).fetch();
      expect(fetchedImageFilter).to.be.an.instanceof(ImageFilter);
      expect(fetchedImageFilter.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(ImageFilter.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      expect(imageFilter.update.bind(imageFilter)).to.throw();
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ImageFilter.delete(id);
    });

    it('Fetch deleted', function() {
      expect(ImageFilter.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('ImageFilterCollection', function() {

    let nbImageFilters = 3;
    let imageFilters;
    let totalNb = 0;

    before(async function() {
      let imageFilterPromises = [];
      for(let i = 0; i < nbImageFilters; i++) {
        imageFilterPromises.push(new ImageFilter({
          name: utils.randomString(),
          processingServer: processingServer.url,
          baseUrl: 'path/'
        }).save());
      }
      imageFilters = await Promise.all(imageFilterPromises);
    });

    after(async function() {
      let deletionPromises = imageFilters.map(imageFilter => ImageFilter.delete(imageFilter.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ImageFilterCollection().fetchAll();
        expect(collection).to.be.an.instanceof(ImageFilterCollection);
        expect(collection).to.have.lengthOf.at.least(nbImageFilters);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ImageFilterCollection.fetchAll();
        expect(collection).to.be.an.instanceof(ImageFilterCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ImageFilterCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(ImageFilterCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ImageFilterCollection.fetchAll();
        for(let imageFilter of collection) {
          expect(imageFilter).to.be.an.instanceof(ImageFilter);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ImageFilterCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new ImageFilter());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ImageFilterCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ImageFilterCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

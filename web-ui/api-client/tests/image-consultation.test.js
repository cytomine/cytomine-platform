import * as utils from './utils.js';
import {ProjectConnection, ImageConsultation, ImageConsultationCollection, User} from '@';

describe('ImageConsultation', function() {
  let project;
  let image;
  let projectConnection;

  let imageConsultation = null;

  beforeAll(async function() {
    await utils.connect();
    ({id: project} = await utils.getProject());
    ({id: projectConnection} = await new ProjectConnection({project}).save());
    ({id: image} = await utils.getImageInstance({project}));
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      imageConsultation = new ImageConsultation({image});
      await imageConsultation.save();
      expect(imageConsultation).toBeInstanceOf(ImageConsultation);
      expect(imageConsultation.id).toBebove(0);
    });
  });

  describe('Fetch', function() {
    it('Fetch', function() {
      expect(imageConsultation.fetch.bind(imageConsultation)).toThrow();
    });
  });

  describe('Update', function() {
    it('Update', function() {
      expect(imageConsultation.update.bind(imageConsultation)).toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', function() {
      expect(imageConsultation.delete.bind(imageConsultation)).toThrow();
    });
  });


  // --------------------

  describe('ImageConsultationCollection', function() {

    let nbConsultations = 3;
    let totalNb = 0;
    let user;

    beforeAll(async function() {
      ({id: user} = await User.fetchCurrent());

      let consultationsPromise = [];
      for(let i = 0; i < nbConsultations; i++) {
        consultationsPromise.push(new ImageConsultation({image}).save());
      }
      consultationsPromise = await Promise.all(consultationsPromise);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ImageConsultationCollection({user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toBeGreaterThanOrEqual(nbConsultations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ImageConsultationCollection.fetchAll({user, project});
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch with several requests', async function() {
        let collection = await ImageConsultationCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch from project connection', async function() { // erratic core behaviour
        let collection = await new ImageConsultationCollection({projectConnection}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toBeGreaterThanOrEqual(nbConsultations);
      });

      it('Fetch resume', async function() {
        let collection = await new ImageConsultationCollection({resume: true, user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toBeGreaterThanOrEqual(1);
      });

      it('Fetch with incorrect parameters', async function() {
        let collection = new ImageConsultationCollection();
        expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ImageConsultationCollection.fetchAll({user, project});
        for(let connection of collection) {
          expect(connection).toBeInstanceOf(ImageConsultation);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ImageConsultationCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageConsultation());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ImageConsultationCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });

      it('Download URL', async function() {
        let collection = new ImageConsultationCollection({user, project});
        expect(collection.downloadURL).toBe('string');
      });
    });


    describe.skip('Pagination', function() { // incorrect values returned for size and totalPages
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

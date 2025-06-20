import * as utils from './utils.js';
import {ProjectConnection, ImageConsultation, ImageConsultationCollection, User} from '@/index.js';

describe('ImageConsultation', () => {
  let project;
  let image;
  let projectConnection;

  let imageConsultation = null;

  beforeAll(async () => {
    await utils.connect();
    ({id: project} = await utils.getProject());
    ({id: projectConnection} = await new ProjectConnection({project}).save());
    ({id: image} = await utils.getImageInstance({project}));
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      imageConsultation = new ImageConsultation({image});
      await imageConsultation.save();
      expect(imageConsultation).toBeInstanceOf(ImageConsultation);
      expect(imageConsultation.id).toBeGreaterThan(0);
    });
  });

  describe('Fetch', () => {
    it('Fetch', () => {
      expect(imageConsultation.fetch.bind(imageConsultation)).toThrow();
    });
  });

  describe('Update', () => {
    it('Update', () => {
      expect(imageConsultation.update.bind(imageConsultation)).toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', () => {
      expect(imageConsultation.delete.bind(imageConsultation)).toThrow();
    });
  });


  // --------------------

  describe('ImageConsultationCollection', () => {

    let nbConsultations = 3;
    let totalNb = 0;
    let user;

    beforeAll(async () => {
      ({id: user} = await User.fetchCurrent());

      let consultationsPromise = [];
      for (let i = 0; i < nbConsultations; i++) {
        consultationsPromise.push(new ImageConsultation({image}).save());
      }
      consultationsPromise = await Promise.all(consultationsPromise);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ImageConsultationCollection({user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbConsultations);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await ImageConsultationCollection.fetchAll({user, project});
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch with several requests', async () => {
        let collection = await ImageConsultationCollection.fetchAll({user, project, nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it.skip('Fetch from project connection', async () => { // erratic core behaviour
        let collection = await new ImageConsultationCollection({projectConnection}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbConsultations);
      });

      it('Fetch resume', async () => {
        let collection = await new ImageConsultationCollection({resume: true, user, project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageConsultationCollection);
        expect(collection.length).toBeGreaterThanOrEqual(1);
      });

      it('Fetch with incorrect parameters', async () => {
        let collection = new ImageConsultationCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ImageConsultationCollection.fetchAll({user, project});
        for (let connection of collection) {
          expect(connection).toBeInstanceOf(ImageConsultation);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ImageConsultationCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageConsultation());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ImageConsultationCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });

      it('Download URL', async () => {
        let collection = new ImageConsultationCollection({user, project});
        expect(typeof collection.downloadURL).toBe('string');
      });
    });


    describe.skip('Pagination', () => { // incorrect values returned for size and totalPages
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ImageConsultationCollection({user, project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });
  });
});

import * as utils from './utils.js';
import {ImageFilterProject, ImageFilterProjectCollection} from '@/index.js';

// Skipped tests
describe.skip('ImageFilterProject', function () {
  let id;
  let imageFilter;
  let imageFilterName;
  let imageFilterProject;
  let project;

  beforeAll(async function () {
    await utils.connect(true);
    ({id: project} = await utils.getProject());
    ({id: imageFilter, name: imageFilterName} = await utils.getImageFilter());
  });

  afterAll(async function () {
    await utils.cleanData();
  });

  describe('Create', function () {
    it('Create', async function () {
      imageFilterProject = new ImageFilterProject({imageFilter, project});
      imageFilterProject = await imageFilterProject.save();
      id = imageFilterProject.id;
      expect(imageFilterProject).toBeInstanceOf(ImageFilterProject);
      expect(imageFilterProject.name).toEqual(imageFilterName);
    });
  });

  describe('Delete', function () {
    it('Delete', async function () {
      await ImageFilterProject.delete(id);
    });

    it('Fetch deleted', async function () {
      await expect(ImageFilterProject.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ImageFilterProjectCollection', function () {

    let nbImageFilterProjects = 3;
    let imageFilterProjects;
    let totalNb = 0;

    beforeAll(async function () {
      async function createImageFilterAndImageFilterProject() {
        let tempFilter = await utils.getImageFilter();
        let imageFilterProject = new ImageFilterProject({project, imageFilter: tempFilter.id});
        await imageFilterProject.save();
        return imageFilterProject;
      }

      let imageFilterProjectPromises = [];
      for (let i = 0; i < nbImageFilterProjects; i++) {
        imageFilterProjectPromises.push(createImageFilterAndImageFilterProject());
      }
      imageFilterProjects = await Promise.all(imageFilterProjectPromises);
    });

    afterAll(async function () {
      let deletionPromises = imageFilterProjects.map(imageFilterProject => ImageFilterProject.delete(imageFilterProject.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function () {
      it('Fetch (instance method)', async function () {
        let collection = await new ImageFilterProjectCollection().fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterProjectCollection);
        expect(collection).toBeGreaterThanOrEqual(nbImageFilterProjects);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function () {
        let collection = await ImageFilterProjectCollection.fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function () {
        let collection = await ImageFilterProjectCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(ImageFilterProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with project filter', async function () {
        let collection = await new ImageFilterProjectCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageFilterProjectCollection);
        expect(collection).toHaveLength(nbImageFilterProjects);
      });
    });

    describe('Working with the collection', function () {
      it('Iterate through', async function () {
        let collection = await ImageFilterProjectCollection.fetchAll();
        for (let imageFilterProject of collection) {
          expect(imageFilterProject).toBeInstanceOf(ImageFilterProject);
        }
      });

      it('Add item to the collection', function () {
        let collection = new ImageFilterProjectCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageFilterProject());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function () {
        let collection = new ImageFilterProjectCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function () {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function () {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function () {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function () {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

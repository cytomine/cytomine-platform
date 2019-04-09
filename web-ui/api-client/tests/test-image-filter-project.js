import * as utils from './utils.js';
import {ImageFilterProject, ImageFilterProjectCollection} from '@';

describe('ImageFilterProject', function() {

  let imageFilter;
  let project;
  let imageFilterName;

  let imageFilterProject;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({id: project} = await utils.getProject());
    ({id: imageFilter, name: imageFilterName} = await utils.getImageFilter());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      imageFilterProject = new ImageFilterProject({imageFilter, project});
      imageFilterProject = await imageFilterProject.save();
      id = imageFilterProject.id;
      expect(imageFilterProject).to.be.an.instanceof(ImageFilterProject);
      expect(imageFilterProject.name).to.equal(imageFilterName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ImageFilterProject.delete(id);
    });

    it('Fetch deleted', function() {
      expect(ImageFilterProject.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('ImageFilterProjectCollection', function() {

    let nbImageFilterProjects = 3;
    let imageFilterProjects;
    let totalNb = 0;

    before(async function() {
      async function createImageFilterAndImageFilterProject() {
        let tempFilter = await utils.getImageFilter();
        let imageFilterProject = new ImageFilterProject({project, imageFilter: tempFilter.id});
        await imageFilterProject.save();
        return imageFilterProject;
      }

      let imageFilterProjectPromises = [];
      for(let i = 0; i < nbImageFilterProjects; i++) {
        imageFilterProjectPromises.push(createImageFilterAndImageFilterProject());
      }
      imageFilterProjects = await Promise.all(imageFilterProjectPromises);
    });

    after(async function() {
      let deletionPromises = imageFilterProjects.map(imageFilterProject => ImageFilterProject.delete(imageFilterProject.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ImageFilterProjectCollection().fetchAll();
        expect(collection).to.be.an.instanceof(ImageFilterProjectCollection);
        expect(collection).to.have.lengthOf.at.least(nbImageFilterProjects);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ImageFilterProjectCollection.fetchAll();
        expect(collection).to.be.an.instanceof(ImageFilterProjectCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ImageFilterProjectCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(ImageFilterProjectCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with project filter', async function() {
        let collection = await new ImageFilterProjectCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).to.be.an.instanceof(ImageFilterProjectCollection);
        expect(collection).to.have.lengthOf(nbImageFilterProjects);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ImageFilterProjectCollection.fetchAll();
        for(let imageFilterProject of collection) {
          expect(imageFilterProject).to.be.an.instanceof(ImageFilterProject);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ImageFilterProjectCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new ImageFilterProject());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ImageFilterProjectCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ImageFilterProjectCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

import * as utils from './utils.js';
import {ProjectDefaultLayer, ProjectDefaultLayerCollection} from '@';

describe('ProjectDefaultLayer', function() {

  let user;
  let project;
  let projectObject;

  let projectDefaultLayer;
  let id = 0;

  beforeAll(async function() {
    await utils.connect(true);
    projectObject = await utils.getProject();
    project = projectObject.id;
    ({id: user} = await utils.getUser());
    await projectObject.addUser(user);
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      projectDefaultLayer = new ProjectDefaultLayer({user, project});
      projectDefaultLayer = await projectDefaultLayer.save();
      expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      id = projectDefaultLayer.id;
      expect(id).to.exist;
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedProjectDefaultLayer = await ProjectDefaultLayer.fetch(id, project);
      expect(fetchedProjectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(fetchedProjectDefaultLayer).to.deep.equal(projectDefaultLayer);
    });

    it('Fetch with instance method', async function() {
      let fetchedProjectDefaultLayer = await new ProjectDefaultLayer({project, id}).fetch();
      expect(fetchedProjectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(fetchedProjectDefaultLayer).to.deep.equal(projectDefaultLayer);
    });

    it('Fetch with wrong ID', function() {
      expect(ProjectDefaultLayer.fetch(0)).rejects..toThrow();
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let hideByDefault = !projectDefaultLayer.hideByDefault;
      projectDefaultLayer.hideByDefault = hideByDefault;
      projectDefaultLayer = await projectDefaultLayer.update();
      expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(projectDefaultLayer.hideByDefault).to.equal(hideByDefault);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ProjectDefaultLayer.delete(id, project);
    });

    it('Fetch deleted', function() {
      expect(ProjectDefaultLayer.fetch(id, project)).rejects..toThrow();
    });
  });

  // --------------------

  describe('ProjectDefaultLayerCollection', function() {

    let nbProjectDefaultLayers = 3;
    let projectDefaultLayers;

    beforeAll(async function() {
      async function createUserAndProjectDefaultLayer() {
        let tempUser = await utils.getUser();
        let projectDefaultLayer = new ProjectDefaultLayer({project, user: tempUser.id});
        await projectObject.addUser(tempUser.id);
        await projectDefaultLayer.save();
        return projectDefaultLayer;
      }

      let projectDefaultLayerPromises = [];
      for(let i = 0; i < nbProjectDefaultLayers; i++) {
        projectDefaultLayerPromises.push(createUserAndProjectDefaultLayer());
      }
      projectDefaultLayers = await Promise.all(projectDefaultLayerPromises);
    });

    afterAll(async function() {
      let deletionPromises = projectDefaultLayers.map(projectDefaultLayer =>
        ProjectDefaultLayer.delete(projectDefaultLayer.id, project));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ProjectDefaultLayerCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch (static method)', async function() {
        let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch with several requests', async function() {
        let collection = await ProjectDefaultLayerCollection.fetchAll({nbPerPage: 1,
          filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch without filter', async function() {
        let collection = new ProjectDefaultLayerCollection();
        expect(collection.fetchAll()).rejects..toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: 'project', filterValue: project});
        for(let projectDefaultLayer of collection) {
          expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ProjectDefaultLayerCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ProjectDefaultLayer());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ProjectDefaultLayerCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

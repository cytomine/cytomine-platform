import * as utils from './utils.js';
import {ProjectDefaultLayer, ProjectDefaultLayerCollection} from '@/index.js';

describe('ProjectDefaultLayer', () => {

  let user;
  let project;
  let projectObject;

  let projectDefaultLayer;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    projectObject = await utils.getProject();
    project = projectObject.id;
    ({id: user} = await utils.getUser());
    await projectObject.addUser(user);
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      projectDefaultLayer = new ProjectDefaultLayer({user, project});
      projectDefaultLayer = await projectDefaultLayer.save();
      expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      id = projectDefaultLayer.id;
      expect(id).toBeDefined();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedProjectDefaultLayer = await ProjectDefaultLayer.fetch(id, project);
      expect(fetchedProjectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(fetchedProjectDefaultLayer).toEqual(projectDefaultLayer);
    });

    it('Fetch with instance method', async () => {
      let fetchedProjectDefaultLayer = await new ProjectDefaultLayer({project, id}).fetch();
      expect(fetchedProjectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(fetchedProjectDefaultLayer).toEqual(projectDefaultLayer);
    });

    it('Fetch with wrong ID', async () => {
      await expect(ProjectDefaultLayer.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let hideByDefault = !projectDefaultLayer.hideByDefault;
      projectDefaultLayer.hideByDefault = hideByDefault;
      projectDefaultLayer = await projectDefaultLayer.update();
      expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
      expect(projectDefaultLayer.hideByDefault).toEqual(hideByDefault);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await ProjectDefaultLayer.delete(id, project);
    });

    it('Fetch deleted', async () => {
      await expect(ProjectDefaultLayer.fetch(id, project)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ProjectDefaultLayerCollection', () => {

    let nbProjectDefaultLayers = 3;
    let projectDefaultLayers;

    beforeAll(async () => {
      async function createUserAndProjectDefaultLayer() {
        let tempUser = await utils.getUser();
        let projectDefaultLayer = new ProjectDefaultLayer({project, user: tempUser.id});
        await projectObject.addUser(tempUser.id);
        await projectDefaultLayer.save();
        return projectDefaultLayer;
      }

      let projectDefaultLayerPromises = [];
      for (let i = 0; i < nbProjectDefaultLayers; i++) {
        projectDefaultLayerPromises.push(createUserAndProjectDefaultLayer());
      }
      projectDefaultLayers = await Promise.all(projectDefaultLayerPromises);
    });

    afterAll(async () => {
      let deletionPromises = projectDefaultLayers.map(projectDefaultLayer =>
        ProjectDefaultLayer.delete(projectDefaultLayer.id, project));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ProjectDefaultLayerCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch (static method)', async () => {
        let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch with several requests', async () => {
        let collection = await ProjectDefaultLayerCollection.fetchAll({
          nbPerPage: 1,
          filterKey: 'project', filterValue: project
        });
        expect(collection).toBeInstanceOf(ProjectDefaultLayerCollection);
        expect(collection).toHaveLength(nbProjectDefaultLayers);
      });

      it('Fetch without filter', async () => {
        let collection = new ProjectDefaultLayerCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ProjectDefaultLayerCollection.fetchAll({filterKey: 'project', filterValue: project});
        for (let projectDefaultLayer of collection) {
          expect(projectDefaultLayer).toBeInstanceOf(ProjectDefaultLayer);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ProjectDefaultLayerCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ProjectDefaultLayer());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ProjectDefaultLayerCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ProjectDefaultLayerCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

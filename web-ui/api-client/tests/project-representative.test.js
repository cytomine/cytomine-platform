import * as utils from './utils.js';
import {ProjectRepresentative, ProjectRepresentativeCollection} from '@/index.js';

describe('ProjectRepresentative', () => {

  let user;
  let projectInstance;
  let project;

  let projectRepresentative;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    projectInstance = await utils.getProject();
    project = projectInstance.id;
    ({id: user} = await utils.getUser());
    await projectInstance.addUser(user);
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      projectRepresentative = new ProjectRepresentative({user, project});
      projectRepresentative = await projectRepresentative.save();
      expect(projectRepresentative).toBeInstanceOf(ProjectRepresentative);
      id = projectRepresentative.id;
      expect(id).toBeDefined();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedProjectRepresentative = await ProjectRepresentative.fetch(id, project);
      expect(fetchedProjectRepresentative).toBeInstanceOf(ProjectRepresentative);
      expect(fetchedProjectRepresentative).toEqual(projectRepresentative);
    });

    it('Fetch with instance method', async () => {
      let fetchedProjectRepresentative = await new ProjectRepresentative({project, id}).fetch();
      expect(fetchedProjectRepresentative).toBeInstanceOf(ProjectRepresentative);
      expect(fetchedProjectRepresentative).toEqual(projectRepresentative);
    });

    it('Fetch with wrong ID', async () => {
      await expect(ProjectRepresentative.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await ProjectRepresentative.delete(id, project);
    });

    it('Fetch deleted', async () => {
      await expect(ProjectRepresentative.fetch(id, project)).rejects.toThrow();
    });

    it('Create again', async () => {
      projectRepresentative = new ProjectRepresentative({user, project});
      projectRepresentative = await projectRepresentative.save();
      id = projectRepresentative.id;
    });

    it('Delete with project and user', async () => {
      await ProjectRepresentative.delete(0, project, user);
      await expect(ProjectRepresentative.fetch(id, project)).rejects.toThrow();
    });

  });

  // --------------------

  describe('ProjectRepresentativeCollection', () => {

    let nbProjectRepresentatives = 3;
    let projectRepresentatives;

    beforeAll(async () => {
      async function createUserAndProjectRepresentative() {
        let {id: tempUser} = await utils.getUser();
        await projectInstance.addUser(tempUser);
        let projectRepresentative = new ProjectRepresentative({project, user: tempUser});
        await projectRepresentative.save();
        return projectRepresentative;
      }

      let projectRepresentativePromises = [];
      for (let i = 0; i < nbProjectRepresentatives; i++) {
        projectRepresentativePromises.push(createUserAndProjectRepresentative());
      }
      projectRepresentatives = await Promise.all(projectRepresentativePromises);
    });

    afterAll(async () => {
      let deletionPromises = projectRepresentatives.map(projectRepresentative =>
        ProjectRepresentative.delete(projectRepresentative.id, project));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ProjectRepresentativeCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ProjectRepresentativeCollection);
        expect(collection).toHaveLength(nbProjectRepresentatives + 1); // creator is by default representative
      });

      it('Fetch (static method)', async () => {
        let collection = await ProjectRepresentativeCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ProjectRepresentativeCollection);
        expect(collection).toHaveLength(nbProjectRepresentatives + 1); // creator is by default representative
      });

      it('Fetch with several requests', async () => {
        let collection = await ProjectRepresentativeCollection.fetchAll({
          nbPerPage: 1,
          filterKey: 'project', filterValue: project
        });
        expect(collection).toBeInstanceOf(ProjectRepresentativeCollection);
        expect(collection).toHaveLength(nbProjectRepresentatives + 1);
      });

      it('Fetch without filter', async () => {
        let collection = new ProjectRepresentativeCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ProjectRepresentativeCollection.fetchAll({filterKey: 'project', filterValue: project});
        for (let projectRepresentative of collection) {
          expect(projectRepresentative).toBeInstanceOf(ProjectRepresentative);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ProjectRepresentativeCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ProjectRepresentative());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ProjectRepresentativeCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ProjectRepresentativeCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

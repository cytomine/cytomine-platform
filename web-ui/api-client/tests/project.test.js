import * as utils from './utils.js';
import { Project, ProjectCollection, ProjectConnection, User, UserCollection, AnnotationType } from '@/index.js';

describe('Project', () => {

  let ontology;
  let name = utils.randomString();

  let project = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
    ({ id: ontology } = await utils.getOntology());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      project = new Project({ name, ontology });
      project = await project.save();
      id = project.id;
      expect(project).toBeInstanceOf(Project);
      expect(project.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedProject = await Project.fetch(id);
      expect(fetchedProject).toBeInstanceOf(Project);
      expect(fetchedProject.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedProject = await new Project({ id }).fetch();
      expect(fetchedProject).toBeInstanceOf(Project);
      expect(fetchedProject.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Project.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', () => {
    let nbUsers;
    let nbAdmins;
    let idUser;
    let uiConfig;

    beforeAll(async () => {
      ({ id: idUser } = await utils.getUser());
    });

    it('Fetch creator', async () => {
      let user = await project.fetchCreator();
      expect(user).toBeInstanceOf(User);
      let currentUser = await User.fetchCurrent();
      expect(user.id).toEqual(currentUser.id);
    });

    it('Fetch users', async () => {
      let users = await project.fetchUsers();
      expect(users).toBeInstanceOf(UserCollection);
      nbUsers = users.length;
    });

    it('Fetch users activity', async () => {
      let users = await project.fetchUsersActivity();
      expect(users).toBeInstanceOf(UserCollection);
    });

    it('Fetch connected users', async () => {
      let users = await project.fetchConnectedUsers();
      expect(users).toBeInstanceOf(Array);
    });

    it('Fetch user layers', async () => {
      let userLayers = await project.fetchUserLayers();
      expect(userLayers).toBeInstanceOf(UserCollection);
    });

    it('Fetch admins', async () => {
      let admins = await project.fetchAdministrators();
      expect(admins).toBeInstanceOf(UserCollection);
      nbAdmins = admins.length;
    });

    it('Fetch representatives', async () => {
      let representatives = await project.fetchRepresentatives();
      expect(representatives).toBeInstanceOf(UserCollection);
    });

    it('Add user', async () => {
      await project.addUser(idUser);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers + 1);
    });

    it('Delete user', async () => {
      await project.deleteUser(idUser);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers);
    });

    it('Add users', async () => {
      await project.addUsers([idUser]);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers + 1);
    });

    it('Delete users', async () => {
      await project.deleteUsers([idUser]);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers);
    });

    it('Add admin', async () => {
      await project.addAdmin(idUser);
      let admins = await project.fetchAdministrators();
      expect(admins.length).toEqual(nbAdmins + 1);
    });

    it('Delete admin', async () => {
      await project.deleteAdmin(idUser);
      let admins = await project.fetchAdministrators();
      expect(admins.length).toEqual(nbAdmins);
    });

    it('Fetch UI config', async () => {
      uiConfig = await project.fetchUIConfig();
      expect(uiConfig).toBeInstanceOf(Object);
      for (let prop in uiConfig) {
        expect(typeof uiConfig[prop]['CONTRIBUTOR_PROJECT']).toBe('boolean');
        expect(typeof uiConfig[prop]['ADMIN_PROJECT']).toBe('boolean');
      }
    });

    it('Set UI config', async () => {
      uiConfig['project-images-tab']['ADMIN_PROJECT'] = false;
      let result = await project.saveUIConfig(uiConfig);
      expect(result).toEqual(uiConfig);
    });

    it('Fetch command history', async () => {
      let result = await project.fetchCommandHistory({ max: 10 });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of connections', async () => {
      let result = await project.fetchConnectionsEvolution({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of image consultations', async () => {
      let result = await project.fetchImageConsultationsEvolution({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of annotation actions', async () => {
      let result = await project.fetchAnnotationActionsEvolution({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of annotation', async () => {
      let result = await project.fetchAnnotationActionsEvolution({ startDate: new Date().getTime(), annotationType: AnnotationType.ALGO });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch number of connections', async () => {
      let result = await project.fetchNbConnections({ startDate: new Date().getTime() });
      expect(typeof result).toBe('number');
    });

    it('Fetch number of image consultations', async () => {
      let result = await project.fetchNbImageConsultations({ startDate: new Date().getTime() });
      expect(typeof result).toBe('number');
    });

    it('Fetch number of annotation actions', async () => {
      let result = await project.fetchNbAnnotationActions({ startDate: new Date().getTime() });
      expect(typeof result).toBe('number');
    });

    it('Fetch number of annotations', async () => {
      let result = await project.fetchNbAnnotations({ startDate: new Date().getTime(), annotationType: AnnotationType.USER });
      expect(typeof result).toBe('number');
    });

    it('Fetch terms statistics', async () => {
      let result = await project.fetchStatsTerms({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch annotated images by terms statistics', async () => {
      let result = await project.fetchStatsAnnotatedImagesByTerm({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch contributors statistics', async () => {
      let result = await project.fetchStatsAnnotationCreators({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch annotated images by contributor statistics', async () => {
      let result = await project.fetchStatsAnnotatedImagesByCreator({ startDate: new Date().getTime() });
      expect(result).toBeInstanceOf(Array);
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      project.name = newName;
      project = await project.update();
      expect(project).toBeInstanceOf(Project);
      expect(project.name).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Project.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Project.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ProjectCollection', () => {

    let nbProjects = 3;
    let projects;
    let totalNb = 0;

    let currentUser;

    beforeAll(async () => {

      currentUser = await User.fetchCurrent();

      async function createAndAccessProject() {
        let project = new Project({ name: utils.randomString(), ontology });
        await project.save();
        await new ProjectConnection({ project: project.id }).save();
        return project;
      }

      let projectPromises = [];
      for (let i = 0; i < nbProjects; i++) {
        projectPromises.push(createAndAccessProject());
      }
      projects = await Promise.all(projectPromises);
    });

    afterAll(async () => {
      let deletionPromises = projects.map(project => Project.delete(project.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ProjectCollection().fetchAll();
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbProjects);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await ProjectCollection.fetchAll();
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await ProjectCollection.fetchAll({ nbPerPage: Math.ceil(totalNb / 3) });
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch last opened projects', async () => {
        let collection = await ProjectCollection.fetchLastOpened(nbProjects);
        expect(collection).toHaveLength(nbProjects);
        let listId = collection.map(project => project.id);
        projects.forEach(project => {
          expect(listId).toContain(project.id);
        });
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ProjectCollection.fetchAll();
        for (let project of collection) {
          expect(project).toBeInstanceOf(Project);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ProjectCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Project());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ProjectCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', () => {
      it('Filter on user', async () => {
        let collection = await ProjectCollection.fetchAll({ filterKey: 'user', filterValue: currentUser.id });
        expect(collection.length).toBeGreaterThanOrEqual(nbProjects);
      });

      it('Filter on user ; light version', async () => {
        let collection = await ProjectCollection.fetchAll({ filterKey: 'user', filterValue: currentUser.id, light: true });
        expect(collection.length).toBeGreaterThanOrEqual(nbProjects);
      });

      it('Filter on ontology', async () => {
        let collection = new ProjectCollection({ filterKey: 'ontology', filterValue: ontology });
        await collection.fetchAll();
        expect(collection.length).toBeGreaterThanOrEqual(nbProjects);
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ProjectCollection({ nbPerPage });
        await collection.fetchPage(1);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ProjectCollection({ nbPerPage });
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ProjectCollection({ nbPerPage });
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

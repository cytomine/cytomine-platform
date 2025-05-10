import * as utils from './utils.js';
import {Project, ProjectCollection, ProjectConnection, User, UserCollection, AnnotationType} from '@/index.js';

describe('Project', function() {

  let ontology;
  let name = utils.randomString();

  let project = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
    ({id: ontology} = await utils.getOntology());
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      project = new Project({name, ontology});
      project = await project.save();
      id = project.id;
      expect(project).toBeInstanceOf(Project);
      expect(project.name).toEqual(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedProject = await Project.fetch(id);
      expect(fetchedProject).toBeInstanceOf(Project);
      expect(fetchedProject.name).toEqual(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedProject = await new Project({id}).fetch();
      expect(fetchedProject).toBeInstanceOf(Project);
      expect(fetchedProject.name).toEqual(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Project.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', function() {
    let nbUsers;
    let nbAdmins;
    let idUser;
    let uiConfig;

    beforeAll(async function() {
      ({id: idUser} = await utils.getUser());
    });

    it('Fetch creator', async function() {
      let user = await project.fetchCreator();
      expect(user).toBeInstanceOf(User);
      let currentUser = await User.fetchCurrent();
      expect(user.id).toEqual(currentUser.id);
    });

    it('Fetch users', async function() {
      let users = await project.fetchUsers();
      expect(users).toBeInstanceOf(UserCollection);
      nbUsers = users.length;
    });

    it('Fetch users activity', async function() {
      let users = await project.fetchUsersActivity();
      expect(users).toBeInstanceOf(UserCollection);
    });

    it('Fetch connected users', async function() {
      let users = await project.fetchConnectedUsers();
      expect(users).toBeInstanceOf(Array);
    });

    it('Fetch user layers', async function() {
      let userLayers = await project.fetchUserLayers();
      expect(userLayers).toBeInstanceOf(UserCollection);
    });

    it('Fetch admins', async function() {
      let admins = await project.fetchAdministrators();
      expect(admins).toBeInstanceOf(UserCollection);
      nbAdmins = admins.length;
    });

    it('Fetch representatives', async function() {
      let representatives = await project.fetchRepresentatives();
      expect(representatives).toBeInstanceOf(UserCollection);
    });

    it('Add user', async function() {
      await project.addUser(idUser);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers + 1);
    });

    it('Delete user', async function() {
      await project.deleteUser(idUser);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers);
    });

    it('Add users', async function() {
      await project.addUsers([idUser]);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers + 1);
    });

    it('Delete users', async function() {
      await project.deleteUsers([idUser]);
      let users = await project.fetchUsers();
      expect(users.length).toEqual(nbUsers);
    });

    it('Add admin', async function() {
      await project.addAdmin(idUser);
      let admins = await project.fetchAdministrators();
      expect(admins.length).toEqual(nbAdmins + 1);
    });

    it('Delete admin', async function() {
      await project.deleteAdmin(idUser);
      let admins = await project.fetchAdministrators();
      expect(admins.length).toEqual(nbAdmins);
    });

    it('Fetch UI config', async function() {
      uiConfig = await project.fetchUIConfig();
      expect(uiConfig).toBeInstanceOf(Object);
      for(let prop in uiConfig){
        expect(uiConfig[prop]['CONTRIBUTOR_PROJECT']).toBe('boolean');
        expect(uiConfig[prop]['ADMIN_PROJECT']).toBe('boolean');
      }
    });

    it('Set UI config', async function() {
      uiConfig['project-images-tab']['ADMIN_PROJECT'] = false;
      let result = await project.saveUIConfig(uiConfig);
      expect(result).toEqual(uiConfig);
    });

    it('Fetch command history', async function() {
      let result = await project.fetchCommandHistory({max: 10});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of connections', async function() {
      let result = await project.fetchConnectionsEvolution({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of image consultations', async function() {
      let result = await project.fetchImageConsultationsEvolution({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of annotation actions', async function() {
      let result = await project.fetchAnnotationActionsEvolution({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch evolution of annotation', async function() {
      let result = await project.fetchAnnotationActionsEvolution({startDate: new Date().getTime(), annotationType: AnnotationType.ALGO});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch number of connections', async function() {
      let result = await project.fetchNbConnections({startDate: new Date().getTime()});
      expect(result).toBe('number');
    });

    it('Fetch number of image consultations', async function() {
      let result = await project.fetchNbImageConsultations({startDate: new Date().getTime()});
      expect(result).toBe('number');
    });

    it('Fetch number of annotation actions', async function() {
      let result = await project.fetchNbAnnotationActions({startDate: new Date().getTime()});
      expect(result).toBe('number');
    });

    it('Fetch number of annotations', async function() {
      let result = await project.fetchNbAnnotations({startDate: new Date().getTime(), annotationType: AnnotationType.USER});
      expect(result).toBe('number');
    });

    it('Fetch terms statistics', async function() {
      let result = await project.fetchStatsTerms({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch annotated images by terms statistics', async function() {
      let result = await project.fetchStatsAnnotatedImagesByTerm({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch contributors statistics', async function() {
      let result = await project.fetchStatsAnnotationCreators({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });

    it('Fetch annotated images by contributor statistics', async function() {
      let result = await project.fetchStatsAnnotatedImagesByCreator({startDate: new Date().getTime()});
      expect(result).toBeInstanceOf(Array);
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      project.name = newName;
      project = await project.update();
      expect(project).toBeInstanceOf(Project);
      expect(project.name).toEqual(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Project.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Project.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ProjectCollection', function() {

    let nbProjects = 3;
    let projects;
    let totalNb = 0;

    let currentUser;

    beforeAll(async function() {

      currentUser = await User.fetchCurrent();

      async function createAndAccessProject() {
        let project = new Project({name: utils.randomString(), ontology});
        await project.save();
        await new ProjectConnection({project: project.id}).save();
        return project;
      }

      let projectPromises = [];
      for(let i = 0; i < nbProjects; i++) {
        projectPromises.push(createAndAccessProject());
      }
      projects = await Promise.all(projectPromises);
    });

    afterAll(async function() {
      let deletionPromises = projects.map(project => Project.delete(project.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ProjectCollection().fetchAll();
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection).toBeGreaterThanOrEqual(nbProjects);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await ProjectCollection.fetchAll();
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await ProjectCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(ProjectCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch last opened projects', async function() {
        let collection = await ProjectCollection.fetchLastOpened(nbProjects);
        expect(collection).toHaveLength(nbProjects);
        let listId = collection.map(project => project.id);
        projects.forEach(project => {
          expect(listId).toContain(project.id);
        });
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ProjectCollection.fetchAll();
        for(let project of collection) {
          expect(project).toBeInstanceOf(Project);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ProjectCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Project());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ProjectCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', function() {
      it('Filter on user', async function() {
        let collection = await ProjectCollection.fetchAll({filterKey: 'user', filterValue: currentUser.id});
        expect(collection).toBeGreaterThanOrEqual(nbProjects);
      });

      it('Filter on user ; light version', async function() {
        let collection = await ProjectCollection.fetchAll({filterKey: 'user', filterValue: currentUser.id, light: true});
        expect(collection).toBeGreaterThanOrEqual(nbProjects);
      });

      it('Filter on ontology', async function() {
        let collection = new ProjectCollection({filterKey: 'ontology', filterValue: ontology});
        await collection.fetchAll();
        expect(collection).toBeGreaterThanOrEqual(nbProjects);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ProjectCollection({nbPerPage});
        await collection.fetchPage(1);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ProjectCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ProjectCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

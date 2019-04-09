import * as utils from './utils.js';
import {UserRole, UserRoleCollection} from '@';

describe('UserRole', function() {

  let role;
  let user;

  let userRole;

  before(async function() {
    await utils.connect(true);
    ({id: user} = await utils.getUser());
    ({id: role} = await utils.getRole());

    // clean the roles automatically associated to the user during its creation (otherwise, duplicate errors)
    let collection = await UserRoleCollection.fetchAll({filterKey: 'user', filterValue: user});
    for(let userRole of collection) {
      await userRole.delete();
    }
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      userRole = new UserRole({role, user});
      userRole = await userRole.save();
      expect(userRole).to.be.an.instanceof(UserRole);
      expect(userRole.id).to.exist;
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedUserRole = await UserRole.fetch(user, role);
      expect(fetchedUserRole).to.be.an.instanceof(UserRole);
      expect(fetchedUserRole).to.deep.equal(userRole);
    });

    it('Fetch with instance method', async function() {
      let fetchedUserRole = await new UserRole({user, role}).fetch();
      expect(fetchedUserRole).to.be.an.instanceof(UserRole);
      expect(fetchedUserRole).to.deep.equal(userRole);
    });

    it('Fetch with wrong ID', function() {
      expect(UserRole.fetch(0)).to.be.rejected;
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await UserRole.delete(user, role);
    });

    it('Fetch deleted', function() {
      expect(UserRole.fetch(user, role)).to.be.rejected;
    });
  });

  // --------------------

  describe('UserRoleCollection', function() {

    let nbUserRoles = 3;
    let userRoles;

    before(async function() {
      let roles = await utils.getMultipleRoles(nbUserRoles);
      let userRolePromises = roles.map(role => new UserRole({role, user}).save());
      userRoles = await Promise.all(userRolePromises);
    });

    after(async function() {
      let deletionPromises = userRoles.map(userRole => UserRole.delete(userRole.user, userRole.role));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new UserRoleCollection({filterKey: 'user', filterValue: user}).fetchAll();
        expect(collection).to.be.an.instanceof(UserRoleCollection);
        expect(collection).to.have.lengthOf(nbUserRoles);
      });

      it('Fetch (static method)', async function() {
        let collection = await UserRoleCollection.fetchAll({filterKey: 'user', filterValue: user});
        expect(collection).to.be.an.instanceof(UserRoleCollection);
        expect(collection).to.have.lengthOf(nbUserRoles);
      });

      it('Fetch with several requests', async function() {
        let collection = await UserRoleCollection.fetchAll({nbPerPage: 1,
          filterKey: 'user', filterValue: user});
        expect(collection).to.be.an.instanceof(UserRoleCollection);
        expect(collection).to.have.lengthOf(nbUserRoles);
      });

      it('Fetch without filter', async function() {
        let collection = new UserRoleCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await UserRoleCollection.fetchAll({filterKey: 'user', filterValue: user});
        for(let userRole of collection) {
          expect(userRole).to.be.an.instanceof(UserRole);
        }
      });

      it('Add item to the collection', function() {
        let collection = new UserRoleCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new UserRole());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new UserRoleCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new UserRoleCollection({nbPerPage, filterKey: 'user', filterValue: user});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new UserRoleCollection({nbPerPage, filterKey: 'user', filterValue: user});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new UserRoleCollection({nbPerPage, filterKey: 'user', filterValue: user});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

import * as utils from './utils.js';
import {UserJob, UserJobCollection, Job} from '@';

describe.skip('UserJob', function() {

  let software;
  let project;

  let userJob = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({project, software} = await utils.getSoftwareProject());
  });

  after(async function() {
    if(userJob) {
      await Job.delete(userJob.job);
    }
    await utils.cleanData();
  });

  describe('Create', function() {
    it.skip('Create', async function() {
      userJob = new UserJob();
      userJob = await userJob.save(software, project);
      id = userJob.id;
      expect(userJob).to.be.an.instanceof(UserJob);
      expect(userJob.job).to.exist;
    });
  });

  describe('Fetch', function() {
    it.skip('Fetch with static method', async function() {
      let fetchedUserJob = await UserJob.fetch(id);
      expect(fetchedUserJob).to.be.an.instanceof(UserJob);
      expect(fetchedUserJob).to.deep.equal(userJob);
    });

    it.skip('Fetch with instance method', async function() {
      let fetchedUserJob = await new UserJob({id}).fetch();
      expect(fetchedUserJob).to.be.an.instanceof(UserJob);
      expect(fetchedUserJob).to.deep.equal(userJob);
    });

    it.skip('Fetch with wrong ID', function() {
      expect(UserJob.fetch(0)).to.be.rejected;
    });
  });

  describe('Delete', function() {
    it.skip('Delete', async function() {
      await UserJob.delete(id);
    });

    it.skip('Fetch deleted', function() {
      expect(UserJob.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('UserJobCollection', function() {

    let nbUserJobs = 3;
    let userJobs;
    let totalNb = 0;

    before(async function() {
      userJobs = [];
      // create user jobs sequentially to avoid having the same username for several of them (happens if several
      // user jobs associated to same software are created during the same second)
      for(let i = 0; i < nbUserJobs; i++) {
        if(i > 0) {
          await utils.wait(1000);
        }
        let userJob = await new UserJob().save(software, project);
        userJobs.push(userJob);
      }
    });

    after(async function() {
      let deletionPromises = userJobs.map(userJob => UserJob.delete(userJob.id));
      await Promise.all(deletionPromises);
      let jobDeletionPromises = userJobs.map(userJob => Job.delete(userJob.job));
      await Promise.all(jobDeletionPromises);
    });

    describe('Fetch', function() {
      it.skip('Fetch (instance method)', async function() {
        let collection = await new UserJobCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).to.be.an.instanceof(UserJobCollection);
        expect(collection).to.have.lengthOf.at.least(nbUserJobs);
        totalNb = collection.length;
      });

      it.skip('Fetch (static method)', async function() {
        let collection = await UserJobCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(UserJobCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it.skip('Fetch with several requests', async function() {
        let collection = await UserJobCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3),
          filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(UserJobCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it.skip('Fetch without filter', async function() {
        let collection = new UserJobCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });
    });

    describe('Working with the collection', function() {
      it.skip('Iterate through', async function() {
        let collection = await UserJobCollection.fetchAll({filterKey: 'project', filterValue: project});
        for(let userJob of collection) {
          expect(userJob).to.be.an.instanceof(UserJob);
        }
      });

      it.skip('Add item to the collection', function() {
        let collection = new UserJobCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new UserJob());
        expect(collection).to.have.lengthOf(1);
      });

      it.skip('Add arbitrary object to the collection', function() {
        let collection = new UserJobCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it.skip('Fetch arbitrary page', async function() {
        let collection = new UserJobCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it.skip('Fetch next page', async function() {
        let collection = new UserJobCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it.skip('Fetch previous page', async function() {
        let collection = new UserJobCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

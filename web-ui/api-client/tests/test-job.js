import * as utils from './utils.js';
import {Job, JobStatus, JobCollection, JobParameter} from '@';

describe('Job', function() {

  let software;
  let softwareParameter;
  let project;

  let job;
  let id = 0;

  before(async function() {
    await utils.connect();
    ({project, software} = await utils.getSoftwareProject());
    ({id: softwareParameter} = await utils.getSoftwareParameter({software, type:'String'}));
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      job = new Job({software, project});
      job.jobParameters.push(new JobParameter({softwareParameter, value: "test"}));
      job = await job.save();
      id = job.id;
      expect(job).to.be.an.instanceof(Job);
      expect(job.status).to.equal(JobStatus.NOTLAUNCH);
      expect(job.progress).to.equal(0);
      expect(job.userJob).to.exist;
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedJob = await Job.fetch(id);
      expect(fetchedJob).to.be.an.instanceof(Job);
      expect(fetchedJob.userJob).to.equal(job.userJob);
    });

    it('Fetch with instance method', async function() {
      let fetchedJob = await new Job({id}).fetch();
      expect(fetchedJob).to.be.an.instanceof(Job);
      expect(fetchedJob.userJob).to.equal(job.userJob);
    });

    it('Fetch with wrong ID', function() {
      expect(Job.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let statusComment = utils.randomString();
      job.statusComment = statusComment;
      job = await job.update();
      expect(job).to.be.an.instanceof(Job);
      expect(job.statusComment).to.equal(statusComment);
    });
  });

  describe('Specific operations', function() {
    it('Execute', async function() {
      await job.execute();
    });

    it('Fetch all data', async function() {
      let data = await job.fetchAllData();
      expect(data.annotations).to.exist;
      expect(data.annotationsTerm).to.exist;
      expect(data.jobDatas).to.exist;
      expect(data.reviewed).to.exist;
    });

    it('Delete all data', async function() {
      await job.deleteAllData();
      expect(job.dataDeleted).to.be.true;
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Job.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Job.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('JobCollection', function() {

    let nbJobs = 4;
    let jobs;
    let totalNb = 0;

    let otherSoftware;

    before(async function() {
      ({id: otherSoftware} = await utils.getSoftware());

      let jobPromises = [];
      for(let i = 0; i < nbJobs - 1; i++) {
        jobPromises.push(new Job({software, project}).save());
      }
      jobPromises.push(new Job({software: otherSoftware, project}).save());
      jobs = await Promise.all(jobPromises);
    });

    after(async function() {
      let deletionPromises = jobs.map(job => Job.delete(job.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new JobCollection().fetchAll();
        expect(collection).to.be.an.instanceof(JobCollection);
        expect(collection).to.have.lengthOf.at.least(nbJobs);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await JobCollection.fetchAll();
        expect(collection).to.be.an.instanceof(JobCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await JobCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(JobCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with project filter', async function() {
        let collection = await new JobCollection({project}).fetchAll();
        expect(collection).to.be.an.instanceof(JobCollection);
        expect(collection).to.have.lengthOf(nbJobs);
      });

      it('Fetch with project and software filters', async function() {
        let collection = await new JobCollection({project, software}).fetchAll();
        expect(collection).to.be.an.instanceof(JobCollection);
        expect(collection).to.have.lengthOf(nbJobs - 1);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await JobCollection.fetchAll();
        for(let job of collection) {
          expect(job).to.be.an.instanceof(Job);
        }
      });

      it('Add item to the collection', function() {
        let collection = new JobCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Job());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new JobCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new JobCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new JobCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new JobCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

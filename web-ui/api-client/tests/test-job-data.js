import * as utils from './utils.js';
import {JobData, JobDataCollection} from '@';

describe('JobData', function() {

  let job;
  let key = utils.randomString();
  let filename = key + '.xml';
  let file = new Blob(['<a id="a"><b id="b">hey!</b></a>'], {type: 'text/xml'});

  let jobData = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
    ({id: job} = await utils.getJob());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      jobData = new JobData({job, filename, key});
      jobData = await jobData.save();
      id = jobData.id;
      expect(jobData).to.be.an.instanceof(JobData);
      expect(jobData.filename).to.equal(filename);
      expect(jobData.key).to.equal(key);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedJobData = await JobData.fetch(id);
      expect(fetchedJobData).to.be.an.instanceof(JobData);
      expect(fetchedJobData).to.deep.equal(jobData);
    });

    it('Fetch with instance method', async function() {
      let fetchedJobData = await new JobData({id}).fetch();
      expect(fetchedJobData).to.be.an.instanceof(JobData);
      expect(fetchedJobData).to.deep.equal(jobData);
    });

    it('Fetch with wrong ID', function() {
      return expect(JobData.fetch(0)).to.be.rejected;
    });
  });

  describe.skip('Specific operations', function() { // skipped because deletion of job data with uploaded file fails with error HibernateSystemException: instance was not in a valid state
    it('Upload file', async function() {
      await jobData.upload(file);
      expect(jobData.size).to.be.above(0);
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newKey = utils.randomString();
      jobData.key = newKey;
      jobData = await jobData.update();
      expect(jobData).to.be.an.instanceof(JobData);
      expect(jobData.key).to.equal(newKey);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await JobData.delete(id);
    });

    it('Fetch deleted', function() {
      return expect(JobData.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('JobDataCollection', function() {

    let nbJobDatas = 3;
    let jobDatas;
    let totalNb = 0;

    before(async function() {
      let jobDataPromises = [];
      for(let i = 0; i < nbJobDatas; i++) {
        let promise = new JobData({
          job,
          key: utils.randomString(),
          filename: utils.randomString() + '.xml'
        }).save();
        jobDataPromises.push(promise);
      }
      jobDatas = await Promise.all(jobDataPromises);
    });

    after(async function() {
      let deletionPromises = jobDatas.map(jobData => JobData.delete(jobData.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new JobDataCollection().fetchAll();
        expect(collection).to.be.an.instanceof(JobDataCollection);
        expect(collection).to.have.lengthOf.at.least(nbJobDatas);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await JobDataCollection.fetchAll();
        expect(collection).to.be.an.instanceof(JobDataCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await JobDataCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(JobDataCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Filtering', function() {
      it('Filter on job (static method)', async function() {
        let collection = await JobDataCollection.fetchAll({filterKey: 'job', filterValue: job});
        expect(collection).to.have.lengthOf(nbJobDatas);
      });

      it('Filter on job (instance method)', async function() {
        let collection = new JobDataCollection(0);
        collection.setFilter('job', job);
        await collection.fetchAll();
        expect(collection).to.have.lengthOf(nbJobDatas);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await JobDataCollection.fetchAll();
        for(let jobData of collection) {
          expect(jobData).to.be.an.instanceof(JobData);
        }
      });

      it('Add item to the collection', function() {
        let collection = new JobDataCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new JobData());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new JobDataCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new JobDataCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new JobDataCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new JobDataCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });


});

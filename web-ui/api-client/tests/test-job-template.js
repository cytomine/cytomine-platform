import * as utils from './utils.js';
import {JobTemplate, JobTemplateCollection} from '@';

describe('JobTemplate', function() {

  let name = utils.randomString();
  let software;
  let project;

  let jobTemplate = null;
  let id = 0;

  before(async function() {
    await utils.connect();
    ({software} = await utils.getSoftwareParameter({name: 'annotation', forceCreation: true, cascadeForceCreation: true}));
    ({id: project} = await utils.getProject());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      jobTemplate = new JobTemplate({software, project, name});
      jobTemplate = await jobTemplate.save();
      id = jobTemplate.id;
      expect(jobTemplate).to.be.an.instanceof(JobTemplate);
      expect(jobTemplate.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedJobTemplate = await JobTemplate.fetch(id);
      expect(fetchedJobTemplate).to.be.an.instanceof(JobTemplate);
      expect(fetchedJobTemplate).to.deep.equal(jobTemplate);
    });

    it('Fetch with instance method', async function() {
      let fetchedJobTemplate = await new JobTemplate({id}).fetch();
      expect(fetchedJobTemplate).to.be.an.instanceof(JobTemplate);
      expect(fetchedJobTemplate).to.deep.equal(jobTemplate);
    });

    it('Fetch with wrong ID', function() {
      expect(JobTemplate.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      jobTemplate.name = newName;
      jobTemplate = await jobTemplate.update();
      expect(jobTemplate).to.be.an.instanceof(JobTemplate);
      expect(jobTemplate.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await JobTemplate.delete(id);
    });

    it('Fetch deleted', function() {
      expect(JobTemplate.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('JobTemplateCollection', function() {

    let nbJobTemplates = 3;
    let jobTemplates;

    before(async function() {
      let jobTemplatePromises = [];
      for(let i = 0; i < nbJobTemplates; i++) {
        let promise = new JobTemplate({software, project, name: utils.randomString()}).save();
        jobTemplatePromises.push(promise);
      }
      jobTemplates = await Promise.all(jobTemplatePromises);
    });

    after(async function() {
      let deletionPromises = jobTemplates.map(jobTemplate => JobTemplate.delete(jobTemplate.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new JobTemplateCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).to.be.an.instanceof(JobTemplateCollection);
        expect(collection).to.have.lengthOf(nbJobTemplates);
      });

      it('Fetch (static method)', async function() {
        let collection = await JobTemplateCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(JobTemplateCollection);
        expect(collection).to.have.lengthOf(nbJobTemplates);
      });

      it('Fetch with several requests', async function() {
        let collection = await JobTemplateCollection.fetchAll({nbPerPage: 1, filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(JobTemplateCollection);
        expect(collection).to.have.lengthOf(nbJobTemplates);
      });

      it('Fetch without filter', async function() {
        let collection = new JobTemplateCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await JobTemplateCollection.fetchAll({filterKey: 'project', filterValue: project});
        for(let jobTemplate of collection) {
          expect(jobTemplate).to.be.an.instanceof(JobTemplate);
        }
      });

      it('Add item to the collection', function() {
        let collection = new JobTemplateCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new JobTemplate());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new JobTemplateCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new JobTemplateCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new JobTemplateCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new JobTemplateCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

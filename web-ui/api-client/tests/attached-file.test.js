import * as utils from './utils.js';
import {AttachedFile, AttachedFileCollection} from '@';

describe('AttachedFile', function() {

  let file = new Blob(['<a id="a"><b id="b">hey!</b></a>'], { type: 'text/xml'});
  let filename = 'test_file.xml';
  let project;

  let attachedFile = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
    project = await utils.getProject();
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      attachedFile = new AttachedFile({file, filename}, project);
      await attachedFile.save();
      id = attachedFile.id;
      expect(attachedFile).toBeInstanceOf(AttachedFile);
      expect(attachedFile.id).toBebove(0);
    });

    it('Create without providing associated object', async function() {
      let attachedFileWithoutObject = new AttachedFile({file});
      expect(attachedFileWithoutObject.save()).rejects.toThrow();
    });

    it('Create without providing file', async function() {
      let attachedFile = new AttachedFile({}, project);
      expect(attachedFile.save()).rejects.toThrow();
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedFile = await AttachedFile.fetch(id);
      expect(fetchedFile).toBeInstanceOf(AttachedFile);
      expect(fetchedFile.domainIdent).toEqual(project.id);
      // expect(fetchedFile.filename).toEqual(filename);
    });

    it('Fetch with instance method', async function() {
      let fetchedFile = await new AttachedFile({id}).fetch();
      expect(fetchedFile).toBeInstanceOf(AttachedFile);
      expect(fetchedFile.domainIdent).toEqual(project.id);
      // expect(fetchedFile.filename).toEqual(filename);
    });

    it('Fetch without providing associated object', function() {
      expect(AttachedFile.fetch({id})).rejects.toThrow();
    });
  });

  describe('Update', function() {
    it('Update', function() {
      attachedFile.filename = 'new_filename.xml';
      expect(attachedFile.update.bind(attachedFile)).toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await AttachedFile.delete(id);
    });

    it('Fetch a deleted element', function() {
      expect(AttachedFile.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('AttachedFileCollection', function() {
    let nbAttachedFiles = 3;
    let attachedFiles;
    let totalNb = 0;

    beforeAll(async function() {
      let attachedFilePromises = [];
      for(let i = 0; i < nbAttachedFiles; i++) {
        attachedFilePromises.push(new AttachedFile({file}, project).save());
      }
      attachedFiles = await Promise.all(attachedFilePromises);
    });

    afterAll(async function() {
      let deletionPromises = attachedFiles.map(attachedFile => AttachedFile.delete(attachedFile.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new AttachedFileCollection({object: project}).fetchAll();
        expect(collection).toBeInstanceOf(AttachedFileCollection);
        expect(collection).toBeGreaterThanOrEqual(nbAttachedFiles);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await AttachedFileCollection.fetchAll({object: project});
        expect(collection).toBeInstanceOf(AttachedFileCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await AttachedFileCollection.fetchAll({object: project, nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(AttachedFileCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without associated object', async function() {
        expect(AttachedFileCollection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await AttachedFileCollection.fetchAll({object: project});
        for(let attachedFile of collection) {
          expect(attachedFile).toBeInstanceOf(AttachedFile);
        }
      });

      it('Add item to the collection', function() {
        let collection = new AttachedFileCollection({object: project});
        expect(collection).toHaveLength(0);
        collection.push(new AttachedFile({file}, project));
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new AttachedFileCollection({object: project});
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new AttachedFileCollection({object: project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new AttachedFileCollection({object: project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new AttachedFileCollection({object: project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

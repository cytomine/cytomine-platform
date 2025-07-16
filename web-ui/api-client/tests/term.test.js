import * as utils from './utils.js';
import {Term, TermCollection} from '@/index.js';

describe('Term', () => {

  let ontology;
  let name = utils.randomString();
  let color = '#ffffff';

  let term = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect(true);
    ({id: ontology} = await utils.getOntology());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      term = new Term({name, ontology, color});
      term = await term.save();
      id = term.id;
      expect(term).toBeInstanceOf(Term);
      expect(term.name).toEqual(name);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedTerm = await Term.fetch(id);
      expect(fetchedTerm).toBeInstanceOf(Term);
      expect(fetchedTerm.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedTerm = await new Term({id}).fetch();
      expect(fetchedTerm).toBeInstanceOf(Term);
      expect(fetchedTerm.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Term.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', () => {
    let idParent1;
    let idParent2;

    beforeAll(async () => {
      ({id: idParent1} = await utils.getTerm({ontology}));
      ({id: idParent2} = await utils.getTerm({ontology}));
    });

    it('Set parent', async () => {
      await term.changeParent(idParent1);
      expect(term.parent).toEqual(idParent1);
      await term.fetch();
      expect(term.parent).toEqual(idParent1);
    });

    it('Update parent', async () => {
      await term.changeParent(idParent2);
      expect(term.parent).toEqual(idParent2);
      await term.fetch();
      expect(term.parent).toEqual(idParent2);
    });

    it('Remove parent', async () => {
      await term.changeParent(null);
      expect(term.parent).toEqual(null);
      await term.fetch();
      expect(term.parent).toEqual(null);
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      term.name = newName;
      term = await term.update();
      expect(term).toBeInstanceOf(Term);
      expect(term.name).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Term.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Term.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('TermCollection', () => {

    let nbTerms = 3;
    let terms;
    let totalNb = 0;

    let project;

    beforeAll(async () => {
      ({id: project} = await utils.getProject({ontology}));

      let termPromises = [];
      for (let i = 0; i < nbTerms; i++) {
        termPromises.push(new Term({name: utils.randomString(), ontology, color}).save());
      }
      terms = await Promise.all(termPromises);
    });

    afterAll(async () => {
      let deletionPromises = terms.map(term => Term.delete(term.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new TermCollection().fetchAll();
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbTerms);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await TermCollection.fetchAll();
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await TermCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await TermCollection.fetchAll();
        for (let term of collection) {
          expect(term).toBeInstanceOf(Term);
        }
      });

      it('Add item to the collection', () => {
        let collection = new TermCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Term());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new TermCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', () => {
      it('Filter on project', async () => {
        let collection = new TermCollection();
        collection.setFilter('project', project);
        await collection.fetchAll();
        expect(collection.length).toBeGreaterThanOrEqual(nbTerms);
      });

      it('Filter on ontology', async () => {
        let collection = new TermCollection({filterKey: 'ontology', filterValue: ontology});
        await collection.fetchAll();
        expect(collection.length).toBeGreaterThanOrEqual(nbTerms);
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new TermCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new TermCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new TermCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

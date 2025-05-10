import * as utils from './utils.js';
import {Term, TermCollection} from '@';

describe('Term', function() {

  let ontology;
  let name = utils.randomString();
  let color = '#ffffff';

  let term = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect(true);
    ({id: ontology} = await utils.getOntology());
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      term = new Term({name, ontology, color});
      term = await term.save();
      id = term.id;
      expect(term).toBeInstanceOf(Term);
      expect(term.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedTerm = await Term.fetch(id);
      expect(fetchedTerm).toBeInstanceOf(Term);
      expect(fetchedTerm.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedTerm = await new Term({id}).fetch();
      expect(fetchedTerm).toBeInstanceOf(Term);
      expect(fetchedTerm.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Term.fetch(0)).rejects..toThrow();
    });
  });

  describe('Specific operations', function() {
    let idParent1;
    let idParent2;

    beforeAll(async function() {
      ({id: idParent1} = await utils.getTerm({ontology}));
      ({id: idParent2} = await utils.getTerm({ontology}));
    });

    it('Set parent', async function() {
      await term.changeParent(idParent1);
      expect(term.parent).to.equal(idParent1);
      await term.fetch();
      expect(term.parent).to.equal(idParent1);
    });

    it('Update parent', async function() {
      await term.changeParent(idParent2);
      expect(term.parent).to.equal(idParent2);
      await term.fetch();
      expect(term.parent).to.equal(idParent2);
    });

    it('Remove parent', async function() {
      await term.changeParent(null);
      expect(term.parent).to.equal(null);
      await term.fetch();
      expect(term.parent).to.equal(null);
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      term.name = newName;
      term = await term.update();
      expect(term).toBeInstanceOf(Term);
      expect(term.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Term.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Term.fetch(id)).rejects..toThrow();
    });
  });

  // --------------------

  describe('TermCollection', function() {

    let nbTerms = 3;
    let terms;
    let totalNb = 0;

    let project;

    beforeAll(async function() {
      ({id: project} = await utils.getProject({ontology}));

      let termPromises = [];
      for(let i = 0; i < nbTerms; i++) {
        termPromises.push(new Term({name: utils.randomString(), ontology, color}).save());
      }
      terms = await Promise.all(termPromises);
    });

    afterAll(async function() {
      let deletionPromises = terms.map(term => Term.delete(term.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new TermCollection().fetchAll();
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection).to.have.lengthOf.at.least(nbTerms);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await TermCollection.fetchAll();
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await TermCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(TermCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await TermCollection.fetchAll();
        for(let term of collection) {
          expect(term).toBeInstanceOf(Term);
        }
      });

      it('Add item to the collection', function() {
        let collection = new TermCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Term());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new TermCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Filtering', function() {
      it('Filter on project', async function() {
        let collection = new TermCollection();
        collection.setFilter('project', project);
        await collection.fetchAll();
        expect(collection).to.have.lengthOf.at.least(nbTerms);
      });

      it('Filter on ontology', async function() {
        let collection = new TermCollection({filterKey: 'ontology', filterValue: ontology});
        await collection.fetchAll();
        expect(collection).to.have.lengthOf.at.least(nbTerms);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new TermCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new TermCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new TermCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

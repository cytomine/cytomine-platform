import * as utils from './utils.js';
import {Ontology, OntologyCollection, TermCollection} from '@';

describe('Ontology', function() {

  let name = utils.randomString();

  let ontology = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
  });

  describe('Create', function() {
    it('Create', async function() {
      ontology = new Ontology({name});
      ontology = await ontology.save();
      id = ontology.id;
      expect(id).toBeDefined();
      expect(ontology.name).toEqual(name);
    });
  });

  describe('Clone', function() {
    it('Clone', async function() {
      let fetchedOntology = await utils.getOntology();
      let clone = fetchedOntology.clone();
      expect(clone).toBeInstanceOf(Ontology);

      expect(clone.children).toBeInstanceOf(TermCollection);
      expect(clone.children).not.toBe(fetchedOntology.children);
      expect(clone.children).toEqual(fetchedOntology.children);

      if(fetchedOntology.children.length > 0) {
        expect(clone.children.get(0)).not.toBe(fetchedOntology.children.get(0));
        expect(clone.children.get(0)).toEqual(fetchedOntology.children.get(0));
      }
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedOntology = await Ontology.fetch(id);
      expect(fetchedOntology).toBeInstanceOf(Ontology);
      expect(fetchedOntology.name).toEqual(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedOntology = await new Ontology({id}).fetch();
      expect(fetchedOntology).toBeInstanceOf(Ontology);
      expect(fetchedOntology.name).toEqual(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Ontology.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      ontology.name = newName;
      ontology = await ontology.update();
      expect(ontology).toBeInstanceOf(Ontology);
      expect(ontology.name).toEqual(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Ontology.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Ontology.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('OntologyCollection', function() {

    let nbOntologies = 3;
    let ontologies;
    let totalNb = 0;

    beforeAll(async function() {
      let ontologyPromises = [];
      for(let i = 0; i < nbOntologies; i++) {
        ontologyPromises.push(new Ontology({name: utils.randomString()}).save());
      }
      ontologies = await Promise.all(ontologyPromises);
    });

    afterAll(async function() {
      let deletionPromises = ontologies.map(ontology => Ontology.delete(ontology.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new OntologyCollection().fetchAll();
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toBeGreaterThanOrEqual(nbOntologies);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await OntologyCollection.fetchAll();
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch light version', async function() {
        let collection = await OntologyCollection.fetchAll({light: true});
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
        let model = collection.get(0);
        expect(model.user).toBeNull();
        expect(model.projects).toHaveLength(0);
        expect(model.children).toHaveLength(0);
      });

      it('Fetch with several requests', async function() {
        let collection = await OntologyCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await OntologyCollection.fetchAll();
        for(let ontology of collection) {
          expect(ontology).toBeInstanceOf(Ontology);
        }
      });

      it('Add item to the collection', function() {
        let collection = new OntologyCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Ontology());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new OntologyCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

import * as utils from './utils.js';
import {Ontology, OntologyCollection, TermCollection} from '@/index.js';

describe('Ontology', () => {

  let name = utils.randomString();

  let ontology = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
  });

  describe('Create', () => {
    it('Create', async () => {
      ontology = new Ontology({name});
      ontology = await ontology.save();
      id = ontology.id;
      expect(id).toBeDefined();
      expect(ontology.name).toEqual(name);
    });
  });

  describe('Clone', () => {
    it('Clone', async () => {
      let fetchedOntology = await utils.getOntology();
      let clone = fetchedOntology.clone();
      expect(clone).toBeInstanceOf(Ontology);

      expect(clone.children).toBeInstanceOf(TermCollection);
      expect(clone.children).not.toBe(fetchedOntology.children);
      expect(clone.children).toEqual(fetchedOntology.children);

      if (fetchedOntology.children.length > 0) {
        expect(clone.children.get(0)).not.toBe(fetchedOntology.children.get(0));
        expect(clone.children.get(0)).toEqual(fetchedOntology.children.get(0));
      }
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedOntology = await Ontology.fetch(id);
      expect(fetchedOntology).toBeInstanceOf(Ontology);
      expect(fetchedOntology.name).toEqual(name);
    });

    it('Fetch with instance method', async () => {
      let fetchedOntology = await new Ontology({id}).fetch();
      expect(fetchedOntology).toBeInstanceOf(Ontology);
      expect(fetchedOntology.name).toEqual(name);
    });

    it('Fetch with wrong ID', async () => {
      await expect(Ontology.fetch(0)).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newName = utils.randomString();
      ontology.name = newName;
      ontology = await ontology.update();
      expect(ontology).toBeInstanceOf(Ontology);
      expect(ontology.name).toEqual(newName);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Ontology.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(Ontology.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('OntologyCollection', () => {

    let nbOntologies = 3;
    let ontologies;
    let totalNb = 0;

    beforeAll(async () => {
      let ontologyPromises = [];
      for (let i = 0; i < nbOntologies; i++) {
        ontologyPromises.push(new Ontology({name: utils.randomString()}).save());
      }
      ontologies = await Promise.all(ontologyPromises);
    });

    afterAll(async () => {
      let deletionPromises = ontologies.map(ontology => Ontology.delete(ontology.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new OntologyCollection().fetchAll();
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection.length).toBeGreaterThanOrEqual(nbOntologies);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await OntologyCollection.fetchAll();
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch light version', async () => {
        let collection = await OntologyCollection.fetchAll({light: true});
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
        let model = collection.get(0);
        expect(model.user).toBeNull();
        expect(model.projects).toHaveLength(0);
        expect(model.children).toHaveLength(0);
      });

      it('Fetch with several requests', async () => {
        let collection = await OntologyCollection.fetchAll({nbPerPage: Math.ceil(totalNb / 3)});
        expect(collection).toBeInstanceOf(OntologyCollection);
        expect(collection).toHaveLength(totalNb);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await OntologyCollection.fetchAll();
        for (let ontology of collection) {
          expect(ontology).toBeInstanceOf(Ontology);
        }
      });

      it('Add item to the collection', () => {
        let collection = new OntologyCollection();
        expect(collection).toHaveLength(0);
        collection.push(new Ontology());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new OntologyCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new OntologyCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

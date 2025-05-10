import * as utils from './utils.js';
import {Ontology, OntologyCollection, TermCollection} from '@';

describe('Ontology', function() {

  let name = utils.randomString();

  let ontology = null;
  let id = 0;

  before(async function() {
    await utils.connect();
  });

  describe('Create', function() {
    it('Create', async function() {
      ontology = new Ontology({name});
      ontology = await ontology.save();
      id = ontology.id;
      expect(id).to.exist;
      expect(ontology.name).to.equal(name);
    });
  });

  describe('Clone', function() {
    it('Clone', async function() {
      let fetchedOntology = await utils.getOntology();
      let clone = fetchedOntology.clone();
      expect(clone).to.be.an.instanceof(Ontology);

      expect(clone.children).to.be.an.instanceof(TermCollection);
      expect(clone.children).to.not.equal(fetchedOntology.children);
      expect(clone.children).to.deep.equal(fetchedOntology.children);

      if(fetchedOntology.children.length > 0) {
        expect(clone.children.get(0)).to.not.equal(fetchedOntology.children.get(0));
        expect(clone.children.get(0)).to.deep.equal(fetchedOntology.children.get(0));
      }
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedOntology = await Ontology.fetch(id);
      expect(fetchedOntology).to.be.an.instanceof(Ontology);
      expect(fetchedOntology.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedOntology = await new Ontology({id}).fetch();
      expect(fetchedOntology).to.be.an.instanceof(Ontology);
      expect(fetchedOntology.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Ontology.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      ontology.name = newName;
      ontology = await ontology.update();
      expect(ontology).to.be.an.instanceof(Ontology);
      expect(ontology.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Ontology.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Ontology.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('OntologyCollection', function() {

    let nbOntologies = 3;
    let ontologies;
    let totalNb = 0;

    before(async function() {
      let ontologyPromises = [];
      for(let i = 0; i < nbOntologies; i++) {
        ontologyPromises.push(new Ontology({name: utils.randomString()}).save());
      }
      ontologies = await Promise.all(ontologyPromises);
    });

    after(async function() {
      let deletionPromises = ontologies.map(ontology => Ontology.delete(ontology.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new OntologyCollection().fetchAll();
        expect(collection).to.be.an.instanceof(OntologyCollection);
        expect(collection).to.have.lengthOf.at.least(nbOntologies);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await OntologyCollection.fetchAll();
        expect(collection).to.be.an.instanceof(OntologyCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch light version', async function() {
        let collection = await OntologyCollection.fetchAll({light: true});
        expect(collection).to.be.an.instanceof(OntologyCollection);
        expect(collection).to.have.lengthOf(totalNb);
        let model = collection.get(0);
        expect(model.user).to.be.null;
        expect(model.projects).to.have.lengthOf(0);
        expect(model.children).to.have.lengthOf(0);
      });

      it('Fetch with several requests', async function() {
        let collection = await OntologyCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(OntologyCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await OntologyCollection.fetchAll();
        for(let ontology of collection) {
          expect(ontology).to.be.an.instanceof(Ontology);
        }
      });

      it('Add item to the collection', function() {
        let collection = new OntologyCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Ontology());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new OntologyCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new OntologyCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

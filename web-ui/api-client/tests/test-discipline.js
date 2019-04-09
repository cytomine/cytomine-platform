import * as utils from './utils.js';
import {Discipline, DisciplineCollection} from '@';

describe('Discipline', function() {

  let name = utils.randomString();

  let discipline = null;
  let id = 0;

  before(async function() {
    await utils.connect(true);
  });

  describe('Create', function() {
    it('Create', async function() {
      discipline = new Discipline({name});
      discipline = await discipline.save();
      id = discipline.id;
      expect(discipline).to.be.an.instanceof(Discipline);
      expect(discipline.name).to.equal(name);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedDiscipline = await Discipline.fetch(id);
      expect(fetchedDiscipline).to.be.an.instanceof(Discipline);
      expect(fetchedDiscipline.name).to.equal(name);
    });

    it('Fetch with instance method', async function() {
      let fetchedDiscipline = await new Discipline({id}).fetch();
      expect(fetchedDiscipline).to.be.an.instanceof(Discipline);
      expect(fetchedDiscipline.name).to.equal(name);
    });

    it('Fetch with wrong ID', function() {
      expect(Discipline.fetch(0)).to.be.rejected;
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newName = utils.randomString();
      discipline.name = newName;
      discipline = await discipline.update();
      expect(discipline).to.be.an.instanceof(Discipline);
      expect(discipline.name).to.equal(newName);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Discipline.delete(id);
    });

    it('Fetch deleted', function() {
      expect(Discipline.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('DisciplineCollection', function() {

    let nbDisciplines = 3;
    let disciplines;
    let totalNb = 0;

    before(async function() {
      let disciplinePromises = [];
      for(let i = 0; i < nbDisciplines; i++) {
        disciplinePromises.push(new Discipline({name: utils.randomString()}).save());
      }
      disciplines = await Promise.all(disciplinePromises);
    });

    after(async function() {
      let deletionPromises = disciplines.map(discipline => Discipline.delete(discipline.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new DisciplineCollection().fetchAll();
        expect(collection).to.be.an.instanceof(DisciplineCollection);
        expect(collection).to.have.lengthOf.at.least(nbDisciplines);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await DisciplineCollection.fetchAll();
        expect(collection).to.be.an.instanceof(DisciplineCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await DisciplineCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3)});
        expect(collection).to.be.an.instanceof(DisciplineCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await DisciplineCollection.fetchAll();
        for(let discipline of collection) {
          expect(discipline).to.be.an.instanceof(Discipline);
        }
      });

      it('Add item to the collection', function() {
        let collection = new DisciplineCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new Discipline());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new DisciplineCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new DisciplineCollection({nbPerPage});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new DisciplineCollection({nbPerPage});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new DisciplineCollection({nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

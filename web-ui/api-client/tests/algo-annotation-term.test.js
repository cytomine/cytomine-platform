import * as utils from './utils.js';
import {AlgoAnnotationTerm, AlgoAnnotationTermCollection} from '@';

describe('AlgoAnnotationTerm', function() {

  let annotation;

  before(async function() {
    await utils.connect();
    ({id: annotation} = await utils.getAnnotation());
  });

  after(async function() {
    await utils.cleanData();
  });

  // --------------------

  describe('AlgoAnnotationTermCollection', function() {

    let totalNb = 0;

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new AlgoAnnotationTermCollection({nbPerPage: 0,
          filterKey: 'annotation', filterValue: annotation}).fetchAll();
        expect(collection).to.be.an.instanceof(AlgoAnnotationTermCollection);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        expect(collection).to.be.an.instanceof(AlgoAnnotationTermCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3),
          filterKey: 'annotation', filterValue: annotation});
        expect(collection).to.be.an.instanceof(AlgoAnnotationTermCollection);
        expect(collection).to.have.lengthOf(totalNb);
      });

      it('Fetch without filter', async function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        for(let algoAnnotationTerm of collection) {
          expect(algoAnnotationTerm).to.be.an.instanceof(AlgoAnnotationTerm);
        }
      });

      it('Add item to the collection', function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new AlgoAnnotationTerm());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

  });

});

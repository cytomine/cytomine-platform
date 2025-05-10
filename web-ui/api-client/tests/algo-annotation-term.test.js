import * as utils from './utils.js';
import {AlgoAnnotationTerm, AlgoAnnotationTermCollection} from '@';

describe('AlgoAnnotationTerm', function() {

  let annotation;

  beforeAll(async function() {
    await utils.connect();
    ({id: annotation} = await utils.getAnnotation());
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  // --------------------

  describe('AlgoAnnotationTermCollection', function() {

    let totalNb = 0;

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new AlgoAnnotationTermCollection({nbPerPage: 0,
          filterKey: 'annotation', filterValue: annotation}).fetchAll();
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({nbPerPage: Math.ceil(totalNb/3),
          filterKey: 'annotation', filterValue: annotation});
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without filter', async function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await AlgoAnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        for(let algoAnnotationTerm of collection) {
          expect(algoAnnotationTerm).toBeInstanceOf(AlgoAnnotationTerm);
        }
      });

      it('Add item to the collection', function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection).toHaveLength(0);
        collection.push(new AlgoAnnotationTerm());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

  });

});

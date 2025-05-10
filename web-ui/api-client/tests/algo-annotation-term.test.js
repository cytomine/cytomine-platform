import * as utils from './utils.js';
import { AlgoAnnotationTerm, AlgoAnnotationTermCollection } from '@/index.js';

describe('AlgoAnnotationTerm', () => {

  let annotation;

  beforeAll(async () => {
    await utils.connect();
    ({ id: annotation } = await utils.getAnnotation());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  // --------------------

  describe('AlgoAnnotationTermCollection', () => {

    let totalNb = 0;

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new AlgoAnnotationTermCollection({
          nbPerPage: 0,
          filterKey: 'annotation', filterValue: annotation
        }).fetchAll();
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        totalNb = collection.length;
      });

      it('Fetch (static method)', async () => {
        let collection = await AlgoAnnotationTermCollection.fetchAll({ filterKey: 'annotation', filterValue: annotation });
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch with several requests', async () => {
        let collection = await AlgoAnnotationTermCollection.fetchAll({
          nbPerPage: Math.ceil(totalNb / 3),
          filterKey: 'annotation', filterValue: annotation
        });
        expect(collection).toBeInstanceOf(AlgoAnnotationTermCollection);
        expect(collection).toHaveLength(totalNb);
      });

      it('Fetch without filter', async () => {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await AlgoAnnotationTermCollection.fetchAll({ filterKey: 'annotation', filterValue: annotation });
        for (let algoAnnotationTerm of collection) {
          expect(algoAnnotationTerm).toBeInstanceOf(AlgoAnnotationTerm);
        }
      });

      it('Add item to the collection', () => {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection).toHaveLength(0);
        collection.push(new AlgoAnnotationTerm());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new AlgoAnnotationTermCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });
  });
});

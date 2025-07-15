import * as utils from './utils.js';
import {AnnotationTerm, AnnotationTermCollection} from '@/index.js';

describe('AnnotationTerm', () => {
  let userannotation;

  beforeAll(async () => {
    await utils.connect();
    ({id: userannotation} = await utils.getAnnotation());
  });

  describe('AnnotationTermCollection', () => {

    let nbAnnotationTerms = 3;
    let annotationTerms;

    beforeAll(async () => {
      async function createTermAndAnnotTerm() {
        let tempTerm = await utils.getTerm();
        let annotTerm = new AnnotationTerm({userannotation, term: tempTerm.id});
        await annotTerm.save();
        return annotTerm;
      }

      let annotationTermPromises = [];
      for (let i = 0; i < nbAnnotationTerms; i++) {
        annotationTermPromises.push(createTermAndAnnotTerm());
      }
      annotationTerms = await Promise.all(annotationTermPromises);
    });

    afterAll(async () => {
      let deletionPromises = annotationTerms.map(at => AnnotationTerm.delete(at.userannotation, at.term));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new AnnotationTermCollection({filterKey: 'annotation', filterValue: userannotation}).fetchAll();
        expect(collection).toBeInstanceOf(AnnotationTermCollection);
        expect(collection).toHaveLength(nbAnnotationTerms);
      });

      it('Fetch (static method)', async () => {
        let collection = await AnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: userannotation});
        expect(collection).toBeInstanceOf(AnnotationTermCollection);
        expect(collection).toHaveLength(nbAnnotationTerms);
      });

      it('Fetch with several requests', async () => {
        let collection = await AnnotationTermCollection.fetchAll({
          nbPerPage: Math.ceil(nbAnnotationTerms / 3),
          filterKey: 'annotation', filterValue: userannotation
        });
        expect(collection).toBeInstanceOf(AnnotationTermCollection);
        expect(collection).toHaveLength(nbAnnotationTerms);
      });

      it('Fetch without filter', async () => {
        let collection = new AnnotationTermCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await AnnotationTermCollection.fetchAll({filterKey: 'annotation', filterValue: userannotation});
        for (let annotationTerm of collection) {
          expect(annotationTerm).toBeInstanceOf(AnnotationTerm);
        }
      });

      it('Add item to the collection', () => {
        let collection = new AnnotationTermCollection();
        expect(collection).toHaveLength(0);
        collection.push(new AnnotationTerm());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new AnnotationTermCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new AnnotationTermCollection({
          nbPerPage,
          filterKey: 'annotation', filterValue: userannotation
        });
        await collection.fetchPage(1);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new AnnotationTermCollection({
          nbPerPage,
          filterKey: 'annotation', filterValue: userannotation
        });
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new AnnotationTermCollection({
          nbPerPage,
          filterKey: 'annotation', filterValue: userannotation
        });
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

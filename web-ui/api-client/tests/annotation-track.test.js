import * as utils from './utils.js';
import {AnnotationTrack, AnnotationTrackCollection} from '@/index.js';

describe('AnnotationTrack', () => {

  let annotation;
  let track;

  let annotationTrack = null;

  beforeAll(async () => {
    await utils.connect();
    ({id: annotation} = await utils.getAnnotation());
    ({id: track} = await utils.getTrack());
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      annotationTrack = new AnnotationTrack({annotation, track});
      annotationTrack = await annotationTrack.save();
      expect(annotationTrack).toBeInstanceOf(AnnotationTrack);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedAnnotationTrack = await AnnotationTrack.fetch(annotation, track);
      expect(fetchedAnnotationTrack).toBeInstanceOf(AnnotationTrack);
      expect(fetchedAnnotationTrack).toEqual(annotationTrack);
    });

    it('Fetch with instance method', async () => {
      let fetchedAnnotationTrack = await new AnnotationTrack({annotation, track}).fetch();
      expect(fetchedAnnotationTrack).toBeInstanceOf(AnnotationTrack);
      expect(fetchedAnnotationTrack).toEqual(annotationTrack);
    });

    it('Fetch with wrong ID', async () => {
      await expect(AnnotationTrack.fetch(0)).rejects.toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await AnnotationTrack.delete(annotation, track);
    });

    it('Fetch deleted', async () => {
      await expect(AnnotationTrack.fetch(annotation, track)).rejects.toThrow();
    });
  });

  // --------------------

  describe('AnnotationTrackCollection', () => {

    let nbAnnotationTracks = 3;
    let annotationTracks;

    beforeAll(async () => {
      let tempImage = await utils.getImageInstance();
      async function createTrackAndAnnotTrack() {
        let tempTrack = await utils.getTrack({image: tempImage.id});
        let annotTrack = new AnnotationTrack({annotation: annotation, track: tempTrack.id});
        await annotTrack.save();
        return annotTrack;
      }

      let annotationTrackPromises = [];
      for (let i = 0; i < nbAnnotationTracks; i++) {
        annotationTrackPromises.push(createTrackAndAnnotTrack());
      }
      annotationTracks = await Promise.all(annotationTrackPromises);
    });

    afterAll(async () => {
      let deletionPromises = annotationTracks.map(at => AnnotationTrack.delete(at.annotation, at.track));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new AnnotationTrackCollection({filterKey: 'annotation', filterValue: annotation}).fetchAll();
        expect(collection).toBeInstanceOf(AnnotationTrackCollection);
        expect(collection).toHaveLength(nbAnnotationTracks);
      });

      it('Fetch (static method)', async () => {
        let collection = await AnnotationTrackCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        expect(collection).toBeInstanceOf(AnnotationTrackCollection);
        expect(collection).toHaveLength(nbAnnotationTracks);
      });

      it('Fetch with several requests', async () => {
        let collection = await AnnotationTrackCollection.fetchAll({
          nbPerPage: Math.ceil(nbAnnotationTracks / 3),
          filterKey: 'annotation', filterValue: annotation
        });
        expect(collection).toBeInstanceOf(AnnotationTrackCollection);
        expect(collection).toHaveLength(nbAnnotationTracks);
      });

      it('Fetch without filter', async () => {
        let collection = new AnnotationTrackCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await AnnotationTrackCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        for (let annotationTrack of collection) {
          expect(annotationTrack).toBeInstanceOf(AnnotationTrack);
        }
      });

      it('Add item to the collection', () => {
        let collection = new AnnotationTrackCollection();
        expect(collection).toHaveLength(0);
        collection.push(new AnnotationTrack());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new AnnotationTrackCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new AnnotationTrackCollection({
          nbPerPage,
          filterKey: 'annotation',
          filterValue: annotation,
        });
        await collection.fetchPage(1);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new AnnotationTrackCollection({
          nbPerPage,
          filterKey: 'annotation', filterValue: annotation
        });
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new AnnotationTrackCollection({
          nbPerPage,
          filterKey: 'annotation',
          filterValue: annotation,
        });
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });
  });
});

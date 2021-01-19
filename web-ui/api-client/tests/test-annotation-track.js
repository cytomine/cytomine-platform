import * as utils from './utils.js';
import {AnnotationTrack, AnnotationTrackCollection} from '@';

describe('AnnotationTrack', function() {

  let annotation;
  let track;

  let annotationTrack = null;

  before(async function() {
    await utils.connect();
    ({id: annotation} = await utils.getAnnotation());
    ({id: track} = await utils.getTrack());
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      annotationTrack = new AnnotationTrack({annotation, track});
      annotationTrack = await annotationTrack.save();
      expect(annotationTrack).to.be.an.instanceof(AnnotationTrack);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedAnnotationTrack = await AnnotationTrack.fetch(annotation, track);
      expect(fetchedAnnotationTrack).to.be.an.instanceof(AnnotationTrack);
      expect(fetchedAnnotationTrack).to.deep.equal(annotationTrack);
    });

    it('Fetch with instance method', async function() {
      let fetchedAnnotationTrack = await new AnnotationTrack({annotation, track}).fetch();
      expect(fetchedAnnotationTrack).to.be.an.instanceof(AnnotationTrack);
      expect(fetchedAnnotationTrack).to.deep.equal(annotationTrack);
    });

    it('Fetch with wrong ID', function() {
      expect(AnnotationTrack.fetch(0)).to.be.rejected;
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await AnnotationTrack.delete(annotation, track);
    });

    it('Fetch deleted', function() {
      expect(AnnotationTrack.fetch(annotation, track)).to.be.rejected;
    });
  });

  // --------------------

  describe('AnnotationTrackCollection', function() {

    let nbAnnotationTracks = 3;
    let annotationTracks;

    before(async function() {
      let tempImage = await utils.getImageInstance();
      async function createTrackAndAnnotTrack() {
        let tempTrack = await utils.getTrack({image : tempImage.id});
        let annotTrack = new AnnotationTrack({annotation: annotation, track: tempTrack.id});
        await annotTrack.save();
        return annotTrack;
      }

      let annotationTrackPromises = [];
      for(let i = 0; i < nbAnnotationTracks; i++) {
        annotationTrackPromises.push(createTrackAndAnnotTrack());
      }
      annotationTracks = await Promise.all(annotationTrackPromises);
    });

    after(async function() {
      let deletionPromises = annotationTracks.map(at => AnnotationTrack.delete(at.annotation, at.track));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new AnnotationTrackCollection({filterKey: 'annotation', filterValue: annotation}).fetchAll();
        expect(collection).to.be.an.instanceof(AnnotationTrackCollection);
        expect(collection).to.have.lengthOf(nbAnnotationTracks);
      });

      it('Fetch (static method)', async function() {
        let collection = await AnnotationTrackCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        expect(collection).to.be.an.instanceof(AnnotationTrackCollection);
        expect(collection).to.have.lengthOf(nbAnnotationTracks);
      });

      it('Fetch with several requests', async function() {
        let collection = await AnnotationTrackCollection.fetchAll({nbPerPage: Math.ceil(nbAnnotationTracks/3),
          filterKey: 'annotation', filterValue: annotation});
        expect(collection).to.be.an.instanceof(AnnotationTrackCollection);
        expect(collection).to.have.lengthOf(nbAnnotationTracks);
      });

      it('Fetch without filter', async function() {
        let collection = new AnnotationTrackCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await AnnotationTrackCollection.fetchAll({filterKey: 'annotation', filterValue: annotation});
        for(let annotationTrack of collection) {
          expect(annotationTrack).to.be.an.instanceof(AnnotationTrack);
        }
      });

      it('Add item to the collection', function() {
        let collection = new AnnotationTrackCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new AnnotationTrack());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new AnnotationTrackCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new AnnotationTrackCollection({nbPerPage,
          filterKey: 'annotation', filterValue: annotation});
        await collection.fetchPage(1);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new AnnotationTrackCollection({nbPerPage,
          filterKey: 'annotation', filterValue: annotation});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new AnnotationTrackCollection({nbPerPage,
          filterKey: 'annotation', filterValue: annotation});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

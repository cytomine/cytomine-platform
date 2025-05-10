import * as utils from './utils.js';
import {Cytomine, Annotation, AnnotationType, AnnotationCollection, User} from '@/index.js';

describe('Annotation', function() {

  let location = 'POLYGON ((10 10, 10 20, 20 20, 20 10, 15 10, 10 10), (16 16, 18 16, 18 18, 16 18, 16 16))';
  let project;
  let image;

  let annotation = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
    ({id: project} = await utils.getProject()); // HACK required for "Save collection" test case
    let imageInstance = await utils.getImageInstance({project, review: true});
    await imageInstance.review();
    image = imageInstance.id;
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      annotation = new Annotation({location, image});
      annotation = await annotation.save();
      id = annotation.id;
      expect(annotation).toBeInstanceOf(Annotation);
      expect(annotation.type).toEqual(AnnotationType.USER);
      expect(id).toBeDefined();
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedAnnotation = await Annotation.fetch(id);
      expect(fetchedAnnotation).toBeInstanceOf(Annotation);
      //annotationTrack is returned at the creation and not at the fetching.
      delete annotation['annotationTrack'];
      expect(fetchedAnnotation).toEqual(annotation);
    });

    it('Fetch with type', async function() {
      let fetchedAnnotation = await Annotation.fetch(id, AnnotationType.USER);
      expect(fetchedAnnotation).toBeInstanceOf(Annotation);
      expect(fetchedAnnotation).toEqual(annotation);
    });

    it('Fetch with instance method', async function() {
      let fetchedAnnotation = await new Annotation({id}).fetch();
      expect(fetchedAnnotation).toBeInstanceOf(Annotation);
      expect(fetchedAnnotation).toEqual(annotation);
    });

    it('Fetch with wrong ID', function() {
      expect(Annotation.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', function() {
    it('Annotation action', async function() {
      let annotationAction = await annotation.recordAction();
      expect(annotationAction.id).toBeDefined();
    });

    it('[Correction] Add', async function() {
      let currentUser = await User.fetchCurrent();
      let initialArea = annotation.area;
      let result = await Annotation.correctAnnotations({
        image,
        location: 'POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))',
        layers: [currentUser.id]
      });
      expect(result.id).toEqual(annotation.id);
      expect(result.area).toBebove(initialArea);
    });

    it('[Correction] Remove', async function() {
      let currentUser = await User.fetchCurrent();
      let initialArea = annotation.area;
      let result = await Annotation.correctAnnotations({
        image,
        location: 'POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))',
        remove: true,
        layers: [currentUser.id]
      });
      expect(result.id).toEqual(annotation.id);
      expect(result.area).toBeLessThan(initialArea);
    });

    it('Review', async function() {
      let reviewedAnnotation = await annotation.review();
      expect(reviewedAnnotation).toBeInstanceOf(Annotation);
      expect(reviewedAnnotation.type).toEqual(AnnotationType.REVIEWED);
      expect(reviewedAnnotation.parentIdent).toEqual(annotation.id);
      await annotation.fetch();
      expect(annotation.reviewed).toBe(true);
    });

    it('Cancel review', async function() {
      await annotation.cancelReview();
      await annotation.fetch();
      expect(annotation.reviewed).toBe(false);
    });

    it('Simplify', async function() {
      await annotation.simplify(5, 10);
    });

    it('Fill', async function() {
      let initialArea = annotation.area;
      await annotation.fill();
      expect(annotation.area).toBebove(initialArea);
    });
  });

  describe('Undo/redo', function() {
    let urAnnot;
    let command;

    it('Create', async function() {
      urAnnot = await new Annotation({location, image}).save();
      command = Cytomine.instance.lastCommand;
      expect(urAnnot.id).toBeDefined();
      expect(command).toBeDefined();
    });

    it('Undo', async function() {
      let collection = await Cytomine.instance.undo(command);
      expect(collection).toHaveLength(1);
      let annot = collection[0].annotation;
      expect(annot.id).toEqual(urAnnot.id);
      expect(Annotation.fetch(urAnnot.id)).rejects.toThrow();
    });

    it('Redo', async function() {
      let collection = await Cytomine.instance.redo(command);
      expect(collection).toHaveLength(1);
      let annot = collection[0].annotation;
      expect(annot.id).toEqual(urAnnot.id);
      let fetchedAnnotation = await Annotation.fetch(urAnnot.id);
      expect(fetchedAnnotation.id).toEqual(urAnnot.id);
    });

    it('Undo again', async function() {
      let collection = await Cytomine.instance.undo();
      expect(collection).toHaveLength(1);
      let annot = collection[0].annotation;
      expect(annot.id).toEqual(urAnnot.id);
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newLocation = 'POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))';
      annotation.location = newLocation;
      await annotation.update();
      expect(annotation).toBeInstanceOf(Annotation);
      expect(annotation.centroid).toEqual({x: 15, y: 15});
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Annotation.delete(id);
    });

    it('Fetch a deleted element', function() {
      expect(Annotation.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('AnnotationCollection', function() {
    let nb = 3;

    beforeAll(async function() {
      let annotationPromises = [];
      for(let i = 0; i < nb; i++) {
        annotationPromises.push(new Annotation({image, location}).save());
      }
      await Promise.all(annotationPromises);
    });

    afterAll(async function() {
      let collection = await new AnnotationCollection({image}).fetchAll();
      let deletionPromises = [];
      for(let annot of collection) {
        deletionPromises.push(Annotation.delete(annot.id));
      }
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new AnnotationCollection({image}).fetchAll();
        expect(collection).toBeInstanceOf(AnnotationCollection);
        expect(collection).toHaveLength(nb);
      });

      it('Fetch (static method)', async function() {
        let collection = await AnnotationCollection.fetchAll({image});
        expect(collection).toBeInstanceOf(AnnotationCollection);
        expect(collection).toHaveLength(nb);
      });

      it('Fetch with several requests', async function() {
        let collection = await AnnotationCollection.fetchAll({image}, 1);
        expect(collection).toBeInstanceOf(AnnotationCollection);
        expect(collection).toHaveLength(nb);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through the collection', async function() {
        let collection = await AnnotationCollection.fetchAll({image});
        for(let annot of collection) {
          expect(annot).toBeInstanceOf(Annotation);
        }
      });

      it('Add an item to the collection', function() {
        let collection = new AnnotationCollection({image});
        expect(collection).toHaveLength(0);
        collection.push(new Annotation());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new AnnotationCollection({image});
        expect(collection.push.bind(collection, {})).toThrow();
      });

      it('Download URL', function() {
        let collection = new AnnotationCollection({project});
        expect(collection.getDownloadURL()).toBe('string');
      });
    });

    describe('Save collection', function() {
      it('Save', async function() {
        let collection = new AnnotationCollection();
        collection.push(new Annotation({location, image, project}));
        collection.push(new Annotation({location, image, project}));
        await collection.save();
        expect(collection).toHaveLength(2);
      });
    });

    describe('Search service', function() {
      it('Search', async function() {
        let collection = new AnnotationCollection({project});
        collection.project = project;
        collection.terms = [1, 2];
        collection.showTerm = true;
        await collection.fetchAll();
        expect(collection).toHaveLength(0);
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new AnnotationCollection({project, nbPerPage});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new AnnotationCollection({project, nbPerPage});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new AnnotationCollection({project, nbPerPage});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

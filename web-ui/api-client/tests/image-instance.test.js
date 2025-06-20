import * as utils from './utils.js';
import {
  Description,
  ImageConsultation,
  ImageInstance,
  ImageInstanceCollection,
  ProjectConnection,
  Property,
  SliceInstance,
  User,
} from '@/index.js';

describe('ImageInstance', () => {

  let baseImage;
  let project;

  let imageInstance = null;
  let id = 0;

  let idUser;

  beforeAll(async () => {
    await utils.connect();

    let currentUser = await User.fetchCurrent();
    idUser = currentUser.id;

    ({id: baseImage} = await utils.getAbstractImage());
    let projectInstance = await utils.getProject();
    project = projectInstance.id;
    await new ProjectConnection({project}).save(); // required for image consultation
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      imageInstance = new ImageInstance({baseImage, project});
      imageInstance = await imageInstance.save();
      id = imageInstance.id;
      expect(imageInstance).toBeInstanceOf(ImageInstance);
      expect(imageInstance.baseImage).toEqual(baseImage);
    });

    it('Duplicate', async () => {
      let tempImageInstance = new ImageInstance({baseImage, project});
      await expect(tempImageInstance.save()).rejects.toThrow();
    });

    it('Create without base image', async () => {
      let tempImageInstance = new ImageInstance({baseImage});
      await expect(tempImageInstance.save()).rejects.toThrow();
    });

    it('Create without project', async () => {
      let tempImageInstance = new ImageInstance({project});
      await expect(tempImageInstance.save()).rejects.toThrow();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedImageInstance = await ImageInstance.fetch(id);
      expect(fetchedImageInstance).toBeInstanceOf(ImageInstance);
      expect(fetchedImageInstance).toEqual(imageInstance);
    });

    it('Fetch with instance method', async () => {
      let fetchedImageInstance = await new ImageInstance({id}).fetch();
      expect(fetchedImageInstance).toBeInstanceOf(ImageInstance);
      expect(fetchedImageInstance).toEqual(imageInstance);
    });

    it('Fetch next/previous', async () => {
      let nextImageInstance = await imageInstance.fetchNext();
      expect(nextImageInstance).toBeInstanceOf(ImageInstance);
      expect(Number(nextImageInstance.created)).toBeLessThan(Number(imageInstance.created));
      // let previousImageInstance = await nextImageInstance.fetchPrevious();
      // expect(previousImageInstance).toEqual(imageInstance);
    });

    it('Fetch with wrong ID', async () => {
      await expect(ImageInstance.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', () => {
    let imageSource;

    let dataDescription = utils.randomString();

    let property;
    let propKey = 'TEST_JS_KEY';
    let propValue = utils.randomString();

    // let annotation;
    // let location = 'POLYGON((10 10, 20 10, 20 20, 10 20, 10 10), (16 16, 18 16, 18 18, 16 18, 16 16))';

    beforeAll(async () => {
      imageSource = await utils.getImageInstance({baseImage});

      await new Description({data: dataDescription}, imageSource).save();
      property = await new Property({key: propKey, value: propValue}, imageSource).save();
      // annotation = await new Annotation({location, image: imageSource.id}).save();
    });

    afterAll(async () => {
      await Description.delete(imageSource);
      await Property.delete(property.id, imageSource);
      // await Annotation.delete(annotation.id);
    });

    it('Fetch connected users', async () => {
      let connectedUsers = await imageInstance.fetchConnectedUsers();
      expect(connectedUsers).toBeInstanceOf(Array);
    });

    // it('Fetch layers in other projects', async ()  =>{
    //   let layers = await imageInstance.fetchLayersInOtherProjects(imageSource.project);
    //   expect(layers).toBeInstanceOf(Array);
    //   expect(layers).toHaveLength(1);
    //   expect(layers[0].image).toEqual(imageSource.id);
    // });

    // it('Fetch annotations index', async ()  =>{
    //   let layers = await imageInstance.fetchAnnotationsIndex();
    //   expect(layers).toBeInstanceOf(Array);
    // });

    // it('Copy metadata', async ()  =>{
    //   await imageInstance.copyMetadata(imageSource.id);
    //
    //   let copiedDescription = await Description.fetch(imageSource);
    //   expect(copiedDescription.data).toEqual(dataDescription);
    //
    //   let collection = await new PropertyCollection({object: imageSource}).fetchAll();
    //   expect(collection).toHaveLength(1);
    //   let copiedProperty = collection.get(0);
    //   expect(copiedProperty.key).toEqual(propKey);
    //   expect(copiedProperty.value).toEqual(propValue);
    // });

    // it('Copy annotations', async ()  =>{
    //   await imageInstance.copyData([{image: imageSource.id, user: idUser}]);
    //   let collection = await new AnnotationCollection({image: imageSource.id, showGIS: true}).fetchAll();
    //   expect(collection).toHaveLength(1);
    //   let copiedAnnot = collection.get(0);
    //   expect(copiedAnnot.perimeter).toEqual(annotation.perimeter);
    // });

    it('Start review', async () => {
      await imageInstance.review();
      expect(imageInstance.reviewStart).toBeDefined();
    });

    it('Cancel review', async () => {
      await imageInstance.stopReview(true);
      expect(imageInstance.inReview).toBe(false);
    });

    it('Fetch review statistics', async () => {
      let stats = await imageInstance.fetchReviewStatistics();
      expect(stats).toBeInstanceOf(Array);
    });

    it('Download URL', async () => {
      expect(typeof imageInstance.downloadURL).toBe('string');
    });

    it('Fetch reference slice', async () => {
      let slice = await imageInstance.fetchReferenceSlice();
      expect(slice).toBeInstanceOf(SliceInstance);
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newInstanceFilename = utils.randomString();
      imageInstance.instanceFilename = newInstanceFilename;
      imageInstance = await imageInstance.update();
      expect(imageInstance).toBeInstanceOf(ImageInstance);
      expect(imageInstance.instanceFilename).toEqual(newInstanceFilename);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await ImageInstance.delete(id);
    });

    it('Fetch deleted', async () => {
      await expect(ImageInstance.fetch(id)).rejects.toThrow();
    });
  });

  // --------------------

  describe('ImageInstanceCollection', () => {

    let imageInstances;
    let nbImageInstances = 3;

    beforeAll(async () => {
      let imageInstancePromises = [];
      for (let i = 0; i < nbImageInstances; i++) {
        let tmp = utils.randomString();
        let baseImage = await utils.getAbstractImage(tmp);
        imageInstancePromises.push(new ImageInstance({baseImage: baseImage.id, project}).save());
      }
      imageInstances = await Promise.all(imageInstancePromises);

      let consultationsPromise = [];
      for (let i = 0; i < nbImageInstances; i++) {
        consultationsPromise.push(new ImageConsultation({image: imageInstances[i].id}).save());
      }
      await Promise.all(consultationsPromise);
    });

    afterAll(async () => {
      let deletionPromises = imageInstances.map(imageInstance => ImageInstance.delete(imageInstance.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new ImageInstanceCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageInstanceCollection);
        expect(collection).toHaveLength(nbImageInstances);
      });

      it('Fetch (static method)', async () => {
        let collection = await ImageInstanceCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ImageInstanceCollection);
        expect(collection).toHaveLength(nbImageInstances);
      });

      it('Fetch with several requests', async () => {
        let collection = await ImageInstanceCollection.fetchAll({nbPerPage: 1, filterKey: 'project', filterValue: project});
        expect(collection).toBeInstanceOf(ImageInstanceCollection);
        expect(collection).toHaveLength(nbImageInstances);
      });

      it('Fetch without filter', async () => {
        let collection = new ImageInstanceCollection();
        await expect(collection.fetchAll()).rejects.toThrow();
      });

      it('Fetch last opened', async () => {
        let collection = await ImageInstanceCollection.fetchLastOpened({max: nbImageInstances});
        expect(collection).toHaveLength(nbImageInstances);
        let listId = collection.map(imageInstance => imageInstance.id);
        imageInstances.forEach(image => {
          expect(listId).toContain(image.id);
        });
      });

      it('Fetch last opened in project', async () => {
        let collection = await ImageInstanceCollection.fetchLastOpened({
          project: project,
          user: idUser,
          max: nbImageInstances
        });
        expect(collection).toHaveLength(nbImageInstances);
        let listId = collection.map(item => item.image);
        imageInstances.forEach(image => {
          expect(listId).toContain(image.id);
        });
      });

      it('Fetch light version', async () => {
        let collection = await ImageInstanceCollection.fetchAllLight();
        expect(collection.length).toBeGreaterThanOrEqual(nbImageInstances);
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await ImageInstanceCollection.fetchAll({filterKey: 'project', filterValue: project});
        for (let imageInstance of collection) {
          expect(imageInstance).toBeInstanceOf(ImageInstance);
        }
      });

      it('Add item to the collection', () => {
        let collection = new ImageInstanceCollection();
        expect(collection).toHaveLength(0);
        collection.push(new ImageInstance());
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new ImageInstanceCollection();
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Specific operations', () => {
      it('Filter by project', async () => {
        let collection = await new ImageInstanceCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).toBeInstanceOf(ImageInstanceCollection);
        expect(collection).toHaveLength(nbImageInstances);
        expect(collection.get(0).project).toBe(project);
      });

      it('Filter by user', async () => {
        let collection = await new ImageInstanceCollection({filterKey: 'user', filterValue: idUser}).fetchAll();
        expect(collection).toBeInstanceOf(ImageInstanceCollection);
        expect(collection.length).toBeGreaterThan(nbImageInstances);
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

    describe('Search', () => {
      it('Get bounds', async () => {
        let result = await new ImageInstanceCollection.fetchBounds({project: project});
        expect(result.width.max).toBeGreaterThanOrEqual(result.width.min);
      });

      it('Search by name', async () => {
        let searchString = imageInstances[0].instanceFilename;
        let collection = new ImageInstanceCollection({filterKey: 'project', filterValue: project, 'name': {'ilike': searchString}});
        await collection.fetchAll();
        expect(collection).toHaveLength(1);

        searchString = '';
        collection = new ImageInstanceCollection({filterKey: 'project', filterValue: project, 'name': {'ilike': searchString}});
        await collection.fetchAll();
        expect(collection).toHaveLength(imageInstances.length);
      });
    });
  });
});

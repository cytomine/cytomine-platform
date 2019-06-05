import * as utils from './utils.js';
import {ImageInstance, ImageInstanceCollection, ImageConsultation, ProjectConnection,
  User, Description, Property, PropertyCollection, Annotation, AnnotationCollection} from '@';

describe('ImageInstance', function() {

  let baseImage;
  let project;

  let imageInstance = null;
  let id = 0;

  let idUser;

  before(async function() {
    await utils.connect();

    let currentUser = await User.fetchCurrent();
    idUser = currentUser.id;

    ({id: baseImage} = await utils.getAbstractImage());
    let projectInstance = await utils.getProject();
    project = projectInstance.id;
    await new ProjectConnection({project}).save(); // required for image consultation
  });

  after(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      imageInstance = new ImageInstance({baseImage, project});
      imageInstance = await imageInstance.save();
      id = imageInstance.id;
      expect(imageInstance).to.be.an.instanceof(ImageInstance);
      expect(imageInstance.baseImage).to.equal(baseImage);
    });

    it('Duplicate', async function() {
      let tempImageInstance = new ImageInstance({baseImage, project});
      expect(tempImageInstance.save()).to.be.rejected;
    });

    it('Create without base image', async function() {
      let tempImageInstance = new ImageInstance({baseImage});
      expect(tempImageInstance.save()).to.be.rejected;
    });

    it('Create without project', async function() {
      let tempImageInstance = new ImageInstance({project});
      expect(tempImageInstance.save()).to.be.rejected;
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedImageInstance = await ImageInstance.fetch(id);
      expect(fetchedImageInstance).to.be.an.instanceof(ImageInstance);
      expect(fetchedImageInstance).to.deep.equal(imageInstance);
    });

    it('Fetch with instance method', async function() {
      let fetchedImageInstance = await new ImageInstance({id}).fetch();
      expect(fetchedImageInstance).to.be.an.instanceof(ImageInstance);
      expect(fetchedImageInstance).to.deep.equal(imageInstance);
    });

    it('Fetch next/previous', async function() {
      let nextImageInstance = await imageInstance.fetchNext();
      expect(nextImageInstance).to.be.an.instanceof(ImageInstance);
      expect(Number(nextImageInstance.created)).to.be.below(Number(imageInstance.created));
      // let previousImageInstance = await nextImageInstance.fetchPrevious();
      // expect(previousImageInstance).to.deep.equal(imageInstance);
    });

    it('Fetch with wrong ID', function() {
      expect(ImageInstance.fetch(0)).to.be.rejected;
    });
  });

  describe('Specific operations', function() {
    let imageSource;

    let dataDescription = utils.randomString();

    let property;
    let propKey = 'TEST_JS_KEY';
    let propValue = utils.randomString();

    let annotation;
    let location = 'POLYGON((10 10, 20 10, 20 20, 10 20, 10 10), (16 16, 18 16, 18 18, 16 18, 16 16))';

    before(async function() {
      imageSource = await utils.getImageInstance({baseImage});

      await new Description({data: dataDescription}, imageSource).save();
      property = await new Property({key: propKey, value: propValue}, imageSource).save();
      annotation = await new Annotation({location, image: imageSource.id}).save();
    });

    after(async function() {
      await Description.delete(imageSource);
      await Property.delete(property.id, imageSource);
      await Annotation.delete(annotation.id);
    });

    it('Fetch connected users', async function() {
      let connectedUsers = await imageInstance.fetchConnectedUsers();
      expect(connectedUsers).to.be.instanceof(Array);
    });

    it('Fetch layers in other projects', async function() {
      let layers = await imageInstance.fetchLayersInOtherProjects(imageSource.project);
      expect(layers).to.be.instanceof(Array);
      expect(layers).to.have.lengthOf(1);
      expect(layers[0].image).to.equal(imageSource.id);
    });

    it('Fetch annotations index', async function() {
      let layers = await imageInstance.fetchAnnotationsIndex();
      expect(layers).to.be.instanceof(Array);
    });

    it('Copy metadata', async function() {
      await imageInstance.copyMetadata(imageSource.id);

      let copiedDescription = await Description.fetch(imageSource);
      expect(copiedDescription.data).to.equal(dataDescription);

      let collection = await new PropertyCollection({object: imageSource}).fetchAll();
      expect(collection).to.have.lengthOf(1);
      let copiedProperty = collection.get(0);
      expect(copiedProperty.key).to.equal(propKey);
      expect(copiedProperty.value).to.equal(propValue);
    });

    it('Copy annotations', async function() {
      await imageInstance.copyData([{image: imageSource.id, user: idUser}]);
      let collection = await new AnnotationCollection({image: imageSource.id, showGIS: true}).fetchAll();
      expect(collection).to.have.lengthOf(1);
      let copiedAnnot = collection.get(0);
      expect(copiedAnnot.perimeter).to.equal(annotation.perimeter);
    });

    it('Start review', async function() {
      await imageInstance.review();
      expect(imageInstance.reviewStart).to.exist;
    });

    it('Cancel review', async function() {
      await imageInstance.stopReview(true);
      expect(imageInstance.inReview).to.be.false;
    });

    it('Fetch review statistics', async function() {
      let stats = await imageInstance.fetchReviewStatistics();
      expect(stats).to.be.instanceof(Array);
    });

    it('Download URL', async function() {
      expect(imageInstance.downloadURL).to.be.a('string');
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newInstanceFilename = utils.randomString();
      imageInstance.instanceFilename = newInstanceFilename;
      imageInstance = await imageInstance.update();
      expect(imageInstance).to.be.an.instanceof(ImageInstance);
      expect(imageInstance.instanceFilename).to.equal(newInstanceFilename);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await ImageInstance.delete(id);
    });

    it('Fetch deleted', function() {
      expect(ImageInstance.fetch(id)).to.be.rejected;
    });
  });

  // --------------------

  describe('ImageInstanceCollection', function() {

    let imageInstances;
    let nbImageInstances = 3;
    let baseImages;

    before(async function() {
      baseImages = await utils.getMultipleAbstractImages(nbImageInstances);

      async function createAndConsultImageInstance(baseImage, project) {
        let imageInstance = new ImageInstance({baseImage, project});
        await imageInstance.save();
        await new ImageConsultation({image: imageInstance.id}).save();
        return imageInstance;
      }

      let imageInstancePromises = baseImages.map(baseImage => createAndConsultImageInstance(baseImage, project));
      imageInstances = await Promise.all(imageInstancePromises);
    });

    after(async function() {
      let deletionPromises = imageInstances.map(imageInstance => ImageInstance.delete(imageInstance.id));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new ImageInstanceCollection({filterKey: 'project', filterValue: project}).fetchAll();
        expect(collection).to.be.an.instanceof(ImageInstanceCollection);
        expect(collection).to.have.lengthOf(nbImageInstances);
      });

      it('Fetch (static method)', async function() {
        let collection = await ImageInstanceCollection.fetchAll({filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(ImageInstanceCollection);
        expect(collection).to.have.lengthOf(nbImageInstances);
      });

      it('Fetch with several requests', async function() {
        let collection = await ImageInstanceCollection.fetchAll({nbPerPage: 1, filterKey: 'project', filterValue: project});
        expect(collection).to.be.an.instanceof(ImageInstanceCollection);
        expect(collection).to.have.lengthOf(nbImageInstances);
      });

      it('Fetch without filter', async function() {
        let collection = new ImageInstanceCollection();
        expect(collection.fetchAll()).to.be.rejected;
      });

      it('Fetch last opened', async function() {
        let collection = await ImageInstanceCollection.fetchLastOpened({max: nbImageInstances});
        expect(collection).to.have.lengthOf(nbImageInstances);
        let listId = collection.map(imageInstance => imageInstance.id);
        imageInstances.forEach(image => {
          expect(listId).to.include(image.id);
        });
      });

      it('Fetch last opened in project', async function() {
        let collection = await ImageInstanceCollection.fetchLastOpened({
          project: project,
          user: idUser,
          max: nbImageInstances
        });
        expect(collection).to.have.lengthOf(nbImageInstances);
        let listId = collection.map(item => item.image);
        imageInstances.forEach(image => {
          expect(listId).to.include(image.id);
        });
      });

      it('Fetch light version', async function() {
        let collection = await ImageInstanceCollection.fetchAllLight();
        expect(collection).to.have.lengthOf.at.least(nbImageInstances);
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await ImageInstanceCollection.fetchAll({filterKey: 'project', filterValue: project});
        for(let imageInstance of collection) {
          expect(imageInstance).to.be.an.instanceof(ImageInstance);
        }
      });

      it('Add item to the collection', function() {
        let collection = new ImageInstanceCollection();
        expect(collection).to.have.lengthOf(0);
        collection.push(new ImageInstance());
        expect(collection).to.have.lengthOf(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new ImageInstanceCollection();
        expect(collection.push.bind(collection, {})).to.throw();
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchPage(2);
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        await collection.fetchNextPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new ImageInstanceCollection({nbPerPage, filterKey: 'project', filterValue: project});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).to.have.lengthOf(nbPerPage);
      });
    });

  });

});

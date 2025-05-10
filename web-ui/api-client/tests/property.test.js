import * as utils from './utils.js';
import {Property, PropertyCollection, ImageInstance, User} from '@';

describe('Property', function() {

  let annotation = null;
  let key = 'TEST_JS_KEY';
  let value = utils.randomString();

  let property = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
    annotation = await utils.getAnnotation();
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      property = new Property({key, value}, annotation);
      await property.save();
      expect(property).toBeInstanceOf(Property);
      id = property.id;
      expect(id).toBeDefined();
      expect(property.value).toEqual(value);
    });

    it('Create without providing associated object', async function() {
      let propertyWithoutObject = new Property({key, value});
      expect(propertyWithoutObject.save()).rejects.toThrow();
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedProperty = await Property.fetch(id, annotation);
      expect(fetchedProperty).toBeInstanceOf(Property);
      expect(fetchedProperty.domainIdent).toEqual(annotation.id);
      expect(fetchedProperty.value).toEqual(value);
    });

    it('Fetch with instance method', async function() {
      let fetchedProperty = await new Property({id}, annotation).fetch();
      expect(fetchedProperty).toBeInstanceOf(Property);
      expect(fetchedProperty.domainIdent).toEqual(annotation.id);
      expect(fetchedProperty.value).toEqual(value);
    });

    it('Fetch without providing associated object', function() {
      expect(Property.fetch({id})).rejects.toThrow();
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newValue = utils.randomString();
      property.value = newValue;
      property = await property.update();
      expect(property).toBeInstanceOf(Property);
      expect(property.value).toEqual(newValue);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Property.delete(id, annotation);
    });

    it('Fetch deleted', function() {
      expect(Property.fetch(annotation)).rejects.toThrow();
    });
  });

  // --------------------

  describe('PropertyCollection', function() {
    let nbPropertiesAnnot = 3;
    let nbPropertiesImage = 1;
    let properties;

    let image;

    beforeAll(async function() {
      image = await ImageInstance.fetch(annotation.image);

      let propertiesPromises = [];
      for(let i = 0; i < nbPropertiesAnnot; i++) {
        propertiesPromises.push(new Property({key, value: utils.randomString()}, annotation).save());
      }
      for(let i = 0; i < nbPropertiesImage; i++) {
        propertiesPromises.push(new Property({key, value: utils.randomString()}, image).save());
      }
      properties = await Promise.all(propertiesPromises);
    });

    afterAll(async function() {
      let deletionPromises = properties.map(property => Property.delete(property.id, annotation));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', function() {
      it('Fetch (instance method)', async function() {
        let collection = await new PropertyCollection({object: annotation}).fetchAll();
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesAnnot);
      });

      it('Fetch (static method)', async function() {
        let collection = await PropertyCollection.fetchAll({object: image});
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesImage);
      });

      it('Fetch with several requests', async function() {
        let collection = await PropertyCollection.fetchAll({nbPerPage: 1, object: annotation});
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesAnnot);
      });

      it('Fetch without associated object', async function() {
        expect(PropertyCollection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', function() {
      it('Iterate through', async function() {
        let collection = await PropertyCollection.fetchAll({object: annotation});
        for(let property of collection) {
          expect(property).toBeInstanceOf(Property);
        }
      });

      it('Add item to the collection', function() {
        let collection = new PropertyCollection({object: annotation});
        expect(collection).toHaveLength(0);
        collection.push(new Property({}, annotation));
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', function() {
        let collection = new PropertyCollection(annotation);
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Specific operations', function() {
      it('Fetch keys of annotation properties', async function() {
        let keys = await PropertyCollection.fetchKeysAnnotationProperties(null, image.id);
        expect(keys).toBeInstanceOf(Array);
        expect(keys).toHaveLength(1);
        expect(keys[0]).toEqual(key);
      });

      it('Fetch keys of image properties', async function() {
        let keys = await PropertyCollection.fetchKeysImageProperties(image.project);
        expect(keys).toBeInstanceOf(Array);
        expect(keys).toHaveLength(1);
        expect(keys[0]).toEqual(key);
      });

      it('Fetch the properties positions and values', async function() {
        let currentUser = await User.fetchCurrent();
        let props = await PropertyCollection.fetchPropertiesValuesAndPositions(currentUser.id, image.id, key);
        let listId = props.map(item => item.idAnnotation);
        let listValues = props.map(item => item.value);
        listId.forEach(id => {
          expect(id).toEqual(annotation.id);
        });
        properties.forEach(prop => {
          if(prop.domainIdent === annotation.id && prop.domainClassName === annotation.class) {
            expect(listValues).toContain(prop.value);
          }
        });
      });
    });

    describe('Pagination', function() {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async function() {
        let collection = new PropertyCollection({nbPerPage, object: annotation});
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async function() {
        let collection = new PropertyCollection({nbPerPage, object: annotation});
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async function() {
        let collection = new PropertyCollection({nbPerPage, object: annotation});
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

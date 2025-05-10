import * as utils from './utils.js';
import { Property, PropertyCollection, ImageInstance, User } from '@/index.js';

describe('Property', () => {

  let annotation = null;
  let key = 'TEST_JS_KEY';
  let value = utils.randomString();

  let property = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
    annotation = await utils.getAnnotation();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      property = new Property({ key, value }, annotation);
      await property.save();
      expect(property).toBeInstanceOf(Property);
      id = property.id;
      expect(id).toBeDefined();
      expect(property.value).toEqual(value);
    });

    it('Create without providing associated object', async () => {
      let propertyWithoutObject = new Property({ key, value });
      expect(propertyWithoutObject.save()).rejects.toThrow();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedProperty = await Property.fetch(id, annotation);
      expect(fetchedProperty).toBeInstanceOf(Property);
      expect(fetchedProperty.domainIdent).toEqual(annotation.id);
      expect(fetchedProperty.value).toEqual(value);
    });

    it('Fetch with instance method', async () => {
      let fetchedProperty = await new Property({ id }, annotation).fetch();
      expect(fetchedProperty).toBeInstanceOf(Property);
      expect(fetchedProperty.domainIdent).toEqual(annotation.id);
      expect(fetchedProperty.value).toEqual(value);
    });

    it('Fetch without providing associated object', () => {
      expect(Property.fetch({ id })).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newValue = utils.randomString();
      property.value = newValue;
      property = await property.update();
      expect(property).toBeInstanceOf(Property);
      expect(property.value).toEqual(newValue);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Property.delete(id, annotation);
    });

    it('Fetch deleted', () => {
      expect(Property.fetch(annotation)).rejects.toThrow();
    });
  });

  // --------------------

  describe('PropertyCollection', () => {
    let nbPropertiesAnnot = 3;
    let nbPropertiesImage = 1;
    let properties;

    let image;

    beforeAll(async () => {
      image = await ImageInstance.fetch(annotation.image);

      let propertiesPromises = [];
      for (let i = 0; i < nbPropertiesAnnot; i++) {
        propertiesPromises.push(new Property({ key, value: utils.randomString() }, annotation).save());
      }
      for (let i = 0; i < nbPropertiesImage; i++) {
        propertiesPromises.push(new Property({ key, value: utils.randomString() }, image).save());
      }
      properties = await Promise.all(propertiesPromises);
    });

    afterAll(async () => {
      let deletionPromises = properties.map(property => Property.delete(property.id, annotation));
      await Promise.all(deletionPromises);
    });

    describe('Fetch', () => {
      it('Fetch (instance method)', async () => {
        let collection = await new PropertyCollection({ object: annotation }).fetchAll();
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesAnnot);
      });

      it('Fetch (static method)', async () => {
        let collection = await PropertyCollection.fetchAll({ object: image });
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesImage);
      });

      it('Fetch with several requests', async () => {
        let collection = await PropertyCollection.fetchAll({ nbPerPage: 1, object: annotation });
        expect(collection).toBeInstanceOf(PropertyCollection);
        expect(collection).toHaveLength(nbPropertiesAnnot);
      });

      it('Fetch without associated object', async () => {
        expect(PropertyCollection.fetchAll()).rejects.toThrow();
      });
    });

    describe('Working with the collection', () => {
      it('Iterate through', async () => {
        let collection = await PropertyCollection.fetchAll({ object: annotation });
        for (let property of collection) {
          expect(property).toBeInstanceOf(Property);
        }
      });

      it('Add item to the collection', () => {
        let collection = new PropertyCollection({ object: annotation });
        expect(collection).toHaveLength(0);
        collection.push(new Property({}, annotation));
        expect(collection).toHaveLength(1);
      });

      it('Add arbitrary object to the collection', () => {
        let collection = new PropertyCollection(annotation);
        expect(collection.push.bind(collection, {})).toThrow();
      });
    });

    describe('Specific operations', () => {
      it('Fetch keys of annotation properties', async () => {
        let keys = await PropertyCollection.fetchKeysAnnotationProperties(null, image.id);
        expect(keys).toBeInstanceOf(Array);
        expect(keys).toHaveLength(1);
        expect(keys[0]).toEqual(key);
      });

      it('Fetch keys of image properties', async () => {
        let keys = await PropertyCollection.fetchKeysImageProperties(image.project);
        expect(keys).toBeInstanceOf(Array);
        expect(keys).toHaveLength(1);
        expect(keys[0]).toEqual(key);
      });

      it('Fetch the properties positions and values', async () => {
        let currentUser = await User.fetchCurrent();
        let props = await PropertyCollection.fetchPropertiesValuesAndPositions(currentUser.id, image.id, key);
        let listId = props.map(item => item.idAnnotation);
        let listValues = props.map(item => item.value);
        listId.forEach(id => {
          expect(id).toEqual(annotation.id);
        });
        properties.forEach(prop => {
          if (prop.domainIdent === annotation.id && prop.domainClassName === annotation.class) {
            expect(listValues).toContain(prop.value);
          }
        });
      });
    });

    describe('Pagination', () => {
      let nbPerPage = 1;

      it('Fetch arbitrary page', async () => {
        let collection = new PropertyCollection({ nbPerPage, object: annotation });
        await collection.fetchPage(2);
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch next page', async () => {
        let collection = new PropertyCollection({ nbPerPage, object: annotation });
        await collection.fetchNextPage();
        expect(collection).toHaveLength(nbPerPage);
      });

      it('Fetch previous page', async () => {
        let collection = new PropertyCollection({ nbPerPage, object: annotation });
        collection.curPage = 2;
        await collection.fetchPreviousPage();
        expect(collection).toHaveLength(nbPerPage);
      });
    });

  });

});

import * as utils from './utils.js';
import {Description} from '@/index.js';

describe('Description', function() {

  let annotation = null;
  let data = utils.randomString();

  let description = null;

  beforeAll(async function() {
    await utils.connect();
    annotation = await utils.getAnnotation();
  });

  afterAll(async function() {
    await utils.cleanData();
  });

  describe('Create', function() {
    it('Create', async function() {
      description = new Description({data}, annotation);
      await description.save();
      expect(description).toBeInstanceOf(Description);
      expect(description.id).toBebove(0);
      expect(description.data).toEqual(data);
    });

    it('Create without providing associated object', async function() {
      let descriptionWithoutObject = new Description({data});
      expect(descriptionWithoutObject.save()).rejects.toThrow();
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedDescription = await Description.fetch(annotation);
      expect(fetchedDescription).toBeInstanceOf(Description);
      expect(fetchedDescription.domainIdent).toEqual(annotation.id);
      expect(fetchedDescription.data).toEqual(data);
    });

    it('Fetch with instance method', async function() {
      let fetchedDescription = await new Description({}, annotation).fetch();
      expect(fetchedDescription).toBeInstanceOf(Description);
      expect(fetchedDescription.domainIdent).toEqual(annotation.id);
      expect(fetchedDescription.data).toEqual(data);
    });

    it('Fetch without providing associated object', function() {
      expect(Description.fetch({})).rejects.toThrow();
    });
  });

  describe('Update', function() {
    it('Update', async function() {
      let newData = utils.randomString();
      description.data = newData;
      description = await description.update();
      expect(description).toBeInstanceOf(Description);
      expect(description.data).toEqual(newData);
    });
  });

  describe('Delete', function() {
    it('Delete', async function() {
      await Description.delete(annotation);
    });

    it('Fetch deleted', function() {
      expect(Description.fetch(annotation)).rejects.toThrow();
    });
  });

});

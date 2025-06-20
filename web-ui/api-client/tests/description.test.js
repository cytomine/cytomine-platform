import * as utils from './utils.js';
import {Description} from '@/index.js';

describe('Description', () => {

  let annotation = null;
  let data = utils.randomString();

  let description = null;

  beforeAll(async () => {
    await utils.connect();
    annotation = await utils.getAnnotation();
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      description = new Description({data}, annotation);
      await description.save();
      expect(description).toBeInstanceOf(Description);
      expect(description.id).toBeGreaterThan(0);
      expect(description.data).toEqual(data);
    });

    it('Create without providing associated object', async () => {
      let descriptionWithoutObject = new Description({data});
      await expect(descriptionWithoutObject.save()).rejects.toThrow();
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedDescription = await Description.fetch(annotation);
      expect(fetchedDescription).toBeInstanceOf(Description);
      expect(fetchedDescription.domainIdent).toEqual(annotation.id);
      expect(fetchedDescription.data).toEqual(data);
    });

    it('Fetch with instance method', async () => {
      let fetchedDescription = await new Description({}, annotation).fetch();
      expect(fetchedDescription).toBeInstanceOf(Description);
      expect(fetchedDescription.domainIdent).toEqual(annotation.id);
      expect(fetchedDescription.data).toEqual(data);
    });

    it('Fetch without providing associated object', async () => {
      await expect(Description.fetch({})).rejects.toThrow();
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newData = utils.randomString();
      description.data = newData;
      description = await description.update();
      expect(description).toBeInstanceOf(Description);
      expect(description.data).toEqual(newData);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await Description.delete(annotation);
    });

    it('Fetch deleted', async () => {
      await expect(Description.fetch(annotation)).rejects.toThrow();
    });
  });

});

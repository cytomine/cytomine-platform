import * as utils from './utils.js';
import {User} from '@/index';
import {AbstractImage} from '@/index.js';

describe('AbstractImage', () => {
  let originalFilename = utils.randomString();
  let uploadedFile;
  let user;
  let storage;

  let abstractImage = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();

    ({id: user} = await User.fetchCurrent());
    ({id: storage} = await utils.getStorage({user}));
    ({id: uploadedFile} = await utils.getUploadedFile({storage, originalFilename}));
  });

  afterAll(async () => {
    await utils.cleanData();
  });

  describe('Create', () => {
    it('Create', async () => {
      abstractImage = new AbstractImage({originalFilename, uploadedFile, width: 1000, height: 1000});
      abstractImage = await abstractImage.save();
      id = abstractImage.id;
      expect(abstractImage).toBeInstanceOf(AbstractImage);
      expect(abstractImage.originalFilename).toBe(originalFilename);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedImage = await AbstractImage.fetch(id);
      expect(fetchedImage).toBeInstanceOf(AbstractImage);
      expect(fetchedImage.originalFilename).toEqual(originalFilename);
    });

    it('Fetch with instance method', async () => {
      let fetchedImage = await new AbstractImage({id}).fetch();
      expect(fetchedImage).toBeInstanceOf(AbstractImage);
      expect(fetchedImage.originalFilename).toEqual(originalFilename);
    });

    it('Fetch with wrong ID', async () => {
      await expect(AbstractImage.fetch(0)).rejects.toThrow();
    });
  });

  describe('Specific operations', () => {
    it('Get uploader', async () => {
      let user = await abstractImage.fetchUploader();
      expect(user).toBeInstanceOf(User);

      let currentUser = await User.fetchCurrent();
      expect(user.id).toEqual(currentUser.id);
    });
  });

  describe('Update', () => {
    it('Update', async () => {
      let newFilename = utils.randomString();
      abstractImage.originalFilename = newFilename;
      await abstractImage.update();
      expect(abstractImage).toBeInstanceOf(AbstractImage);
      expect(abstractImage.originalFilename).toEqual(newFilename);
    });
  });

  describe('Delete', () => {
    it('Delete', async () => {
      await AbstractImage.delete(id);
    });

    it('Fetch a deleted element', async () => {
      await expect(AbstractImage.fetch(id)).rejects.toThrow();
    });
  });
});

import * as utils from './utils.js';
import { Cytomine, User } from '@/index.js';
import config from './config.js';

describe('Cytomine', () => {
  beforeAll(() => {
    new Cytomine(config.host);
  });

  describe('Login/logout', () => {
    it('Login', async () => {
      await Cytomine.instance.login(config.username, config.password);
    });

    it('Logout', async () => {
      await Cytomine.instance.logout();
    });
  });

  describe('UI config', () => {
    let project;

    beforeAll(async () => {
      await utils.connect();
      project = await utils.getProject();
    });

    afterAll(async () => {
      await utils.cleanData();
    });

    it('Global UI config', async () => {
      let config = await Cytomine.instance.fetchUIConfigCurrentUser();
      expect(config).toBeInstanceOf(Object);
      for (let prop in config) {
        expect(typeof config[prop]).toBe('boolean');
      }
    });

    it('Project UI config', async () => {
      let config = await Cytomine.instance.fetchUIConfigCurrentUser(project.id);
      expect(config).toBeInstanceOf(Object);
      for (let prop in config) {
        expect(typeof config[prop]).toBe('boolean');
      }
    });
  });

  describe('Switch user', () => {
    let otherUser;

    beforeAll(async () => {
      await utils.connect(true);
      otherUser = await utils.getUser();
    });

    afterAll(async () => {
      await utils.cleanData();
    });

    it('Switch to another user account', async () => {
      await Cytomine.instance.switchUser(otherUser.username);
      let currentUser = await User.fetchCurrent();
      expect(currentUser.id).toEqual(otherUser.id);
    });

    it('Switch back to real user account', async () => {
      await Cytomine.instance.stopSwitchUser();
      let currentUser = await User.fetchCurrent();
      expect(currentUser.id).not.toBe(otherUser.id);
    });
  });

  describe('Signature', () => {
    beforeAll(async () => {
      await utils.connect();
    });

    it('Signature', async () => {
      let signature = await Cytomine.instance.fetchSignature();
      expect(typeof signature).toBe('string');
    });
  });

  describe('Forgot credentials', () => {
    let user;

    beforeAll(async () => {
      await utils.connect();
      user = await utils.getUser();
    });

    afterAll(async () => {
      await utils.cleanData();
    });

    it('Forgot username', async () => {
      await Cytomine.instance.forgotUsername(user.email);
    });

    it('Forgot password', async () => {
      await Cytomine.instance.forgotPassword(user.username);
    });
  });

  describe('Stats', () => {
    beforeAll(async () => {
      await utils.connect();
    });

    it('Total counts', async () => {
      let counts = await Cytomine.instance.fetchTotalCounts();
      expect(counts).toBeInstanceOf(Object);
      for (let prop in counts) {
        expect(typeof counts[prop]).toBe('number');
      }
    });

    it('Current stats', async () => {
      let currentStats = await Cytomine.instance.fetchCurrentStats();
      expect(currentStats).toBeInstanceOf(Object);
      expect(typeof currentStats.users).toBe('number');
      expect(typeof currentStats.projects).toBe('number');
    });

    it('Storage stats', async () => {
      let storageStats = await Cytomine.instance.fetchStorageStats();
      expect(storageStats).toBeInstanceOf(Object);
      for (let prop in storageStats) {
        expect(typeof storageStats[prop]).toBe('number');
      }
    });
  });
});

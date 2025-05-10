import * as utils from './utils.js';
import {Cytomine, User} from '@/index.js';
import config from './config.js';

describe('Cytomine', function() {
  beforeAll(function() {
    new Cytomine(config.host);
  });

  describe('Login/logout', function() {
    it('Login', async function() {
      await Cytomine.instance.login(config.username, config.password);
    });

    it('Logout', async function() {
      await Cytomine.instance.logout();
    });
  });

  describe('UI config', function() {
    let project;

    beforeAll(async function() {
      await utils.connect();
      project = await utils.getProject();
    });

    afterAll(async function() {
      await utils.cleanData();
    });

    it('Global UI config', async function() {
      let config = await Cytomine.instance.fetchUIConfigCurrentUser();
      expect(config).toBeInstanceOf(Object);
      for(let prop in config){
        expect(config[prop]).toBe('boolean');
      }
    });

    it('Project UI config', async function() {
      let config = await Cytomine.instance.fetchUIConfigCurrentUser(project.id);
      expect(config).toBeInstanceOf(Object);
      for(let prop in config){
        expect(config[prop]).toBe('boolean');
      }
    });
  });

  describe('Switch user', function() {
    let otherUser;

    beforeAll(async function() {
      await utils.connect(true);
      otherUser = await utils.getUser();
    });

    afterAll(async function() {
      await utils.cleanData();
    });

    it('Switch to another user account', async function() {
      await Cytomine.instance.switchUser(otherUser.username);
      let currentUser = await User.fetchCurrent();
      expect(currentUser.id).toEqual(otherUser.id);
    });

    it('Switch back to real user account', async function() {
      await Cytomine.instance.stopSwitchUser();
      let currentUser = await User.fetchCurrent();
      expect(currentUser.id).not.toBe(otherUser.id);
    });
  });

  describe('Signature', function() {
    beforeAll(async function() {
      await utils.connect();
    });

    it('Signature', async function() {
      let signature = await Cytomine.instance.fetchSignature();
      expect(signature).toBe('string');
    });
  });

  describe('Forgot credentials', function() {
    let user;

    beforeAll(async function() {
      await utils.connect();
      user = await utils.getUser();
    });

    afterAll(async function() {
      await utils.cleanData();
    });

    it('Forgot username', async function() {
      await Cytomine.instance.forgotUsername(user.email);
    });

    it('Forgot password', async function() {
      await Cytomine.instance.forgotPassword(user.username);
    });
  });

  describe('Stats', function() {
    beforeAll(async function() {
      await utils.connect();
    });

    it('Total counts', async function() {
      let counts = await Cytomine.instance.fetchTotalCounts();
      expect(counts).toBeInstanceOf(Object);
      for(let prop in counts){
        expect(counts[prop]).toBe('number');
      }
    });

    it('Current stats', async function() {
      let currentStats = await Cytomine.instance.fetchCurrentStats();
      expect(currentStats).toBeInstanceOf(Object);
      expect(currentStats.users).toBe('number');
      expect(currentStats.projects).toBe('number');
    });

    it('Storage stats', async function() {
      let storageStats = await Cytomine.instance.fetchStorageStats();
      expect(storageStats).toBeInstanceOf(Object);
      for(let prop in storageStats){
        expect(storageStats[prop]).toBe('number');
      }
    });
  });
});

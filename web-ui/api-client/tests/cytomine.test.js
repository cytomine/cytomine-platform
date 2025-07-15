import * as utils from './utils.js';
import {Cytomine} from '@/index.js';
import config from './config.js';

describe('Cytomine', () => {
  beforeAll(() => {
    new Cytomine(config.host);
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

  describe('Signature', () => {
    beforeAll(async () => {
      await utils.connect();
    });

    it('Signature', async () => {
      let signature = await Cytomine.instance.fetchSignature();
      expect(typeof signature).toBe('string');
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

    // Skipped because PIMS has to be spawned
    it.skip('Storage stats', async () => {
      let storageStats = await Cytomine.instance.fetchStorageStats();
      expect(storageStats).toBeInstanceOf(Object);
      for (let prop in storageStats) {
        expect(typeof storageStats[prop]).toBe('number');
      }
    });
  });
});

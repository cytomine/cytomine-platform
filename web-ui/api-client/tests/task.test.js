import * as utils from './utils.js';
import {Task} from '@/index.js';

describe('Task', () => {
  let task = null;
  let id = 0;

  beforeAll(async () => {
    await utils.connect();
  });

  describe('Create', () => {
    it('Create', async () => {
      task = new Task();
      await task.save();
      id = task.id;
      expect(task).toBeInstanceOf(Task);
      expect(task.id).toBeGreaterThan(0);
    });
  });

  describe('Fetch', () => {
    it('Fetch with static method', async () => {
      let fetchedTask = await Task.fetch(id);
      expect(fetchedTask).toBeInstanceOf(Task);
    });

    it('Fetch with instance method', async () => {
      let fetchedTask = await new Task({id}).fetch();
      expect(fetchedTask).toBeInstanceOf(Task);
    });
  });

  describe('Update', () => {
    it('Update', () => {
      expect(task.update.bind(task)).toThrow();
    });
  });

  describe('Delete', () => {
    it('Delete', () => {
      expect(task.delete.bind(task)).toThrow();
    });
  });

});

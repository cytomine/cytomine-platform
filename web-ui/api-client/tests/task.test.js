import * as utils from './utils.js';
import {Task} from '@';

describe('Task', function() {
  let task = null;
  let id = 0;

  beforeAll(async function() {
    await utils.connect();
  });

  describe('Create', function() {
    it('Create', async function() {
      task = new Task();
      await task.save();
      id = task.id;
      expect(task).toBeInstanceOf(Task);
      expect(task.id).toBeGreaterThan(0);
    });
  });

  describe('Fetch', function() {
    it('Fetch with static method', async function() {
      let fetchedTask = await Task.fetch(id);
      expect(fetchedTask).toBeInstanceOf(Task);
    });

    it('Fetch with instance method', async function() {
      let fetchedTask = await new Task({id}).fetch();
      expect(fetchedTask).toBeInstanceOf(Task);
    });
  });

  describe('Update', function() {
    it('Update', function() {
      expect(task.update.bind(task)).toThrow();
    });
  });

  describe('Delete', function() {
    it('Delete', function() {
      expect(task.delete.bind(task)).toThrow();
    });
  });

});

import Model from './model';
import Task from './task';
import {Cytomine} from 'cytomine-client';


export default class TaskRun extends Model {
  /** @inheritdoc */
  static get callbackIdentifier() {
    return '/app-engine/project/${project}/task-runs'; // not used
  }

  get uri() {
    if (!this.project) {
      throw new Error('TaskRun.project is required');
    }
    if (this.isNew()) {
      return `/app-engine/project/${this.project}/task-runs`;
      //   return `${this.callbackIdentifier}.json`;
    } else {
      //   return `${this.callbackIdentifier}/${this.id}.json`;
      return `/app-engine/project/${this.project}/task-runs/${this.id}`;
    }
  }

  _initProperties() {
    super._initProperties();
    this.project = null;
    this.task = new Task();
    this.state = null;
    /* eslint-disable */
    this.created_at = null;
    this.updated_at = null;
    this.last_state_transition_at = null;
    /* eslint-enable */
  }

  static async fetchByProject(projectId) {
    let {data} = await Cytomine.instance.api.get(`project/${projectId}/task-runs`);
    return data;
  }

  // Step-2: Provision task / user inputs
  async batchProvisionTask(params) {
    let {data} = await Cytomine.instance.api.put(`${this.uri}/input-provisions`, params);
    return data;
  }

  async singleProvisionTask(paramName, param) {
    let {data} = Cytomine.instance.api.put(`${this.uri}/input-provisions/${paramName}`, param);
    return data;
  }

  // Step-3 Run/Execute the Provisioned Task
  async start() {
    let {data} = await Cytomine.instance.api.post(`${this.uri}/state-actions`, {'desired': 'RUNNING'});
    return data;
  }

  async fetchInputs() {
    let {data} = await Cytomine.instance.api.get(`${this.uri}/inputs`);
    return data;
  }

  async fetchOutputs() {
    let {data} = await Cytomine.instance.api.get(`${this.uri}/outputs`);
    return data;
  }

  async fetchSingleIO(parameterName, type) {
    let {data} = await Cytomine.instance.api.get(`${this.uri}/${type}/${parameterName}`, {responseType: 'arraybuffer'});
    return data;
  }
}

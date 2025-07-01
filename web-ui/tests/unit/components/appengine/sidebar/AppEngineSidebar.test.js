import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import AppEngineSideBar from '@/components/appengine/sidebar/AppEngineSidebar.vue';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

jest.mock('@/utils/appengine/task', () => ({
  fetchAll: jest.fn(() => Promise.resolve([
    {namespace: 'namespace1', version: '0.1.0', name: 'Task 1', id: 1},
    {namespace: 'namespace2', version: '0.5.0', name: 'Task 2', id: 2},
  ])),
  /* eslint-disable */
  fetchTaskInputs: jest.fn(() => Promise.resolve([
    {param_name: 'input1'},
    {param_name: 'input2'},
  ])),
  fetchTaskOutputs: jest.fn(() => Promise.resolve([
    {param_name: 'output1'},
    {param_name: 'output2'},
  ])),
  /* eslint-enable */
  fetchTaskRunStatus: jest.fn(() => Promise.resolve([
    {id: 'uuid-1'},
    {id: 'uuid-2'},
  ])),
}));

jest.mock('@/utils/appengine/task-run', () => {
  const mockTaskRun = jest.fn().mockImplementation((resource) => ({
    ...resource,
    project: null,
    state: null,
  }));

  mockTaskRun.fetchByProject = jest.fn(() => Promise.resolve([
    {taskRunId: '1', project: 'project1'},
    {taskRunId: '2', project: 'project2'},
  ]));

  return {
    __esModule: true,
    default: mockTaskRun,
  };
});

describe('AppEngineSideBar.vue', () => {
  const fetchTasksSpy = jest.spyOn(AppEngineSideBar.methods, 'fetchTasks');
  const fetchTaskRunsSpy = jest.spyOn(AppEngineSideBar.methods, 'fetchTaskRuns');

  const localVue = createLocalVue();
  localVue.use(Buefy);

  let wrapper;

  beforeEach(async () => {
    fetchTasksSpy.mockClear();
    fetchTaskRunsSpy.mockClear();

    wrapper = shallowMount(AppEngineSideBar, {
      localVue,
      mocks: {
        $t: (key) => key
      },
      computed: {
        currentProject: () => ({id: 42}),
      },
    });
  });

  it('The component should be rendered and show the task options', () => {
    const headers = wrapper.findAll('header');
    expect(headers.length).toBe(2);
    expect(headers.at(0).text()).toBe('app-engine.execute-a-task');
    expect(headers.at(1).text()).toBe('app-engine.previous-task-runs');

    const taskOptions = wrapper.findAll('option');
    expect(taskOptions.length).toBe(2);
    expect(taskOptions.at(0).text()).toBe('Task 1 (0.1.0)');
    expect(taskOptions.at(1).text()).toBe('Task 2 (0.5.0)');

    expect(wrapper.find('h5').text()).toBe('app-engine.runs.title');
  });

  it('The component should display the task name and description when a task is selected', async () => {
    await wrapper.setData({selectedTask: {namespace: 'namespace1', version: '0.1.0', name: 'Task 1', id: 1}});

    expect(wrapper.find('.content p').text()).toBe('Task 1 (0.1.0)');

    const descriptionField = wrapper.find('.no-description');
    expect(descriptionField.exists()).toBe(true);
    expect(descriptionField.text()).toBe('app-engine.task.no-description');
  });

  it('The component should fetch data on component created', async () => {
    expect(fetchTasksSpy).toHaveBeenCalled();
    expect(fetchTasksSpy).toHaveBeenCalledTimes(1);
    expect(fetchTaskRunsSpy).toHaveBeenCalled();
    expect(fetchTaskRunsSpy).toHaveBeenCalledTimes(1);
  });

  it('The component should update tracked task runs every 2 seconds', async () => {
    jest.useFakeTimers();
    jest.advanceTimersByTime(2000);

    expect(fetchTasksSpy).toHaveBeenCalled();
    expect(fetchTaskRunsSpy).toHaveBeenCalled();

    jest.useRealTimers();
  });
});

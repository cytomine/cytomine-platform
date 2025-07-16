import {shallowMount, createLocalVue} from '@vue/test-utils';
import Buefy from 'buefy';

import TaskIoForm from '@/components/appengine/forms/TaskIoForm.vue';
import AppEngineField from '@/components/appengine/forms/fields/AppEngineField';
import Task from '@/utils/appengine/task';

jest.mock('@/utils/appengine/task');

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('TaskIoForm.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);

  const mockTask = {
    namespace: 'mock-namespace',
    version: '1.0.0',
  };

  const mockInputs = [
    {id: 1, name: 'input1', type: {id: 'string'}, default: 'hello'},
    {id: 2, name: 'input2', type: {id: 'number'}, default: 4.2},
  ];

  let wrapper;

  beforeEach(() => {
    Task.fetchTaskInputs.mockResolvedValue(mockInputs);
    Task.createTaskRun.mockResolvedValue({id: 123});
    Task.batchProvisionTask.mockResolvedValue();
    Task.runTask.mockResolvedValue({id: 123});

    wrapper = shallowMount(TaskIoForm, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      propsData: {
        task: mockTask,
        projectId: 1,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    const headers = wrapper.findAll('h3');
    expect(headers.length).toBe(1);
    expect(headers.at(0).text()).toBe('app-engine.ae-run-task');

    expect(wrapper.findAllComponents(AppEngineField).length).toBe(mockInputs.length);

    const buttons = wrapper.findAll('b-button-stub');
    expect(buttons.length).toBe(2);
    expect(buttons.at(0).text()).toBe('button-clear');
    expect(buttons.at(1).text()).toBe('app-engine.ae-run-task');
  });
});

import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';
import moment from 'moment';

import ActivityLogs from '@/components/utils/ActivityLogs';
import ActivityLogsItem from '@/components/utils/ActivityLogsItem';
import {Project} from 'cytomine-client';

jest.mock('cytomine-client', () => ({
  Project: {
    fetchCommandHistory: jest.fn()
  }
}));

describe('ActivityLogs.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);

  localVue.component('b-message', {
    template: '<div><slot/></div>'
  });

  const mocks = {
    $t: message => message
  };

  let wrapper;

  beforeAll(() => {
    localVue.filter('moment', jest.fn((value, format) => moment(Number(value)).format(format)));
  });

  beforeEach(() => {
    Project.fetchCommandHistory.mockResolvedValue([]);

    wrapper = shallowMount(ActivityLogs, {
      localVue,
      mocks,
      propsData: {
        startDate: 1622505600000,
        endDate: 1625097600000,
        idUser: 1,
        project: {id: 123}
      }
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render the loading state correctly', async () => {
    Project.fetchCommandHistory.mockResolvedValue([
      {id: 1, created: '1622505600000', message: 'Action 1'}
    ]);

    await wrapper.vm.loadActions();
    await wrapper.vm.$nextTick();

    const dayHeaders = wrapper.findAll('h4');
    const monthHeaders = wrapper.findAll('h3');

    expect(dayHeaders.length).toBe(1);
    expect(monthHeaders.length).toBe(1);
    expect(wrapper.find('h3').text()).toBe('June 2021');
    expect(wrapper.findComponent(ActivityLogsItem).exists()).toBe(true);
  });

  it('should render error message when "error" is true', async () => {
    wrapper.setData({error: true});
    await wrapper.vm.$nextTick();
    const errorMessage = wrapper.find('p');

    expect(errorMessage.exists()).toBe(true);
    expect(errorMessage.text()).toContain(wrapper.vm.$t('error'));
    expect(errorMessage.text()).toContain(wrapper.vm.$t('unexpected-error-info-message'));
  });

  it('should render a list of actions grouped by month and day', async () => {
    Project.fetchCommandHistory.mockResolvedValue([
      {id: 1, created: '1622505600000', message: 'Action 1'},
      {id: 2, created: '1622592000000', message: 'Action 2'}
    ]);

    await wrapper.vm.loadActions();
    await wrapper.vm.$nextTick();

    const monthHeaders = wrapper.findAll('h3');
    expect(monthHeaders.length).toBe(1);
    expect(monthHeaders.at(0).text()).toContain('June 2021');

    const dayHeaders = wrapper.findAll('h4');
    expect(dayHeaders.length).toBe(2);
    expect(dayHeaders.at(0).text()).toContain('Jun 1, 2021');

    const logItems = wrapper.findAllComponents(ActivityLogsItem);
    expect(logItems.length).toBe(2);
  });

  it('should handle loadActions correctly when no actions are available', async () => {
    await wrapper.vm.loadActions();
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.loadedAllActions).toBe(true);
  });

  it('should set loadedAllActions to true when no more actions are loaded', async () => {
    const actions = new Array(50)
      .fill(null)
      .map((_, index) => ({
        id: index + 1, // Generate unique ID
        created: '1625097600000',
        message: 'Action'
      }));
    Project.fetchCommandHistory.mockResolvedValue(actions);

    await wrapper.vm.loadActions();
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.loadedAllActions).toBe(true);
  });

  it('should reset data when loadActions is called with "append" to false', async () => {
    const actions = [{id: 1, created: '1625097600000', message: 'Action'}];
    Project.fetchCommandHistory.mockResolvedValue(actions);

    await wrapper.vm.loadActions(false);
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.actions).toEqual(actions);
    expect(wrapper.vm.offset).toBe(50);
    expect(wrapper.vm.loadedAllActions).toBe(true);
  });
});

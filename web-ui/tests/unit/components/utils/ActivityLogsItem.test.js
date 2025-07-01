import {createLocalVue, shallowMount} from '@vue/test-utils';
import moment from 'moment';

import ActivityLogsItem from '@/components/utils/ActivityLogsItem';
import VueRouter from 'vue-router';

jest.mock('@/utils/token-utils', () => ({
  appendShortTermToken: jest.fn((url, token) => `${url}?token=${token}`)
}));

jest.mock('@/utils/store-helpers', () => ({
  get: jest.fn(() => 'mock-token')
}));

describe('ActivityLogsItem.vue', () => {
  let wrapper;
  const action = {
    id: 1,
    created: '1626874800000', // Timestamp for 07/21/2021
    serviceName: 'userAnnotationService',
    className: 'be.cytomine.command.AddCommand',
    data: JSON.stringify({
      id: 1,
      url: 'https://example.com/image',
      project: 123,
      image: 456
    }),
    message: 'Added a new annotation'
  };

  const localVue = createLocalVue();
  localVue.use(VueRouter);
  localVue.component('router-link', {template: '<a><slot></slot></a>'});
  localVue.component('router-view', {template: '<div><slot></slot></div>'});

  beforeAll(() => {
    localVue.filter('moment', jest.fn((value, format) => moment.utc(Number(value)).format(format)));
  });

  beforeEach(() => {
    wrapper = shallowMount(ActivityLogsItem, {
      localVue,
      propsData: {
        action
      },
      computed: {
        shortTermToken: () => 'mock-token'
      },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render the correct timestamp and message', () => {
    const strongTag = wrapper.find('strong');
    const messageSpan = wrapper.find('.content');

    expect(strongTag.text()).toBe('7/21/2021 1:40:00 PM:');
    expect(messageSpan.text()).toContain('Added a new annotation');
  });

  it('should create the correct annotation route and preview', async () => {
    wrapper.vm.$options.created[0].call(wrapper.vm);
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.route).toBe('/project/123/image/456/annotation/1');
    expect(wrapper.vm.previewUrl).toBe('https://example.com/image?maxSize=125&complete=true&thickness=2&increaseArea=1.25&draw=true');
  });

  it('should toggle preview when hovering over', async () => {
    expect(wrapper.vm.showPreview).toBe(false);

    // Simulate mouse enter (hover)
    wrapper.vm.enter();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.showPreview).toBe(true);

    // Simulate mouse leave
    wrapper.vm.leave();
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.showPreview).toBe(false);
  });
});

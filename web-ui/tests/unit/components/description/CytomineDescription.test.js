import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';
import Vuex from 'vuex';

import CytomineDescription from '@/components/description/CytomineDescription';

jest.mock('cytomine-client', () => ({
  Description: {
    fetch: jest.fn()
  }
}));

jest.mock('@/utils/constants.js', () => ({
  STOP_PREVIEW_KEYWORD: 'STOP_PREVIEW'
}));

describe('CytomineDescription.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);
  localVue.use(Vuex);

  let actions;
  let store;
  let wrapper;

  beforeEach(() => {
    actions = {
      fetchDescription: jest.fn()
    };
    store = new Vuex.Store({
      actions
    });

    wrapper = shallowMount(CytomineDescription, {
      localVue,
      store,
      mocks: {
        $t: (message) => message
      },
      propsData: {
        object: {id: 1},
        canEdit: true,
        maxPreviewLength: 100
      }
    });
  });

  afterEach(() => {
    wrapper.destroy();
  });

  it('should render the component correctly', () => {
    expect(wrapper.exists()).toBe(true);
  });

  it('should render a loading spinner when loading is true', () => {
    wrapper.setData({loading: true});

    expect(wrapper.findComponent({name: 'b-loading'}).exists()).toBe(true);
  });

  it('should display description content when available', async () => {
    wrapper.setData({
      loading: false,
      description: {data: 'This is a sample description text.'}
    });
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.ql-editor').exists()).toBe(true);
    expect(wrapper.find('.ql-editor').html()).toContain('This is a sample description text.');
  });

  it('should display "no-description" message when description is not available', () => {
    wrapper.setData({loading: false, description: null});

    expect(wrapper.find('em').text()).toBe('no-description');
  });

  it('should display "add" button when description is not available and canEdit is true', () => {
    wrapper.setData({loading: false, description: null});

    expect(wrapper.find('.button.is-small.margin').exists()).toBe(true);
  });

  it('should compute previewDescription correctly', () => {
    wrapper.setData({
      description: {data: 'Short description text'}
    });
    expect(wrapper.vm.previewDescription).toBe('Short description text');

    wrapper.setData({
      description: {data: 'A very long description text that exceeds the preview length limit...'}
    });
    expect(wrapper.vm.previewDescription).toBe('A very long description text that exceeds the preview length limit...');
  });
});

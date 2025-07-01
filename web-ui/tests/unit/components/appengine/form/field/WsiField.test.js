import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import WsiField from '@/components/appengine/forms/fields/WsiField';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true),
}));

describe('WsiField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    description: 'This is a test description',
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(WsiField, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      propsData: {
        parameter: mockParameter,
        value: null,
      },
      stubs: {
        ImageSelection: true,
      }
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);
    expect(wrapper.find('button').text()).toBe('select');
    expect(wrapper.find('.image-container').exists()).toBe(false);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    expect(tooltips.at(0).props('label')).toBe(mockParameter.description);
  });

  it('The input should not have a default value', () => {
    expect(wrapper.vm.input).toBeNull();
  });

  it('The id should be rendered when selected', async  () => {
    await wrapper.setProps({value: 42});

    expect(wrapper.vm.value).toBe(42);
    expect(wrapper.find('.image-container').exists()).toBe(true);
    expect(wrapper.find('.image-container').text()).toBe('image 42');
  });

  it('Changing the value should emit an event', async () => {
    await wrapper.setData({input: {id: 42}});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input.at(0)).toEqual([42]);
  });
});

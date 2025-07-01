import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import ArrayField from '@/components/appengine/forms/fields/ArrayField';
import ArrayModal from '@/components/appengine/forms/fields/array/ArrayModal';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('ArrayField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    description: 'This is a test description',
    type: {subType: {id: 'boolean'}},
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(ArrayField, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      propsData: {
        parameter: mockParameter,
        value: null,
      },
      data() {
        return {
          selectParameters: false,
        };
      },
      stubs: {
        ArrayModal: true,
      }
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('button').text()).toBe('select');
    expect(wrapper.find('.state-container').exists()).toBe(false);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    expect(tooltips.at(0).props('label')).toBe(mockParameter.description + ', items');

    expect(wrapper.findComponent(ArrayModal).exists()).toBe(true);
    expect(wrapper.vm.selectParameters).toBe(false);
  });

  it('The input should not have a default value', () => {
    expect(wrapper.vm.input).toBe(null);
  });

  it('The component should render provisioned text when there is a value', async () => {
    await wrapper.setProps({value: [42]});

    expect(wrapper.find('.state-container').exists()).toBe(true);
    expect(wrapper.find('.state-container').text()).toBe('provisioned');
  });
});

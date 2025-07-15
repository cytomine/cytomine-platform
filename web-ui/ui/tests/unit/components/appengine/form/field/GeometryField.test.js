import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import GeometryField from '@/components/appengine/forms/fields/GeometryField';

describe('GeometryField.vue', () => {
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

    wrapper = mount(GeometryField, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      propsData: {
        parameter: mockParameter,
        value: null,
      },
      stubs: {
        AnnotationSelection: true,
      }
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);
    expect(wrapper.find('button').text()).toBe('select');
    expect(wrapper.find('.annotation-container').exists()).toBe(false);

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
    expect(wrapper.find('.annotation-container').exists()).toBe(true);
    expect(wrapper.find('.annotation-container').text()).toBe('annotation 42');
  });

  it('should emit an event when the value is changed', async () => {
    await wrapper.setData({input: 42});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input.at(0)).toEqual([42]);
  });
});

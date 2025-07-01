import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import StringField from '@/components/appengine/forms/fields/StringField';

describe('StringField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    default: 'default',
    description: 'This is a test description',
    type: {minLength: 2, maxLength: 10},
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(StringField, {
      localVue,
      propsData: {
        parameter: mockParameter,
        value: mockParameter.default,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);

    expect(wrapper.findAll('input[type="text"]').length).toBe(1);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    const expectedLabel = mockParameter.description + ', 2 ≤ Test Parameter.length ≤ 10';
    expect(tooltips.at(0).props('label')).toBe(expectedLabel);
  });

  it('The input should have a default value', () => {
    expect(wrapper.vm.input).toBe(mockParameter.default);
  });

  it('The input should show the correct constraints summary', () => {
    const input = wrapper.find('input[type="text"]');

    expect(input.attributes('placeholder')).toBe('2 ≤ Test Parameter.length ≤ 10');
  });

  it('The input not show the tooltip if description is empty', async () => {
    await wrapper.setProps({
      parameter: {
        default: 'no string',
        description: null,
        type: {minLength: 2, maxLength: 10},
      }
    });

    expect(wrapper.vm.tooltips).toBeUndefined();
    expect(wrapper.findAllComponents({name: 'BTooltip'}).length).toBe(0);
  });

  it('Changing the value should emit an event', async () => {
    await wrapper.setData({input: 'Changing'});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input.at(0)).toEqual(['Changing']);
  });
});

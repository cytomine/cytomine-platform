import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import NumberField from '@/components/appengine/forms/fields/NumberField';

describe('NumberField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    default: 4.2,
    description: 'This is a test description',
    type: {gt: 5, lt: 10, geq: null, leq: null, infinityAllowed: true, nanAllowed: false},
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(NumberField, {
      localVue,
      propsData: {
        parameter: mockParameter,
        value: mockParameter.default,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);

    expect(wrapper.findAll('input[type="number"]').length).toBe(1);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    const expectedLabel = mockParameter.description + ', 5 < Test Parameter < 10, ∞';
    expect(tooltips.at(0).props('label')).toBe(expectedLabel);
  });

  it('The input should have a default value', () => {
    expect(wrapper.vm.input).toBe(mockParameter.default);
  });

  it('The input should show the correct constraints summary',  () => {
    const input = wrapper.find('input[type="number"]');

    expect(input.attributes('placeholder')).toBe('5 < Test Parameter < 10, ∞');
  });

  it('The input not show the tooltip if description is empty', async () => {
    await wrapper.setProps({
      parameter: {
        default: 4.2,
        description: null,
        type: {gt: 10.1, lt: 20.2, geq: null, leq: null, infinityAllowed: true, nanAllowed: false},
      }
    });

    expect(wrapper.vm.tooltips).toBeUndefined();
    expect(wrapper.findAllComponents({name: 'BTooltip'}).length).toBe(0);
  });

  it('Changing the value should emit an event', async () => {
    await wrapper.setData({input: 12});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input.at(0)).toEqual([12]);
  });
});

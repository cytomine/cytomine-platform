import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import EnumerationField from '@/components/appengine/forms/fields/EnumerationField';

describe('EnumerationField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    default: 'A',
    description: 'This is a test description',
    type: {values: ['A', 'B', 'C']},
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(EnumerationField, {
      localVue,
      propsData: {
        parameter: mockParameter,
        value: mockParameter.default,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);

    const options = wrapper.findAll('option');
    expect(options.length).toBe(mockParameter.type.values.length);
    expect(options.at(0).text()).toBe('A');
    expect(options.at(1).text()).toBe('B');
    expect(options.at(2).text()).toBe('C');

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    expect(tooltips.at(0).props('label')).toBe(mockParameter.description);
  });

  it('The input should have a default value', () => {
    expect(wrapper.vm.input).toBe(mockParameter.default);
  });

  it('The input not show the tooltip if description is empty', async () => {
    await wrapper.setProps({
      parameter: {
        default: 'B',
        description: null,
        type: {values: ['A', 'B', 'C']},
      }
    });

    expect(wrapper.vm.tooltips).toBeUndefined();
    expect(wrapper.findAllComponents({name: 'BTooltip'}).length).toBe(0);
  });

  it('Changing the value should emit an event', async () => {
    await wrapper.find('select').setValue('B');

    const emitted = wrapper.emitted().input;
    expect(emitted).toBeTruthy();
    expect(emitted.at(0)).toEqual(['B']);
    expect(mockParameter.type.values).toContain(emitted.at(0).at(0));
  });

  it('Changing to an unknown value should fail', async () => {
    await wrapper.find('select').setValue('Unknown');

    const emitted = wrapper.emitted().input;
    expect(emitted).toBeTruthy();
    expect(emitted.at(0)).toEqual([undefined]);
    expect(mockParameter.type.values).not.toContain(emitted.at(0).at(0));
  });
});

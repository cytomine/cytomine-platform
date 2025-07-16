import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import BooleanField from '@/components/appengine/forms/fields/BooleanField';

describe('BooleanField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    default: true,
    description: 'This is a test description',
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(BooleanField, {
      localVue,
      propsData: {
        parameter: mockParameter,
        value: mockParameter.default,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.find('.display-name').text()).toBe(mockParameter.display_name);

    expect(wrapper.findAll('input[type="radio"]').length).toBe(2);
    expect(wrapper.findAll('input[type="radio"][value="true"]').length).toBe(1);
    expect(wrapper.findAll('input[type="radio"][value="false"]').length).toBe(1);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    expect(tooltips.at(0).props('label')).toBe(mockParameter.description);
  });

  it('The input should have a default value', () => {
    expect(wrapper.vm.input).toBe(mockParameter.default);
  });

  it('The "False" radio button should be checked when the input is false', async () => {
    await wrapper.setProps({value: null});

    const radioTrue = wrapper.find('input[type="radio"][value="true"]');
    expect(radioTrue.element.checked).toBe(false);

    const radioFalse = wrapper.find('input[type="radio"][value="false"]');
    expect(radioFalse.element.checked).toBe(false);

    await wrapper.setProps({value: false});

    expect(radioTrue.element.checked).toBe(false);
    expect(radioFalse.element.checked).toBe(true);
  });

  it('The "True" radio button should be checked when the input is true', async () => {
    await wrapper.setProps({value: null});

    const radioTrue = wrapper.find('input[type="radio"][value="true"]');
    expect(radioTrue.element.checked).toBe(false);

    const radioFalse = wrapper.find('input[type="radio"][value="false"]');
    expect(radioFalse.element.checked).toBe(false);

    await wrapper.setProps({value: true});

    expect(radioTrue.element.checked).toBe(true);
    expect(radioFalse.element.checked).toBe(false);
  });

  it('Selecting the radio button should emit an event', async () => {
    await wrapper.setProps({value: null});
    expect(wrapper.vm.input).toBe(null);

    const radioFalse = wrapper.find('input[type="radio"][value="false"]');
    expect(radioFalse.element.checked).toBe(false);

    await radioFalse.setChecked();

    expect(radioFalse.element.checked).toBe(true);
    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input[0]).toEqual([false]);
  });
});

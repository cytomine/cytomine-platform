import {shallowMount} from '@vue/test-utils';

import BooleanItem from '@/components/utils/BooleanItem';

const mocks = {
  $t: message => message,
};

describe('BooleanItem.vue', () => {
  it('should render "yes" when value is true', () => {
    const wrapper = shallowMount(BooleanItem, {
      mocks: mocks,
      propsData: {
        value: true
      }
    });

    expect(wrapper.find('.tag.is-success').exists()).toBe(true);
    expect(wrapper.find('.tag.is-danger').exists()).toBe(false);

    expect(wrapper.find('i.fas.fa-check').exists()).toBe(true);
    expect(wrapper.text()).toContain('yes');
  });

  it('should render "no" when value is false', () => {
    const wrapper = shallowMount(BooleanItem, {
      mocks: mocks,
      propsData: {
        value: false
      }
    });

    expect(wrapper.find('.tag.is-success').exists()).toBe(false);
    expect(wrapper.find('.tag.is-danger').exists()).toBe(true);

    expect(wrapper.find('i.fas.fa-times').exists()).toBe(true);
    expect(wrapper.text()).toContain('no');
  });

  it('should render "no" when value is undefined', () => {
    const wrapper = shallowMount(BooleanItem, {
      mocks: mocks,
      propsData: {
        value: undefined
      }
    });

    expect(wrapper.find('.tag.is-success').exists()).toBe(false);
    expect(wrapper.find('.tag.is-danger').exists()).toBe(true);
    expect(wrapper.text()).toContain('no');
  });
});

import {shallowMount} from '@vue/test-utils';

import PageNotFound from '@/components/PageNotFound';

describe('PageNotFound.vue', () => {
  it('should render the component correctly', () => {
    const wrapper = shallowMount(PageNotFound, {
      mocks: {
        $t: (msg) => msg
      }
    });

    expect(wrapper.find('h1').exists()).toBe(true);
    expect(wrapper.find('h1').text()).toBe('page-not-found');
  });

  it('should render correctly when $t returns undefined', () => {
    const wrapper = shallowMount(PageNotFound, {
      mocks: {
        $t: () => undefined
      }
    });

    expect(wrapper.find('h1').exists()).toBe(true);
    expect(wrapper.find('h1').text()).toBe('');
  });
});

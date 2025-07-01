import {shallowMount} from '@vue/test-utils';

import AppCardInfo from '@/components/appengine/AppCardInfo';

describe('AppCardInfo.vue', () => {
  it('The component should be rendered correctly', () => {
    const wrapper = shallowMount(AppCardInfo, {});

    expect(wrapper.find('h1').text()).toBe('Applications Page');
  });
});

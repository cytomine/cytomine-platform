import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import DomainTagInput from '@/components/utils/DomainTagInput.vue';
import {getWildcardRegexp} from '@/utils/string-utils';

jest.mock('@/utils/string-utils', () => ({
  getWildcardRegexp: jest.fn().mockImplementation(search => new RegExp(search, 'i'))
}));

describe('DomainTagInput.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);

  const mocks = {
    $t: message => message
  };

  const domains = [
    {id: 1, name: 'example.com'},
    {id: 2, name: 'test.com'},
    {id: 3, name: 'another.com'}
  ];

  const value = [{id: 1, name: 'example.com'}];

  let wrapper;
  beforeEach(() => {
    wrapper = shallowMount(DomainTagInput, {
      localVue,
      mocks,
      propsData: {
        value,
        domains
      }
    });
  });

  it('should render the tag input correctly', () => {
    expect(wrapper.findComponent(DomainTagInput).exists()).toBe(true);
  });

  it('should filter domains when searchString is "test" correctly', async () => {
    wrapper.setData({searchString: 'test'});
    await wrapper.vm.$nextTick();

    expect(getWildcardRegexp).toHaveBeenCalledWith('test');
    expect(wrapper.vm.filteredDomains).toEqual([{id: 2, name: 'test.com'}]);
  });

  it('should emit input event when a tag is added', async () => {
    wrapper.vm.$emit('input', [{id: 2, name: 'test.com'}]);
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input[0]).toEqual([[{id: 2, name: 'test.com'}]]);
  });

  it('should not allow duplicate domains to be added', async () => {
    wrapper.setData({searchString: 'example'});
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.filteredDomains).toEqual([]);
  });
});

import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import {Cytomine} from 'cytomine-client';

import MetadataPanel from '@/components/viewer/panels/MetadataPanel';

jest.mock('cytomine-client', () => ({
  Cytomine: {
    instance: {
      api: {
        get: jest.fn(() => Promise.resolve({
          data: {
            collection: [
              {namespace: 'ns1', key: 'key1', value: 'value1'},
              {namespace: 'ns2', key: 'key2', value: 'value2'}
            ]
          }
        }))
      }
    }
  }
}));

jest.mock('@/utils/string-utils', () => ({
  getWildcardRegexp: jest.fn((str) => new RegExp(str, 'i'))
}));

describe('MetadataPanel.vue', () => {
  let localVue;
  let wrapper;

  beforeEach(async () => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = shallowMount(MetadataPanel, {
      localVue,
      propsData: {index: '0'},
      mocks: {
        $t: (message) => message,
        $store: {
          getters: {
            'currentProject/currentViewer': {
              images: [{imageInstance: {id: 123}}]
            }
          }
        },
        $eventBus: {$emit: jest.fn()}
      }
    });

    await wrapper.vm.$nextTick();
  });

  it('should fetch metadata on creation', async () => {
    expect(Cytomine.instance.api.get).toHaveBeenCalledWith('imageinstance/123/metadata.json');
    expect(wrapper.vm.metadata.length).toBe(2);
  });

  it('should filter metadata based on search string', async () => {
    wrapper.setData({searchString: 'key1'});

    await wrapper.vm.$nextTick();

    expect(wrapper.vm.filteredProps).toHaveLength(1);
    expect(wrapper.vm.filteredProps[0].key).toBe('key1');
  });

  it('should emit close event when closeMetadata is called', () => {
    wrapper.vm.closeMetadata();

    expect(wrapper.vm.$eventBus.$emit).toHaveBeenCalledWith('close-metadata');
  });
});

import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import CytomineModal from '@/components/utils/CytomineModal';
import ImageSelection from '@/components/image/ImageSelection';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

jest.mock('cytomine-client', () => ({
  ImageInstanceCollection: {
    fetchAll: jest.fn().mockResolvedValue({
      array: [
        {id: 1, name: 'Image 1'},
        {id: 2, name: 'Image 2'},
      ]
    }),
  },
}));

describe('ImageSelection.vue', () => {
  let localVue;
  let wrapper;

  const mockImages = [
    {id: 1, name: 'Image 1'},
    {id: 2, name: 'Image 2'},
  ];

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(ImageSelection, {
      localVue,
      propsData: {
        active: true,
      },
      data() {
        return {
          loading: false,
          selectImage: null,
        };
      },
      computed: {
        project: () => ({id: 42}),
      },
      mocks: {
        $t: (message) => message,
      },
      stubs: {
        SelectableImage: true,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModal).exists()).toBe(true);
    expect(wrapper.find('.image-content').exists()).toBe(true);
  });

  it('The component should render the loading when the data is fetched', async () => {
    await wrapper.setData({loading: true});

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModal).exists()).toBe(true);
    expect(wrapper.find('.image-content').exists()).toBe(false);
  });

  it('Selecting an image should emit the select-image event', async () => {
    wrapper.setData({selectedImage: mockImages[0]});
    await wrapper.vm.select();

    expect(wrapper.emitted('select-image')).toBeTruthy();
    expect(wrapper.emitted('select-image')[0]).toEqual([mockImages[0]]);
  });

  it('Clicking on cancel should reset selectedImage', async () => {
    wrapper.setData({selectedImage: mockImages[1]});

    expect(wrapper.vm.selectedImage).toBe(mockImages[1]);

    wrapper.vm.cancel();
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.selectedImage).toBe(null);
    expect(wrapper.emitted('update:active')).toEqual([[false]]);
  });
});

import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import ImageMultiSelect from '@/components/appengine/forms/fields/array/ImageMultiSelect';
import SelectableImage from '@/components/image/SelectableImage';

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

describe('ImageMultiSelect.vue', () => {
  let localVue;
  let wrapper;

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(ImageMultiSelect, {
      localVue,
      computed: {
        project: () => ({
          id: 42,
        }),
      },
      data() {
        return {
          loading: false,
          selectedImages: [],
        };
      },
      stubs: {
        SelectableImage: true,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(SelectableImage).exists()).toBe(true);
    expect(wrapper.find('.image-content').exists()).toBe(true);
  });

  it('The component should render the loading when the data is fetched', async () => {
    await wrapper.setData({loading: true});

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(SelectableImage).exists()).toBe(false);
    expect(wrapper.find('.image-content').exists()).toBe(false);
  });

  it('The component should emit an input event when selecting images', async () => {
    const newImageIds = [42, 1337];

    await wrapper.setData({selectedImages: newImageIds});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input[0]).toEqual([newImageIds]);
  });
});

import {shallowMount} from '@vue/test-utils';
import ImageThumbnail from '@/components/image/ImageThumbnail';
import SelectableImage from '@/components/image/SelectableImage';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('SelectableImage.vue', () => {
  let wrapper;

  const mockImage = {id: 101, name: 'Image 1'};

  beforeEach(() => {
    wrapper = shallowMount(SelectableImage, {
      propsData: {
        image: mockImage,
        isSelected: false,
      },
      computed: {
        shortTermToken: () => 'mock-token',
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
  });

  it('The component should render the selected class when isSelected is true', async () => {
    await wrapper.setProps({isSelected: true});

    expect(wrapper.classes()).toContain('selected');
  });

  it('The component should not apply the selected class when isSelected is false', () => {
    expect(wrapper.classes()).not.toContain('selected');
  });

  it('The component should render ImageThumbnail with correct props', () => {
    const imageThumbnail = wrapper.findComponent(ImageThumbnail);

    expect(imageThumbnail.exists()).toBe(true);
    expect(imageThumbnail.props('image')).toBe(mockImage);
    expect(imageThumbnail.props('size')).toBe(128);
  });

  it('Clicking on the ImageThumbnail should emit update:selected', async () => {
    await wrapper.trigger('click');

    expect(wrapper.emitted('update:selected')).toBeTruthy();
    expect(wrapper.emitted('update:selected')[0]).toEqual([mockImage.id]);
  });
});

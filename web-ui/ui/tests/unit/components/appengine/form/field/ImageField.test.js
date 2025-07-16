import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import ImageField from '@/components/appengine/forms/fields/ImageField';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('ImageField.vue', () => {
  let localVue;
  let wrapper;

  const mockParameter = {
    // eslint-disable-next-line
    display_name: 'Test Parameter',
    description: 'This is a test description',
  };

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(ImageField, {
      localVue,
      mocks: {
        $t: (message) => message,
      },
      propsData: {
        parameter: mockParameter,
        value: null,
      },
      stubs: {
        AnnotationSelection: true,
        ImageSelection: true,
      }
    });
  });

  it('should render the component correctly', () => {
    expect(wrapper.find('.field label').text()).toBe(mockParameter.display_name);
    expect(wrapper.findAllComponents({name: 'BButton'}).length).toBe(2);
    expect(wrapper.find('.value-container').exists()).toBe(false);

    const tooltips = wrapper.findAllComponents({name: 'BTooltip'});
    expect(tooltips.length).toBe(1);
    expect(tooltips.at(0).exists()).toBe(true);
    expect(tooltips.at(0).props('label')).toBe(mockParameter.description);
  });

  it('should not have a default value for input', () => {
    expect(wrapper.vm.input).toBeNull();
  });

  it('should render the annotation with id when selected', async () => {
    const mockedData = {type: 'annotation', id: 42};
    await wrapper.setProps({value: mockedData});
    await wrapper.setData({type: mockedData.type});

    expect(wrapper.vm.value).toBe(mockedData);
    expect(wrapper.find('.value-container').exists()).toBe(true);
    expect(wrapper.find('.value-container').text()).toBe('annotation 42');
  });

  it('should render the image with id when selected', async () => {
    const mockedData = {type: 'image', id: 42};
    await wrapper.setProps({value: mockedData});
    await wrapper.setData({type: mockedData.type});

    expect(wrapper.vm.value).toBe(mockedData);
    expect(wrapper.find('.value-container').exists()).toBe(true);
    expect(wrapper.find('.value-container').text()).toBe('image 42');
  });

  it('should emit an event when the value is changed', async () => {
    const mockedData = {type: 'image', id: 42};
    await wrapper.setData({input: mockedData});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input.at(0)).toEqual([mockedData]);
  });
});

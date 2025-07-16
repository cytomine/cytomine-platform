import {shallowMount} from '@vue/test-utils';
import SelectableAnnotation from '@/components/annotations/SelectableAnnotation';
import AnnotationPreview from '@/components/annotations/AnnotationPreview';

describe('SelectableAnnotation.vue', () => {
  let wrapper;

  const mockAnnotation = {id: 1};
  const mockImages = [{id: 101, name: 'Image 1'}];
  const mockUsers = [{id: 201, username: 'User1'}];

  beforeEach(() => {
    wrapper = shallowMount(SelectableAnnotation, {
      propsData: {
        annotation: mockAnnotation,
        images: mockImages,
        users: mockUsers,
        isSelected: false,
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

  it('The component should render AnnotationPreview with correct props', () => {
    const annotationPreview = wrapper.findComponent(AnnotationPreview);

    expect(annotationPreview.exists()).toBe(true);
    expect(annotationPreview.props('annot')).toBe(mockAnnotation);
    expect(annotationPreview.props('images')).toBe(mockImages);
    expect(annotationPreview.props('users')).toBe(mockUsers);
    expect(annotationPreview.props('size')).toBe(85);
  });

  it('Clicking on the AnnotationPreview should emit update:selected', async () => {
    await wrapper.trigger('click');

    expect(wrapper.emitted('update:selected')).toBeTruthy();
    expect(wrapper.emitted('update:selected')[0]).toEqual([mockAnnotation.id]);
  });
});

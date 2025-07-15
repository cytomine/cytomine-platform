import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import AnnotationMultiSelect from '@/components/appengine/forms/fields/array/AnnotationMultiSelect';
import SelectableAnnotation from '@/components/annotations/SelectableAnnotation';

jest.mock('cytomine-client', () => ({
  AnnotationCollection: jest.fn().mockImplementation(() => ({
    fetchAll: jest.fn().mockResolvedValue({
      array: [
        {id: 1, name: 'Annotation 1'},
        {id: 2, name: 'Annotation 2'},
      ]
    }),
  })),
}));

describe('AnnotationMultiSelect.vue', () => {
  let localVue;
  let wrapper;

  const mockImages = [{imageInstance: {id: 1}}];

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(AnnotationMultiSelect, {
      localVue,
      computed: {
        project: () => ({
          id: 42,
        }),
      },
      mocks: {
        $store: {
          getters: {
            'currentProject/currentViewer': {images: mockImages},
          },
        },
      },
      data() {
        return {
          loading: false,
          selectedAnnotations: [],
        };
      },
      stubs: {
        AnnotationPreview: true,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(SelectableAnnotation).exists()).toBe(true);
    expect(wrapper.find('.annotation-content').exists()).toBe(true);
  });

  it('The component should render the loading when the data is fetched', async () => {
    await wrapper.setData({loading: true});

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(SelectableAnnotation).exists()).toBe(false);
    expect(wrapper.find('.annotation-content').exists()).toBe(false);
  });

  it('The component should emit an input event when selecting annotations', async () => {
    const newAnnotations = [
      {id: 42, name: 'Annotation A'},
      {id: 1337, name: 'Annotation B'}
    ];

    await wrapper.setData({selectedAnnotations: newAnnotations});

    expect(wrapper.emitted().input).toBeTruthy();
    expect(wrapper.emitted().input[0]).toEqual([[42, 1337]]);
  });
});

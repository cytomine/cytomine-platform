import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import AnnotationSelection from '@/components/annotations/AnnotationSelection';
import CytomineModal from '@/components/utils/CytomineModal';

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

describe('AnnotationSelection.vue', () => {
  let localVue;
  let wrapper;

  const mockAnnotations = [
    {id: 1, name: 'Annotation 1'},
    {id: 2, name: 'Annotation 2'},
  ];
  const mockImages = [{imageInstance: {id: 1}}];

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(AnnotationSelection, {
      localVue,
      propsData: {
        active: true,
      },
      computed: {
        project: () => ({
          id: 42,
        }),
        layers: () => [
          {id: 101, name: 'Mock Layer 1'},
          {id: 102, name: 'Mock Layer 2'}
        ]
      },
      mocks: {
        $t: (message) => message,
        $store: {
          getters: {
            'currentProject/currentViewer': {images: mockImages},
            'currentProject/terms': [{id: 1, name: 'Term 1'}],
          },
        },
      },
      data() {
        return {
          loading: false,
          selectedAnnotation: null,
        };
      },
      stubs: {
        AnnotationPreview: true,
      },
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModal).exists()).toBe(true);
    expect(wrapper.find('.annotation-content').exists()).toBe(true);
  });

  it('The component should render the loading when the data is fetched', async () => {
    await wrapper.setData({loading: true});

    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModal).exists()).toBe(true);
    expect(wrapper.find('.annotation-content').exists()).toBe(false);
  });

  it('Selecting an annotation should emit the select-annotation event', async () => {
    wrapper.setData({selectedAnnotation: mockAnnotations[0]});
    await wrapper.vm.selectAnnotation();

    expect(wrapper.emitted('select-annotation')).toBeTruthy();
    expect(wrapper.emitted('select-annotation')[0]).toEqual([mockAnnotations[0]]);
  });

  it('Clicking on cancel annotation should reset selectedAnnotation', async () => {
    wrapper.setData({selectedAnnotation: mockAnnotations[0]});

    expect(wrapper.vm.selectedAnnotation).toBe(mockAnnotations[0]);

    wrapper.vm.cancelAnnotation();
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.selectedAnnotation).toBe(null);
    expect(wrapper.emitted('update:active')).toEqual([[false]]);
  });
});

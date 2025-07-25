import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';
import VueDraggableResizable from 'vue-draggable-resizable';
import Vuex from 'vuex';

import AnnotationPreview from '@/components/annotations/AnnotationPreview';
import SimilarAnnotation from '@/components/annotations/SimilarAnnotation';
import {Annotation} from 'cytomine-client';

jest.mock('cytomine-client', () => ({
  Annotation: {
    fetch: jest.fn(),
  },
}));

describe('SimilarAnnotation.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);
  localVue.use(Vuex);

  const annotationsMock = [
    {id: 1, term: [1]},
    {id: 2, term: [2]},
  ];

  const dataMock = {
    similarities: [
      ['1', 80.0],
      ['2', 70.0],
    ],
  };

  const testIndex = '0';

  let getters;
  let mutations;
  let store;
  let state;
  let wrapper;

  beforeEach(() => {
    Annotation.fetch
      .mockResolvedValueOnce({id: 1, term: [1]})
      .mockResolvedValueOnce({id: 2, term: [2]});

    state = {
      currentProject: {
        imageModule: jest.fn().mockReturnValue('testModule/'),
        currentViewer: {
          images: {
            '0': {
              selectedFeatures: {
                showSimilarAnnotations: true,
                displayAnnotDetails: true,
                similarAnnotations: dataMock,
                queryAnnotation: {id: 1, term: [1]}
              },
            },
          },
        },
        terms: [{id: 1, name: 'term1'}, {id: 2, name: 'term2'}],
      },
    };

    getters = {
      'testModule/selectedFeature': () => ({
        properties: {
          annot: {
            id: 1,
            term: [1],
          },
        },
      }),
      'currentProject/imageModule': () => state.currentProject.imageModule,
      'currentProject/terms': () => state.currentProject.terms,
      'currentProject/currentViewer': () => state.currentProject.currentViewer,
    };

    mutations = {
      'testModule/setShowSimilarAnnotations': (state, value) => {
        const image = state.currentProject.currentViewer.images[testIndex];
        image.selectedFeatures.showSimilarAnnotations = value;
      },
    };

    store = new Vuex.Store({
      state,
      getters,
      mutations,
    });

    wrapper = shallowMount(SimilarAnnotation, {
      localVue,
      store,
      propsData: {
        index: testIndex,
      },
      mocks: {
        $t: (msg) => msg,
        $store: store,
        $notify: jest.fn(),
        $eventBus: {
          $on: jest.fn(),
          $off: jest.fn(),
        },
      },
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should render correctly', () => {
    expect(wrapper.findComponent(VueDraggableResizable).exists()).toBe(true);
    expect(wrapper.find('.similar-annotations-playground').exists()).toBe(true);
    expect(wrapper.find('h1').text()).toBe('similar-annotations');
    expect(wrapper.vm.showSimilarAnnotations).toBe(true);
  });

  it('should render the annotations and distances correctly', async () => {
    await wrapper.vm.$nextTick();

    const annotationPreviews = wrapper.findAllComponents(AnnotationPreview);
    expect(annotationPreviews.length).toBe(2);
    expect(annotationPreviews.at(0).props('annot')).toEqual(annotationsMock[0]);
    expect(annotationPreviews.at(1).props('annot')).toEqual(annotationsMock[1]);

    const distances = wrapper.findAll('.annotation-data div');
    expect(distances.at(0).text()).toBe('80.00%');
    expect(distances.at(1).text()).toBe('70.00%');
  });

  it('should fetch annotations on created hook and updates state', async () => {
    await wrapper.vm.$nextTick();

    expect(Annotation.fetch).toHaveBeenCalledTimes(2);
    expect(wrapper.vm.annotations).toEqual(annotationsMock);
    expect(wrapper.vm.loading).toBe(false);
  });

  it('should suggest terms correctly based on the annotations', async () => {
    await wrapper.vm.$nextTick();
    wrapper.vm.countTerm();

    expect(wrapper.vm.suggestedTerms).toEqual([
      [{id: 2, name: 'term2'}, 1],
    ]);
  });

  it('should close the window when close button is clicked', async () => {
    const closeButton = wrapper.find('.button.is-small.close');
    await closeButton.trigger('click');

    expect(wrapper.vm.showSimilarAnnotations).toBe(false);
  });
});
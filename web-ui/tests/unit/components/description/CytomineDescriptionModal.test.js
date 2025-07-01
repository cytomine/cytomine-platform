import {createLocalVue, shallowMount} from '@vue/test-utils';
import VueI18n from 'vue-i18n';

import CytomineDescriptionModal from '@/components/description/CytomineDescriptionModal';
import CytomineModalCard from '@/components/utils/CytomineModalCard';

jest.mock('@/components/utils/CytomineModalCard', () => ({
  name: 'cytomine-modal-card',
  template: '<div><slot></slot></div>'
}));

jest.mock('@/components/form/CytomineQuillEditor', () => ({
  name: 'cytomine-quill-editor',
  template: '<div><slot></slot></div>',
  props: ['value', 'placeholder']
}));

describe('CytomineDescriptionModal', () => {
  const localVue = createLocalVue();
  localVue.use(VueI18n);

  let wrapper;

  beforeEach(() => {
    wrapper = shallowMount(CytomineDescriptionModal, {
      localVue,
      mocks: {
        $t: (message) => message,
        $notify: jest.fn()
      },
      propsData: {
        description: {data: 'Test description'},
        edit: false
      }
    });
  });

  it('should render the component correctly', () => {
    expect(wrapper.findComponent(CytomineModalCard).exists()).toBe(true);
    expect(wrapper.find('.ql-editor.preview').exists()).toBe(true);
    expect(wrapper.text()).toContain('description');
  });
});

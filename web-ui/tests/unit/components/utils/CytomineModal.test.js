import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';

import CytomineModal from '@/components/utils/CytomineModal';
import CytomineModalCard from '@/components/utils/CytomineModalCard';

describe('CytomineModal.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);

  const mocks = {
    $t: message => message,
  };

  it('should render correctly when active is true', () => {
    const title = 'Test Title';
    const wrapper = shallowMount(CytomineModal, {
      localVue,
      mocks: mocks,
      propsData: {
        active: true,
        title
      },
      stubs: {
        'cytomine-modal-card': CytomineModalCard
      }
    });

    expect(wrapper.props().active).toBe(true);
    expect(wrapper.props().title).toBe(title);

    expect(wrapper.findComponent(CytomineModalCard).exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModalCard).props('title')).toBe(title);
  });

  it('should render slot content correctly', () => {
    const wrapper = shallowMount(CytomineModal, {
      localVue,
      propsData: {
        active: true
      },
      slots: {
        default: '<div class="modal-content">Content</div>'
      }
    });

    expect(wrapper.find('.modal-content').text()).toBe('Content');
  });

  it('should emit "close" and "update:active" events when close method is called', async () => {
    const wrapper = shallowMount(CytomineModal, {
      localVue,
      propsData: {
        active: true
      }
    });

    wrapper.vm.close();

    expect(wrapper.emitted().close).toBeTruthy();
    expect(wrapper.emitted()['update:active'][0]).toEqual([false]);
  });
});

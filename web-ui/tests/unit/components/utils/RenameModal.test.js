import {createLocalVue, shallowMount} from '@vue/test-utils';
import VeeValidate from 'vee-validate';
import Buefy from 'buefy';

import CytomineModal from '@/components/utils/CytomineModal';
import RenameModal from '@/components/utils/RenameModal';

describe('RenameModal.vue', () => {
  const localVue = createLocalVue();
  localVue.use(VeeValidate);
  localVue.use(Buefy);

  const mocks = {
    $t: message => message,
  };

  it('should render the modal with the correct title', () => {
    const wrapper = shallowMount(RenameModal, {
      localVue,
      mocks,
      propsData: {
        active: true,
        title: 'Rename',
        currentName: 'Old Name'
      }
    });

    expect(wrapper.props().active).toBe(true);
    expect(wrapper.props().title).toBe('Rename');
    expect(wrapper.props().currentName).toBe('Old Name');

    expect(wrapper.findComponent(CytomineModal).props('title')).toBe('Rename');
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

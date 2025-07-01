import {createLocalVue, mount} from '@vue/test-utils';
import Buefy from 'buefy';

import AnnotationMultiSelect from '@/components/appengine/forms/fields/array/AnnotationMultiSelect';
import ArrayModal from '@/components/appengine/forms/fields/array/ArrayModal';
import BooleanField from '@/components/appengine/forms/fields/BooleanField';
import CytomineModal from '@/components/utils/CytomineModal';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('ArrayModal.vue', () => {
  let localVue;
  let wrapper;

  beforeEach(() => {
    localVue = createLocalVue();
    localVue.use(Buefy);

    wrapper = mount(ArrayModal, {
      localVue,
      mocks: {
        $t: msg => msg,
        $notify: jest.fn()
      },
      stubs: {
        AnnotationMultiSelect: true,
      },
      propsData: {
        type: {id: 'boolean'},
        active: true,
      }
    });
  });

  it('The component should be rendered correctly', () => {
    expect(wrapper.exists()).toBe(true);
    expect(wrapper.findComponent(CytomineModal).exists()).toBe(true);
  });

  test('The component should render a boolean field when clicking on add', async () => {
    expect(wrapper.findComponent(BooleanField).exists()).toBe(false);

    await wrapper.vm.add();

    expect(wrapper.findComponent(BooleanField).exists()).toBe(true);
    expect(wrapper.vm.items.length).toBe(1);
  });

  test('The component should render a complex field when type is complex', async () => {
    await wrapper.setProps({type: {id: 'geometry'}});

    expect(wrapper.findComponent(AnnotationMultiSelect).exists()).toBe(true);
  });

  test('The items should be correctly updated when adding or removing items', async () => {
    wrapper.vm.add();
    wrapper.vm.add();
    expect(wrapper.vm.items.length).toBe(2);

    wrapper.vm.remove(0);
    expect(wrapper.vm.items.length).toBe(1);
  });

  test('Click on cancel should emit update:active false', () => {
    wrapper.vm.cancel();

    expect(wrapper.emitted('update:active')).toBeTruthy();
    expect(wrapper.emitted('update:active')[0]).toEqual([false]);
  });

  test('Click on select should show an error when the list is empty', () => {
    wrapper.vm.select();

    expect(wrapper.vm.$notify).toHaveBeenCalledWith({
      type: 'error',
      text: 'notify-error-empty-list'
    });
  });

  test('Click on select should show an error when not enough items', async () => {
    await wrapper.setProps({minSize: 2});
    await wrapper.setData({items: [true]});

    wrapper.vm.select();

    expect(wrapper.vm.$notify).toHaveBeenCalledWith({
      type: 'error',
      text: 'notify-error-not-enough-item'
    });
  });

  test('Click on select should emits events and resets when valid', async () => {
    await wrapper.setData({items: [true]});

    wrapper.vm.select();

    expect(wrapper.emitted('create-inputs')).toBeTruthy();
    expect(wrapper.emitted('update:active')).toBeTruthy();
    expect(wrapper.vm.items).toEqual([]);
    expect(wrapper.vm.$notify).toHaveBeenCalledWith({
      type: 'success',
      text: 'notify-success-create-array-inputs'
    });
  });
});

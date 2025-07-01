import {shallowMount} from '@vue/test-utils';

import CytomineModalCard from '@/components/utils/CytomineModalCard';

describe('CytomineModalCard.vue', () => {
  const mocks = {
    $t: message => message,
  };

  it('should render the title correctly', () => {
    const title = 'Test Modal Title';
    const wrapper = shallowMount(CytomineModalCard, {
      mocks,
      propsData: {
        title
      }
    });

    expect(wrapper.props().title).toBe(title);
    expect(wrapper.find('.modal-card-title').text()).toBe(title);
  });

  it('should render the footer when "footer" is true', () => {
    const wrapper = shallowMount(CytomineModalCard, {
      mocks,
      propsData: {
        footer: true
      }
    });

    expect(wrapper.props().footer).toBe(true);
    expect(wrapper.find('.modal-card-foot').exists()).toBe(true);
  });

  it('should not render the footer when "footer" is false', () => {
    const wrapper = shallowMount(CytomineModalCard, {
      mocks,
      propsData: {
        footer: false,
      }
    });

    expect(wrapper.props().footer).toBe(false);
    expect(wrapper.find('.modal-card-foot').exists()).toBe(false);
  });

  it('should render default slot content', () => {
    const wrapper = shallowMount(CytomineModalCard, {
      mocks,
      slots: {
        default: '<div class="slot-content">Slot Content</div>'
      }
    });

    expect(wrapper.find('.slot-content').text()).toBe('Slot Content');
  });

  it('should render named slot content for footer', () => {
    const wrapper = shallowMount(CytomineModalCard, {
      propsData: {
        footer: true
      },
      slots: {
        footer: '<div class="footer-slot-content">Footer Slot Content</div>'
      }
    });

    expect(wrapper.find('.footer-slot-content').text()).toBe('Footer Slot Content');
  });

  it('should emit "close" event when the close button is clicked', async () => {
    const wrapper = shallowMount(CytomineModalCard, {
      mocks,
      propsData: {
        footer: true
      }
    });

    await wrapper.find('button').trigger('click');

    expect(wrapper.emitted('close')).toBeTruthy();
  });
});

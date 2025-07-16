import {createLocalVue, shallowMount} from '@vue/test-utils';
import Buefy from 'buefy';
import VueRouter from 'vue-router';

import AppCard from '@/components/appengine/AppCard';

describe('AppCard.vue', () => {
  const localVue = createLocalVue();
  localVue.use(Buefy);
  localVue.use(VueRouter);

  const mockAppData = {
    namespace: 'my-app',
    version: '1.0.0',
    imageUrl: 'https://example.com/image.png',
    name: 'Test App',
    date: '2025-04-11',
    description: 'This is a test app description.'
  };

  let wrapper;

  beforeEach(() => {
    wrapper = shallowMount(AppCard, {
      localVue,
      propsData: {
        appData: mockAppData,
      }
    });
  });

  it('The component should render the app information', () => {
    expect(wrapper.text()).toContain(mockAppData.name);
    expect(wrapper.text()).toContain(mockAppData.date);
    expect(wrapper.text()).toContain(mockAppData.version);
    expect(wrapper.text()).toContain(mockAppData.description);
  });

  it('The component should render the correct image URL', () => {
    expect(wrapper.find('img').attributes('src')).toBe(mockAppData.imageUrl);
  });

  it('The component should use the correct router link', () => {
    const link = wrapper.findComponent({name: 'RouterLink'});
    expect(link.props('to')).toBe(`/appengine/${mockAppData.namespace}/${mockAppData.version}`);
  });

  it('should fallback to placeholder image if imageUrl is not provided', async () => {
    await wrapper.setProps({appData: {...mockAppData, imageUrl: ''}});

    const expected = 'https://bulma.io/assets/images/placeholders/1280x960.png';
    expect(wrapper.find('img').attributes('src')).toBe(expected);
  });
});

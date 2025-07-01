import {createLocalVue, shallowMount} from '@vue/test-utils';

import AppEngineField from '@/components/appengine/forms/fields/AppEngineField.vue';

jest.mock('@/utils/image-utils', () => ({
  isWebPSupported: jest.fn(() => true)
}));

describe('AppEngineField.vue', () => {
  const localVue = createLocalVue();
  let wrapper;

  beforeEach(() => {
    wrapper = shallowMount(AppEngineField, {
      localVue,
      propsData: {
        parameter: {type: {id: 'array'}},
        value: null,
      },
      data() {
        return {
          loading: false,
          selectImage: null,
        };
      },
      computed: {
        project: () => ({id: 42}),
      },
      mocks: {
        $t: (message) => message,
      },
      stubs: {
        ArrayField: {
          name: 'ArrayField',
          props: ['value', 'parameter'],
          template: '<input class="array-field" @input="$emit(\'input\', $event.target.value)" />'
        },
        BooleanField: true,
        EnumerationField: true,
        FileField: true,
        GeometryField: true,
        ImageField: true,
        IntegerField: true,
        NumberField: true,
        StringField: true,
        WsiField: true
      },
    });
  });

  it('The component should render the correct field based on the type', () => {
    expect(wrapper.findComponent({name: 'ArrayField'}).exists()).toBe(true);
  });

  it('The component should emit input when inner component emits input', async () => {
    const input = wrapper.find('input.array-field');

    // Simulate user input
    input.element.value = 'new value';
    await input.trigger('input');

    expect(wrapper.emitted('input')).toBeTruthy();
    expect(wrapper.emitted('input')[0]).toEqual(['new value']);
  });

  it('The component should return correct value via computed input', async () => {
    await wrapper.setProps({value: 'hello world'});

    expect(wrapper.vm.input).toBe('hello world');
  });

  it('The component should set value via computed setter and emits input', () => {
    wrapper.vm.input = 'updated';

    expect(wrapper.emitted('input')).toBeTruthy();
    expect(wrapper.emitted('input')[0]).toEqual(['updated']);
  });
});
